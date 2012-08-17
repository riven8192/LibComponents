package craterstudio.encryption.ssl.nio;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.InetSocketAddress;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import craterstudio.bytes.NativeHacks;
import craterstudio.misc.MainParams;
import craterstudio.streams.AsyncOutputStream;
import craterstudio.text.TextValues;
import craterstudio.time.Clock;
import craterstudio.util.HighLevel;
import craterstudio.util.Pool;
import craterstudio.util.PoolHandler;

public class NioForwardTCP {
	long tickTime;

	public static final int PUMP_STATE = 0;
	public static final int INIT_STATE = 1;

	static PrintStream async(final String name, OutputStream out) {
		out = new BufferedOutputStream(out, 4 * 1024);

		out = new AsyncOutputStream(out, 64 * 1024, 250L) {
			@Override
			public void onFull(long took) {
				log("PERF", "Warning: blocking AsyncOutputStream[" + name + "] backing OutputStream (" + took + "ms)");
			}
		};

		return new PrintStream(out, false);
	}

	static PrintStream defaultLog = System.out;
	static Map<String, PrintStream> nameToLog = new HashMap<>();
	static String LOG_PREFIX = null;
	static long LOG_TIME_NANOS = 0;
	static String LOG_FILTER = null;

	static {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

					while (true) {
						String line = br.readLine();
						if (line == null) {
							break;
						}
						line = line.trim();
						if (line.equals("")) {
							LOG_FILTER = null;
						} else {
							LOG_FILTER = line;
						}
					}
				} catch (IOException exc) {
					exc.printStackTrace();
				}
			}
		}).start();
	}

	public static void log(String target, String msg) {
		if (LOG_PREFIX == null) {
			throw new NullPointerException();
		}

		String now = Clock.timestamp();

		if (LOG_FILTER == null || LOG_FILTER.equals(target)) {
			long t0 = System.nanoTime();
			defaultLog.println("[" + now + "] [" + target + "] " + msg);
			long t1 = System.nanoTime();
			LOG_TIME_NANOS += t1 - t0;
		}

		if (true) {

			if (target != null) {
				PrintStream stream = nameToLog.get(target);

				if (stream == null) {
					File file = new File("./" + LOG_PREFIX + "_nioforward_" + target + ".log");
					try {
						nameToLog.put(target, stream = async(target, new FileOutputStream(file, true)));
					} catch (IOException exc) {
						exc.printStackTrace();
					}
				}

				long t0 = System.nanoTime();
				stream.println("[" + now + "] " + msg);
				long t1 = System.nanoTime();
				LOG_TIME_NANOS += t1 - t0;
			}
		}
	}

	public static MainParams params = new MainParams();

	public static void main(String[] args) throws Exception {
		params.addProps("host", "port", "backlog", "timeout", "workers", "target-host", "target-port");
		params.parse(args);

		LOG_PREFIX = "TCP";

		NioForwardTCP.loop(NioForwardTCP.class, //
		   params.get("host"), //
		   params.getInt("port"), //
		   params.getInt("backlog"), //
		   params.getInt("timeout") * 1000, //
		   params.getInt("workers") //
		   );
	}

	public InetSocketAddress getTarget(String httpHeaderHost) {
		return new InetSocketAddress(params.get("target-host"), params.getInt("target-port"));
	}

	public static void loop(final Class<? extends NioForwardTCP> type, String host, int port, int backlog, final int timeout, int workerCount) throws IOException, Exception {

		if (workerCount == 0) {
			Thread.currentThread().setName("NIO-FORWARDER");
			final NioForwardTCP forwarder = type.newInstance();
			forwarder.listen(host, port, backlog);
			forwarder.loopSelect(timeout);
			return;
		}

		NioForwardTCP[] workers = new NioForwardTCP[workerCount];
		for (int i = 0; i < workers.length; i++) {
			final NioForwardTCP worker = workers[i] = type.newInstance();

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						worker.loopSelect(timeout);
					} catch (IOException exc) {
						exc.printStackTrace();
					}
				}
			}, "NIO-WORKER-" + i).start();
		}

		Thread.currentThread().setName("NIO-ACCEPTER");

		final NioForwardTCP accepter = type.newInstance();
		accepter.listen(host, port, backlog);
		while (true) {

			accepter.selector.select();

			Iterator<SelectionKey> selectedKeys = accepter.selector.selectedKeys().iterator();
			while (selectedKeys.hasNext()) {
				SelectionKey key = selectedKeys.next();
				selectedKeys.remove();

				if (key.isValid() && key.isAcceptable()) {
					int index = (int) (Math.random() * workers.length);
					final NioForwardTCP worker = workers[index];
					accepter.onAcceptWorker(key, worker);
				}
			}
		}
	}

	public final Selector selector;

	public NioForwardTCP() throws IOException {
		selector = SelectorProvider.provider().openSelector();
	}

	public void listen(String host, int port, int backlog) throws IOException {

		ServerSocketChannel serverChannel = SelectorProvider.provider().openServerSocketChannel();
		serverChannel.configureBlocking(false);
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		serverChannel.socket().bind(new InetSocketAddress(host, port), backlog);

		log("NIO", "Listening: " + serverChannel.socket().getInetAddress().getHostAddress() + " @ " + serverChannel.socket().getLocalPort());
	}

	private List<Runnable> newTasks = new LinkedList<>();
	private List<Runnable> currTasks = new LinkedList<>();

	public void addTask(Runnable task) {
		synchronized (newTasks) {
			newTasks.add(task);
		}
		selector.wakeup();
		System.out.println("WAKEUP");
	}

	long cumulativeRecv = 0;
	long cumulativeSent = 0;

	public void loopSelect(int timeout) throws IOException {

		int gentleClosingInterval = 8_000;
		int forcedClosingInterval = 16_000;

		long lastTimeoutCheck = System.currentTimeMillis();
		long lastClosingCheck = System.currentTimeMillis();
		long lastBandwidthStats = System.currentTimeMillis();

		Set<NioForwardClient> affectedClients = new HashSet<>();

		ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
		long lastSysTime = threadBean.getCurrentThreadCpuTime();
		long lastUsrTime = threadBean.getCurrentThreadUserTime();

		int cumulativeSelectCount = 0;
		int cumulativeSpuriousSelectCount = 0;
		int spuriousSelectCount = 0;
		while (true) {

			tickTime = System.currentTimeMillis();

			this.loopHook();

			if (tickTime - lastClosingCheck > gentleClosingInterval / 3) {
				lastClosingCheck = tickTime;

				for (SelectionKey key : selector.keys()) {
					NioForwardClient client = (NioForwardClient) key.attachment();
					if (client == null || !client.isClosing()) {
						continue;
					}

					if (!client.hasDataPending()) {
						log("NIO", "Closing GENTLE " + client);
						client.closeNow();
					} else if ((tickTime - client.closingIO) > forcedClosingInterval) {
						log("NIO", "Closing FORCED " + client + ", inbound=" + client.inbound + ", outbound1=" + client.outbound1 + ", outbound2=" + client.outbound2);
						client.closeNow();
					}
				}
			}

			if (tickTime - lastTimeoutCheck > timeout / 3) {
				lastTimeoutCheck = tickTime;

				for (SelectionKey key : selector.keys()) {
					NioForwardClient client = (NioForwardClient) key.attachment();
					if (client == null || client.lastIO == 0) {
						continue;
					}

					if ((tickTime - client.lastIO) >= timeout) {
						log("NIO", "Timeout " + client);
						client.closeNow();
					}
				}
			}

			cumulativeSelectCount++;
			if (tickTime - lastBandwidthStats > 10_000L) {

				long currSysTime = threadBean.getCurrentThreadCpuTime();
				long currUsrTime = threadBean.getCurrentThreadUserTime();
				long sysTook = (currSysTime - lastSysTime) / 1_000_000L;
				long usrTook = (currUsrTime - lastUsrTime) / 1_000_000L;
				lastSysTime = currSysTime;
				lastUsrTime = currUsrTime;

				double time = ((tickTime - lastBandwidthStats) / 1000.0);
				String sent = TextValues.formatNumber(cumulativeSent / time / 1024.0, 1) + "K";
				String recv = TextValues.formatNumber(cumulativeRecv / time / 1024.0, 1) + "K";
				log("PERF", "STATS selects=" + (cumulativeSelectCount - cumulativeSpuriousSelectCount) + "/" + cumulativeSelectCount + ", time=" + time + ", sent=" + sent + "/sec, recv=" + recv + "/sec, log=" + (LOG_TIME_NANOS / 1_000_000L) + "ms, sys=" + sysTook + "ms, usr=" + usrTook + "ms");
				cumulativeSent = 0;
				cumulativeRecv = 0;
				cumulativeSelectCount = 0;
				cumulativeSpuriousSelectCount = 0;
				LOG_TIME_NANOS = 0;

				lastBandwidthStats = tickTime;
			}

			long blockFor = gentleClosingInterval / 3;
			long t0 = System.nanoTime();
			int selectCount = selector.select(blockFor);
			long t1 = System.nanoTime();

			// System.out.println("SELECT[" + Thread.currentThread().getName() +
			// "]=" + selectCount + " / " + selector.keys().size());

			{
				synchronized (newTasks) {
					currTasks.addAll(newTasks);
					newTasks.clear();
				}
				for (Runnable task : currTasks) {
					// System.out.println("TASK");
					task.run();
				}
				currTasks.clear();
			}

			Set<NioForwardClient> activeClients = new HashSet<>();

			int selectedKeysCount = 0;
			Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
			while (selectedKeys.hasNext()) {
				SelectionKey key = selectedKeys.next();
				selectedKeys.remove();
				selectedKeysCount++;

				try {
					if (key.isValid() && key.isAcceptable()) {
						onAccept(key, this);
					}

					if (key.isValid() && key.isConnectable()) {
						activeClients.add(onConnect(key));
					}

					if (key.isValid() && key.isReadable()) {
						activeClients.add(onRead(key));
					}

					if (key.isValid() && key.isWritable()) {
						activeClients.add(onWrite(key));
					}
				} catch (IOException | BufferUnderflowException | BufferOverflowException exc) {
					exc.printStackTrace();

					if (key.attachment() instanceof NioForwardClient) {
						((NioForwardClient) key.attachment()).closeNow();
					} else {
						NioForwardClient.close(key);
					}
				}
			}

			affectedClients.clear();
			for (NioForwardClient client : activeClients) {
				affectedClients.add(client);
				if (client.other != null) {
					affectedClients.add(client.other);
				}
			}

			for (NioForwardClient client : affectedClients) {
				client.verifyInboundState();
				client.verifyOutboundState();

				if (!client.key.isValid()) {
					continue;
				}

				client.setWriteOp(client.outbound1 != null);

				if (client.isClosing() || (client.inbound != null)) {
					client.setReadOp(false);
				} else {
					client.setReadOp((client.other == null || client.other.outbound2 == null));
				}
			}

			if (selectCount == 0 && selectedKeysCount == 0 && (t1 - t0) < (blockFor * 1_000_000) / 2) {
				spuriousSelectCount++;
				cumulativeSpuriousSelectCount++;

				if (spuriousSelectCount > 300) {
					log("NIO", "why did we wakeup? ns=" + (t1 - t0) + "/" + (blockFor * 1_000_000));
					for (SelectionKey key : selector.keys()) {
						NioForwardClient client = (NioForwardClient) key.attachment();
						if (client == null) {
							log("NIO", "\tIDLE: " + NioForwardClient.toStringOps(key, key.readyOps()) + " / " + NioForwardClient.toStringOps(key, key.interestOps()));
						} else {
							log("NIO", "\tIDLE: " + client.toDetailedString());
						}
					}
					HighLevel.sleep(1000L);
				} else if (spuriousSelectCount > 100) {
					HighLevel.sleep(250L);
				} else if (spuriousSelectCount > 10) {
					HighLevel.sleep(100L);
				} else if (spuriousSelectCount > 2) {
					HighLevel.sleep(50L);
				} else {
					HighLevel.sleep(25L);
				}
			} else {
				spuriousSelectCount = 0;
			}
		}
	}

	public NioForwardClient onAccept(SelectionKey key, NioForwardTCP worker) throws IOException {
		SelectionKey clientKey = nioAccept(key, worker.selector);
		NioForwardClient clientClient = new NioForwardClient(worker, clientKey, true);
		clientClient.state = INIT_STATE;
		clientKey.attach(clientClient);
		return clientClient;
	}

	public void onAcceptWorker(SelectionKey key, NioForwardTCP worker) throws IOException {
		nioAcceptWorker(key, worker);
	}

	public NioForwardClient onConnect(SelectionKey key) {
		try {
			if (!((SocketChannel) key.channel()).finishConnect()) {
				throw new EOFException("connect failed");
			}
			key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
		} catch (IOException exc) {
			onConnectFailed(key, exc);
		}
		return (NioForwardClient) key.attachment();
	}

	public void onConnectFailed(SelectionKey key, IOException exc) {
		log("NIO", "Connect failed: " + ((SocketChannel) key.channel()).socket().getInetAddress());
		((NioForwardClient) key.attachment()).closeNow();
	}

	static final int BUFFER_SIZE = 8 * 1024;
	static final byte[] TMP_ARRAY = new byte[BUFFER_SIZE];

	public NioForwardClient onRead(SelectionKey key) {
		NioForwardClient client = (NioForwardClient) key.attachment();
		// System.out.println("ON_READ: " + client);

		client.verifyInboundState();
		client.verifyOutboundState();

		int got = client.onRead();
		if (got == 0) {
			client.setReadOp(false);
			System.out.println();
			System.out.println(client.toDetailedString());
			if (client.other != null) {
				client.other.setWriteOp(true);
				System.out.println(client.other.toDetailedString());

				client.other.writeLater(client.inbound);
			}
			if (client.other == null || client.other.isClosing()) {
				System.out.println("QUIT!");
				client.closeNow();
				return client;
			}
		}
		if (got == -1) {
			client.closeLater();
			return client;
		}

		if (client.fromAccept) {
			cumulativeRecv += got;
		}

		int pre, count = 0;
		do {
			pre = client.state;
			try {
				this.handleReadState(client);
			} finally {
				client.tidyInbound();

				if (++count > 10) {
					throw new IllegalStateException("WARNING: inlikely long state switching! current state is " + client.state);
				}
			}
		} while (pre != client.state && client.inbound != null);

		return client;
	}

	public void loopHook() throws IOException {
		//
	}

	public void handleReadState(NioForwardClient client) {

		switch (client.state) {
			case PUMP_STATE:
				if (client.other != null) {
					client.other.writeLater(client.inbound);
				}
				break;

			case INIT_STATE:
				InetSocketAddress targetAddr = getTarget(null);
				if (targetAddr == null) {
					client.closeNow();
					break;
				}

				SelectionKey key;
				try {
					key = nioConnect(client.key.selector(), targetAddr);
				} catch (IOException exc) {
					client.closeNow();
					break;
				}

				String clientAddr = client.channel.socket().getInetAddress().getHostAddress();
				log("TCP", "Forward for: " + clientAddr + " => " + targetAddr);

				NioForwardClient target = new NioForwardClient(this, key, false);
				target.other = client;
				client.other = target;
				target.key.attach(target);

				client.state = PUMP_STATE;
				break;

			default:
				throw new IllegalStateException("state=" + client.state);
		}
	}

	public SelectionKey nioAccept(SelectionKey key, Selector useSelector) throws IOException {
		return nioRegisterNow(nioAcceptRegisterLater(key), useSelector);
	}

	public void nioAcceptWorker(SelectionKey key, final NioForwardTCP worker) throws IOException {
		final SocketChannel client = this.nioAcceptRegisterLater(key);

		worker.addTask(new Runnable() {
			@Override
			public void run() {
				try {
					SelectionKey clientKey = nioRegisterNow(client, worker.selector);

					NioForwardClient clientClient = new NioForwardClient(worker, clientKey, true);
					clientClient.state = INIT_STATE;
					clientKey.attach(clientClient);
				} catch (IOException exc) {
					throw new IllegalStateException(exc);
				}
			}
		});
	}

	public SocketChannel nioAcceptRegisterLater(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel channel = serverSocketChannel.accept();
		channel.configureBlocking(false);
		channel.socket().setTcpNoDelay(true);
		return channel;
	}

	public SelectionKey nioRegisterNow(SocketChannel channel, Selector useSelector) throws IOException {
		return channel.register(useSelector, SelectionKey.OP_READ);
	}

	public SelectionKey nioConnect(Selector selector, InetSocketAddress addr) throws IOException {
		SocketChannel channel = SelectorProvider.provider().openSocketChannel();
		channel.configureBlocking(false);
		channel.socket().setTcpNoDelay(true);
		SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
		channel.connect(addr);
		return key;
	}

	public NioForwardClient onWrite(SelectionKey key) {
		NioForwardClient client = (NioForwardClient) key.attachment();
		// System.out.println("ON_WRITE: " + client);

		client.verifyInboundState();
		client.verifyOutboundState();

		int sent = client.onWrite();
		if (sent == -1) {
			client.closeNow();
		} else {
			if (client.fromAccept) {
				cumulativeSent += sent;
			}
		}
		return client;
	}

	public static int indexOf(ByteBuffer bb, byte[] pattern) {
		if (bb.remaining() < pattern.length) {
			return -1;
		}
		for (int i = 0, off = 0, pos = bb.position(), rem = bb.remaining(); i < rem; i++) {
			if (bb.get(pos + i) != pattern[off]) {
				off = 0;
			} else if (++off == pattern.length) {
				return i - pattern.length + 1;
			}
		}
		return -1;
	}

	public ByteBuffer[] DUO = new ByteBuffer[2];

	public Pool<ByteBuffer> bufferPool = new Pool<ByteBuffer>(new PoolHandler<ByteBuffer>() {
		private int counter;
		private final LinkedList<ByteBuffer> newBuffers = new LinkedList<>();

		@Override
		public ByteBuffer create() {
			log("MEM", "ByteBuffer Pool: created " + (++counter) + " buffers");

			if (newBuffers.isEmpty()) {
				int mergeCount = 4;

				ByteBuffer bb = allocateDirectAligned(BUFFER_SIZE * mergeCount);
				splitPageAligned(bb, BUFFER_SIZE, newBuffers);
				if (newBuffers.size() != mergeCount) {
					throw new IllegalStateException();
				}

				for (ByteBuffer newBuffer : newBuffers) {
					newBuffer.order(ByteOrder.nativeOrder());
				}
			}

			return newBuffers.removeFirst();
		}

		public void clean(ByteBuffer buffer) {
			buffer.clear().flip();
		}
	}, 16/* initial in pool */, (8 * 1024 * 1024) / BUFFER_SIZE /* max in pool */);

	private static void splitPageAligned(ByteBuffer bb, int sliceSize, List<ByteBuffer> dst) {
		bb = bb.slice();

		while (bb.remaining() >= sliceSize) {
			bb.clear().limit(sliceSize);
			dst.add(verifyPageAligned(bb.slice()));

			bb.clear().position(sliceSize);
			bb = pageAlign(bb.slice());
		}
	}

	static final int PAGE_SIZE = 4096;
	static final int PAGE_BITS = 12;
	static final int PAGE_MASK = PAGE_SIZE - 1;

	private static ByteBuffer allocateDirectAligned(int bytes) {
		return pageAlign(ByteBuffer.allocateDirect(bytes + PAGE_MASK));
	}

	private static ByteBuffer pageAlign(ByteBuffer bb) {

		long curr = NativeHacks.getBufferAddress(bb);
		long prev = curr;

		// align ByteBuffer to page
		curr += ((-(curr & PAGE_MASK)) >>> 63) << PAGE_BITS;
		curr &= ~PAGE_MASK;

		bb.position((int) (curr - prev));

		return verifyPageAligned(bb.slice());
	}

	private static ByteBuffer verifyPageAligned(ByteBuffer bb) {
		if ((NativeHacks.getBufferAddress(bb) & PAGE_MASK) != 0) {
			throw new IllegalStateException();
		}
		return bb;
	}
}