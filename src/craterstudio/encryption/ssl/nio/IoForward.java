package craterstudio.encryption.ssl.nio;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import craterstudio.data.TimeSortedQueue;
import craterstudio.func.Callback;
import craterstudio.io.Streams;
import craterstudio.io.TcpServer;
import craterstudio.misc.MainParams;
import craterstudio.text.Text;
import craterstudio.text.TextValues;
import craterstudio.time.Clock;
import craterstudio.util.HighLevel;

public abstract class IoForward {

	public static ThreadMXBean THREAD_MBEAN = ManagementFactory.getThreadMXBean();

	public static void main(String[] args) throws IOException {

		if (args.length == 0) {
			System.out.println("Usage: [SOCKS|HTTP|NAT]");
			return;
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
					while (true) {
						String line = br.readLine();

						Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
						Map<Thread, Long> threadToUsr = new HashMap<>();
						Map<Thread, Long> threadToSys = new HashMap<>();
						for (Entry<Thread, StackTraceElement[]> entry : map.entrySet()) {
							threadToSys.put(entry.getKey(), Long.valueOf(THREAD_MBEAN.getThreadCpuTime(entry.getKey().getId())));
							threadToUsr.put(entry.getKey(), Long.valueOf(THREAD_MBEAN.getThreadUserTime(entry.getKey().getId())));
						}

						HighLevel.sleep(1000);

						for (Entry<Thread, StackTraceElement[]> entry : map.entrySet()) {
							System.out.println(entry.getKey());
							long currSysTime = THREAD_MBEAN.getThreadCpuTime(entry.getKey().getId());
							long currUsrTime = THREAD_MBEAN.getThreadUserTime(entry.getKey().getId());
							long prevSysTime = threadToSys.get(entry.getKey()).longValue();
							long prevUsrTime = threadToUsr.get(entry.getKey()).longValue();

							String totalSys = (currSysTime / 1000000) + "ms";
							String totalUsr = (currUsrTime / 1000000) + "ms";
							String deltaSys = ((currSysTime - prevSysTime) / 1000000) + "ms";
							String deltaUsr = ((currUsrTime - prevUsrTime) / 1000000) + "ms";
							System.out.println("\t\ttotal.usr=" + totalUsr + ", total.sys=" + totalSys + " delta.usr=" + deltaUsr + ", delta.sys=" + deltaSys);
							for (StackTraceElement elem : entry.getValue()) {
								System.out.println("\t" + elem);
							}
							System.out.println();
						}
					}
				} catch (IOException exc) {
					exc.printStackTrace();
				}

			}
		}).start();

		String cmd = args[0];
		args = Arrays.copyOfRange(args, 1, args.length);
		MainParams params = new MainParams();

		if (cmd.equals("SOCKS")) {
			params.addProps("host", "port", "backlog", "timeout", "workers", "conf");
			params.parse(args);
			new IoForwardSOCKS(params).listen(params);
		} else if (cmd.equals("HTTP")) {
			params.addProps("host", "port", "backlog", "timeout", "workers", "conf");
			params.parse(args);
			new IoForwardHTTP(params).listen(params);
		} else if (cmd.equals("NAT")) {
			params.addProps("host", "port", "backlog", "timeout", "workers", "conf");
			params.parse(args);
			new IoForwardNAT(params).listen(params);
		} else {
			throw new IllegalArgumentException("Command: " + cmd);
		}
	}

	static class IoForwardNAT extends IoForward {

		private final LiveFile conf;
		private Map<String, String> natTable;

		public IoForwardNAT(MainParams params) {
			natTable = new HashMap<>();

			this.conf = new LiveMapFile(new File(params.get("conf")), 10_000L) {
				@Override
				public void onMapUpdate(Map<String, String> map) {
					IoForwardNAT.this.natTable = map;
				}
			};
		}

		@Override
		public Socket determineTarget(Socket client, InputStream clientSrc, OutputStream clientDst) throws IOException {
			this.conf.poll();

			String hostportSrc = client.getLocalAddress().getHostAddress() + ":" + client.getLocalPort();
			String hostportDst = natTable.get(hostportSrc);

			if (hostportDst == null) {
				throw new EOFException("no mapping for: " + hostportSrc);
			}

			String hostDst = Text.beforeLast(hostportDst, ':');
			int portDst = Integer.parseInt(Text.afterLast(hostportDst, ':'));
			return new Socket(hostDst, portDst);
		}
	}

	static class IoForwardHTTP extends IoForward {

		private final LiveFile conf;
		private Map<String, String> hostTable;

		public IoForwardHTTP(MainParams params) {
			hostTable = new HashMap<>();

			this.conf = new LiveMapFile(new File(params.get("conf")), 10_000L) {
				@Override
				public void onMapUpdate(Map<String, String> map) {
					IoForwardHTTP.this.hostTable = map;
				}
			};
		}

		@Override
		public Socket determineTarget(Socket client, InputStream clientSrc, OutputStream clientDst) throws IOException {

			final ByteArrayOutputStream response = new ByteArrayOutputStream();

			String hostportSrc = null;

			while (true) {
				String line = Streams.binaryReadLineAsString(clientSrc);
				if (line == null || line.equals("")) {
					throw new EOFException("No host in http header");
				}

				response.write(Text.utf8(line + "\r\n"));

				if (line.startsWith("Host: ")) {
					hostportSrc = line.substring(6);

					break;
				}
			}

			String hostSrc = Text.beforeLastIfAny(hostportSrc, ':');

			this.conf.poll();
			String hostportDst = hostTable.get(hostSrc);
			if (hostportDst == null) {
				throw new EOFException("no host for hostname: " + hostSrc);
			}

			String host = Text.beforeLastIfAny(hostportDst, ':');
			String port = Text.afterLast(hostportDst, ":", "80");

			Socket target = new Socket(host, TextValues.tryParseInt(port, 80));

			response.write(Text.utf8("X-Forwarded-For: " + client.getInetAddress().getHostAddress() + "\r\n"));
			target.getOutputStream().write(response.toByteArray());
			return target;
		}
	}

	static class IoForwardSOCKS extends IoForward {

		private final LiveSetFile conf;
		private Set<String> ipTable;

		public IoForwardSOCKS(MainParams params) {
			ipTable = new HashSet<>();

			this.conf = new LiveSetFile(new File(params.get("conf")), 10_000L) {
				@Override
				public void onSetUpdate(Set<String> set) {
					IoForwardSOCKS.this.ipTable = set;
				}
			};
		}

		@Override
		public Socket determineTarget(Socket client, InputStream clientSrc, OutputStream clientDst) throws IOException {
			this.conf.poll();
			if (!ipTable.contains(client.getInetAddress().getHostAddress())) {
				throw new EOFException("Unexpected client: " + client.getInetAddress().getHostAddress());
			}

			final DataInputStream clientSrcData = new DataInputStream(clientSrc);

			final int version = clientSrcData.readUnsignedByte();

			final ByteArrayOutputStream response = new ByteArrayOutputStream();

			if (version == 4) {
				int command = clientSrcData.readUnsignedByte();
				if (command != 1) {
					throw new EOFException("Unexpected command");
				}

				int port = clientSrcData.readUnsignedShort();

				byte[] addr = new byte[4];
				clientSrcData.readFully(addr);

				byte[] userid = new byte[0xff];
				for (byte got, p = 0; (got = clientSrcData.readByte()) != 0x00; p++) {
					userid[p & 0xff] = got;
				}

				InetAddress iaddr;
				try {
					iaddr = InetAddress.getByAddress(addr);
				} catch (UnknownHostException exc) {
					throw new EOFException("Unknown host");
				}

				Socket target = new Socket(iaddr, port);

				response.write((byte) 0x00);
				response.write((byte) 0x5a); // ACCEPTED
				for (int i = 0; i < 2; i++)
					response.write((byte) 0x00);
				for (int i = 0; i < 4; i++)
					response.write((byte) 0x00);

				clientDst.write(response.toByteArray());
				clientDst.flush();
				response.reset();

				return target;
			}

			if (version == 5) {

				byte[] auths = new byte[clientSrcData.readUnsignedByte()];
				clientSrcData.readFully(auths);

				System.out.println("SOCKS5 AUTHS");
				for (int i = 0; i < auths.length; i++) {
					System.out.println("\t" + auths[i]);
				}

				response.write((byte) 0x05);
				response.write((byte) 0x00); // NO AUTH
				clientDst.write(response.toByteArray());
				clientDst.flush();
				response.reset();

				int version2 = clientSrcData.readUnsignedByte();
				int command = clientSrcData.readUnsignedByte();
				int reserved = clientSrcData.readUnsignedByte();
				int addressType = clientSrcData.readUnsignedByte();

				if (version2 != 5 || command != 1 || reserved != 0) {
					throw new EOFException("Unexpected header: version=" + version2 + ", command=" + command);
				}

				byte[] addr;
				InetAddress iaddr;
				try {
					if (addressType == 1 || addressType == 4) {
						addr = new byte[addressType * 4];
						clientSrcData.readFully(addr);
						iaddr = InetAddress.getByAddress(addr);
					} else if (addressType == 3) {
						addr = new byte[clientSrcData.readUnsignedByte()];
						clientSrcData.readFully(addr);
						iaddr = InetAddress.getByName(Text.ascii(addr));
					} else {
						throw new EOFException("Unexpected address type: " + addressType);
					}
					System.out.println("SOCKS5 resolved: " + iaddr);
				} catch (UnknownHostException exc) {
					exc.printStackTrace();
					addr = null;
					iaddr = null;
				}

				int port = clientSrcData.readUnsignedShort();

				if (iaddr == null) {
					response.write((byte) 0x05);
					response.write((byte) 0x04); // host unreachable
					response.write((byte) 0x00); // reserved
					response.write((byte) addressType); // addr-type
					response.write((byte) 0x00); // NULL host
					response.write((byte) 0x00); // NULL port hi
					response.write((byte) 0x00); // NULL port lo
					response.flush();
					throw new EOFException();
				}

				Socket target;
				try {
					target = new Socket(iaddr, port);
				} catch (IOException exc) {
					response.write((byte) 0x05);
					response.write((byte) 0x05); // connection refused
					response.write((byte) 0x00); // reserved
					response.write((byte) addressType); // addr-type
					response.write((byte) 0x00); // NULL host
					response.write((byte) 0x00); // NULL port hi
					response.write((byte) 0x00); // NULL port lo
					response.flush();
					throw new EOFException();
				}

				{
					response.write((byte) 0x05);
					response.write((byte) 0x00); // succeeded
					response.write((byte) 0x00); // reserved
					response.write((byte) addressType); // addr-type
					if (addressType == 3) {
						response.write((byte) addr.length);
					}
					response.write(addr); // host
					response.write((byte) (port >> 8)); // NULL port hi
					response.write((byte) (port >> 0)); // NULL port lo
					response.flush();
					return target;
				}
			}

			throw new EOFException();
		}
	}

	public abstract Socket determineTarget(Socket client, InputStream clientSrc, OutputStream clientDst) throws IOException;

	public void listen(final MainParams params) throws IOException {

		System.out.println("listen: host: " + params.get("host") + ", port: " + params.getInt("port"));

		final int timeout = params.getInt("timeout") * 1000;

		final TimeSortedQueue<Runnable> queue = new TimeSortedQueue<Runnable>();

		queue.spawnPollLoop(new Callback<Runnable>() {
			@Override
			public void callback(Runnable task) {

			}
		}, 1_000);

		//

		final ExecutorService pool = TcpServer.pool(params.getInt("workers"), 30_000, 48 * 1024);

		TcpServer.listen(new ServerSocket(params.getInt("port"), params.getInt("backlog"), InetAddress.getByName(params.get("host"))), new Callback<Socket>() {
			@Override
			public void callback(final Socket client) {
				Socket target = null;

				try {
					final InputStream clientSrc = client.getInputStream();
					final OutputStream clientDst = client.getOutputStream();

					client.setSoTimeout(10_000);					
					
					target = determineTarget(client, clientSrc, clientDst);
					if (target == null) {
						throw new NullPointerException();
					}

					final String id = client.getInetAddress().getHostAddress() + ":" + client.getPort() + " => " + target.getInetAddress().getHostAddress() + ":" + target.getPort();

					System.out.println(id + " Bridged");

					client.setTcpNoDelay(true);
					target.setTcpNoDelay(true);

					client.setSoTimeout(0);
					target.setSoTimeout(0);

					// ---

					pump(id, timeout, pool, queue, client, clientSrc, clientDst, target, target.getInputStream(), target.getOutputStream());

				} catch (IOException exc) {
					Streams.safeClose(target);
					Streams.safeClose(client);
				}
			}
		}, pool, false);
	}

	static void pump(//
	   final String id, final int timeout, //
	   final ExecutorService pool, final TimeSortedQueue<Runnable> queue, //
	   final Socket client, final InputStream clientSrc, final OutputStream clientDst, //
	   final Socket target, final InputStream targetSrc, final OutputStream targetDst) {

		final AtomicLong lastIO = new AtomicLong(Clock.now());
		final AtomicLong lastClose = new AtomicLong();

		final Future<?> targetToClient = pool.submit(new Runnable() {
			@Override
			public void run() {
				try {
					copy(targetSrc, clientDst, lastIO);
				} catch (IOException exc) {
					// exc.printStackTrace();
				} finally {
					lastClose.set(Clock.now());
				}
			}
		});

		final Future<?> clientToTarget = pool.submit(new Runnable() {
			@Override
			public void run() {
				try {
					copy(clientSrc, targetDst, lastIO);
				} catch (IOException exc) {
					// exc.printStackTrace();
				} finally {
					lastClose.set(Clock.now());
				}
			}
		});

		queue.insert(1_000L, new Runnable() {
			@Override
			public void run() {
				if (targetToClient.isDone() && clientToTarget.isDone()) {
					System.out.println(id + " Died");

					Streams.safeClose(target);
					Streams.safeClose(client);
					return;
				}

				if ((Clock.now() - lastIO.get()) > timeout) {
					System.out.println(id + " Timeout");

					Streams.safeClose(target);
					Streams.safeClose(client);
					return;
				}

				if (lastClose.get() != 0L && (Clock.now() - lastClose.get()) > 4_000L) {
					System.out.println(id + " Disconnect");

					Streams.safeClose(target);
					Streams.safeClose(client);
					return;
				}

				// reschedule
				queue.insert(Clock.now() + 4_000L, this);
			}
		});
	}

	static void copy(InputStream src, OutputStream dst, AtomicLong lastIO) throws IOException {
		byte[] tmp = new byte[4 * 1024];
		for (int got; (got = src.read(tmp)) > 0;) {
			dst.write(tmp, 0, got);
			lastIO.set(Clock.now());
		}

		src.close();
		dst.close();
	}
}