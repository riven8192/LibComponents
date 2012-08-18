package craterstudio.encryption.ssl.nio;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import craterstudio.text.Text;
import craterstudio.text.TextValues;

public class NioForwardSOCKS extends NioForwardTCP {

	public static void main(String[] args) throws Exception {

		params.addProps("host", "port", "backlog", "timeout", "workers");
		params.parse(args);

		LOG_PREFIX = "SOCKS";

		NioForwardTCP.loop(NioForwardSOCKS.class, //
		   params.get("host"), //
		   params.getInt("port"), //
		   params.getInt("backlog"), //
		   params.getInt("timeout") * 1000, //
		   params.getInt("workers") //
		   );
	}

	public NioForwardSOCKS() throws IOException {
		super();
	}

	@Override
	public void loopHook() throws IOException {
		//
	}

	@Override
	public final InetSocketAddress getTarget(String httpHeaderHost) {
		throw new IllegalStateException();
	}

	static final int SOCKS4_BIND = 4001;
	static final int SOCKS5_AUTH = 5001;
	static final int SOCKS5_BIND = 5002;
	static final int HTTP_LOG = 10001;
	static final int FTP_LOG = 10002;

	@Override
	public void handleReadState(NioForwardClient client) {
		if (client.inbound == null) {
			System.out.println("handleReadState client.inbound==0");
			return;
		}

		switch (client.state) {
			case PUMP_STATE:
				if (client.other != null) {
					client.other.writeLater(client.inbound);
				}
				break;

			case INIT_STATE:

				String addr = client.getAddress();

				ACCEPT_FILE.poll();
				if (!ACCEPT_SET.contains(addr)) {
					log("SOCKS", "ACCESS DENIED FOR HOST: " + addr);
					client.closeNow();
					return;
				}

				log("SOCKS", "ACCESS GRANTED FOR HOST: " + addr);

				int version = client.inbound.get() & 0xFF;
				client.inbound.compact().flip();

				if (version == 4) {
					client.state = SOCKS4_BIND;
				} else if (version == 5) {
					client.state = SOCKS5_AUTH;
				} else {
					log("SOCKS", "VERSION ERROR: " + version);
					client.closeNow();
					return;
				}
				break;

			case SOCKS4_BIND:
				this.handleSocks4(client);
				break;

			case SOCKS5_AUTH:
				this.handleSocks5a(client);
				break;

			case SOCKS5_BIND:
				this.handleSocks5b(client);
				break;

			case HTTP_LOG:
				if (client.other == null) {
					break;
				}

				if (indexOf(client.inbound, NioForwardHTTP.HTTP_END_OF_HEADER) != -1) {
					ByteBuffer bb = client.inbound.duplicate();
					byte[] raw = new byte[bb.remaining()];
					bb.get(raw);

					String httpHeader = Text.ascii(raw);
					String httpAction = Text.between(httpHeader, " /", " ");
					String httpHost = Text.between(httpHeader, "\r\nHost: ", "\r\n");
					if (httpAction != null && httpHost != null) {
						log("HTTP", client.getAddress() + " " + httpHost + "/" + httpAction);
					}
				}

				client.other.writeLater(client.inbound);
				break;

			case FTP_LOG:
				if (client.other == null) {
					break;
				}

				if (client.inbound.remaining() > 5) {
					ByteBuffer local = client.inbound.slice();
					int io = NioForwardTCP.indexOf(local, Text.ascii("USER "));
					if (io != -1) {
						local.position(io + 5);
						io = NioForwardTCP.indexOf(local, Text.ascii("\r\n"));

						if (io != -1) {
							byte[] user = new byte[io];
							local.get(user);
							System.out.println("FTP USER: " + Text.ascii(user));
						}
					}
				}

				client.other.writeLater(client.inbound);
				break;

			default:
				throw new IllegalStateException("state=" + client.state);
		}
	}

	private void handleSocks4(NioForwardClient client) {
		int command = client.inbound.get() & 0xFF;
		if (command != 1) {
			log("SOCKS", "COMMAND ERROR: " + command);
			client.closeNow();
			return;
		}

		int portHi = client.inbound.get() & 0xFF;
		int portLo = client.inbound.get() & 0xFF;

		byte[] addr = new byte[4];
		client.inbound.get(addr);

		byte[] userid = new byte[256];
		for (byte got, p = 0; (got = client.inbound.get()) != 0x00; p++) {
			userid[p] = got;
		}

		int port = (portHi << 8) | portLo;
		InetAddress iaddr;
		try {
			iaddr = InetAddress.getByAddress(addr);
		} catch (UnknownHostException exc) {
			log("SOCKS", "DNS ERROR: " + exc.getMessage());
			client.closeNow();
			return;
		}

		SelectionKey key;
		try {
			InetSocketAddress targetAddr = new InetSocketAddress(iaddr, port);
			key = super.nioConnect(client.key.selector(), targetAddr);
		} catch (IOException exc) {
			log("SOCKS", "CONNECT ERROR: " + exc.getMessage());
			client.closeNow();
			return;
		}

		ByteBuffer packet = bufferPool.aquire();
		packet.clear();
		packet.put((byte) 0x00);
		packet.put((byte) 0x5a); // ACCEPTED
		for (int i = 0; i < 2; i++)
			packet.put((byte) 0x00);
		for (int i = 0; i < 4; i++)
			packet.put((byte) 0x00);
		packet.flip();
		client.writeLater(packet);
		bufferPool.release(packet);

		couple(client, new NioForwardClient(this, key, false), port);
	}

	private void couple(NioForwardClient client, NioForwardClient target, int port) {
		target.other = client;
		client.other = target;
		target.key.attach(target);

		log("SOCKS", client + " => " + target);

		if (!client.fromAccept) {
			client.state = PUMP_STATE;
			return;
		}

		switch (port) {
			case 21:
				client.state = FTP_LOG;
				break;

			case 80:
				client.state = HTTP_LOG;
				break;

			default:
				client.state = PUMP_STATE;
				break;
		}
	}

	private void handleSocks5a(NioForwardClient client) {
		byte[] auths = new byte[client.inbound.get() & 0xff];
		client.inbound.get(auths);

		ByteBuffer packet = bufferPool.aquire();
		packet.clear();
		packet.put((byte) 0x05);
		packet.put((byte) 0x00); // no auth
		packet.flip();
		client.writeLater(packet);
		bufferPool.release(packet);

		client.state = SOCKS5_BIND;
	}

	static Set<String> ACCEPT_SET = null;
	static LiveFile ACCEPT_FILE = new LiveFile(new File("./nio-forward-socks.accept"), 5_000L) {

		@Override
		public void onUpdate(byte[] data) {
			ACCEPT_SET = new HashSet<>();

			for (String line : Text.splitOnLines(Text.ascii(data))) {
				if ((line = Text.beforeIfAny(line.trim(), '#').trim()).isEmpty()) {
					continue;
				}

				for (String word : Text.splitOnWhiteSpace(line)) {
					ACCEPT_SET.add(word);
				}
			}
		}
	};

	static Map<String, byte[]> HOSTS_MAP = null;
	static LiveFile HOSTS_FILE = new LiveFile(new File("./nio-forward-socks.hosts"), 5_000L) {

		@Override
		public void onUpdate(byte[] data) {
			HOSTS_MAP = new HashMap<>();

			for (String line : Text.splitOnLines(Text.ascii(data))) {
				if ((line = Text.beforeIfAny(line.trim(), '#').trim()).isEmpty()) {
					continue;
				}

				String[] parts = Text.splitOnWhiteSpace(line);
				if (parts.length < 2) {
					continue;
				}

				String ip = parts[0];
				String nm = parts[1];

				int[] digits = TextValues.parseInts(Text.split(ip, '.'));
				byte[] addr = new byte[digits.length];
				for (int i = 0; i < digits.length; i++) {
					addr[i] = (byte) digits[i];
				}

				HOSTS_MAP.put(nm, addr);
			}
		}
	};

	private static InetAddress resolveHostname(String hostname) {
		log("DNS", "Resolve " + hostname);

		HOSTS_FILE.poll();
		if (HOSTS_MAP != null) {
			byte[] addr = HOSTS_MAP.get(hostname);
			if (addr != null) {

				log("DNS", "Resolving using hosts-file: " + hostname);

				try {
					InetAddress iaddr = InetAddress.getByAddress(addr);
					log("DNS", "Resolved using hosts-file: " + iaddr);
					return iaddr;
				} catch (UnknownHostException exc2) {
					log("SOCKS", "DNS ERROR 2: " + exc2.getMessage());
				}
			}
		}

		try {
			InetAddress iaddr = InetAddress.getByName(hostname);
			log("DNS", "Resolved using hosts-file: " + hostname + " => " + iaddr);
			return iaddr;
		} catch (UnknownHostException exc2) {
			log("SOCKS", "DNS ERROR 3: " + exc2.getMessage());
		}

		return null;
	}

	private void handleSocks5b(NioForwardClient client) {
		int version = client.inbound.get() & 0xff;
		int command = client.inbound.get() & 0xff;
		int reserved = client.inbound.get() & 0xff;
		int addressType = client.inbound.get() & 0xff;

		if (version != 5 || command != 1 || reserved != 0) {
			log("SOCKS", "COMMAND ERROR: " + command);
			client.closeNow();
			return;
		}

		byte[] addr = null;
		InetAddress iaddr = null;

		if (addressType == 1 || addressType == 4) {
			addr = new byte[addressType * 4];
			client.inbound.get(addr);
			try {
				iaddr = InetAddress.getByAddress(addr);
			} catch (UnknownHostException exc2) {
				log("SOCKS", "DNS ERROR 1: " + exc2.getMessage());
			}
		} else if (addressType == 3) {
			if (true) {
				addr = new byte[client.inbound.get() & 0xff];
				client.inbound.get(addr);
			} else {
				addr = new byte[255];
				int pos = 0, got = 0;
				while ((got = client.inbound.get()) != 0x00) {
					addr[pos++] = (byte) got;
				}
				addr = Arrays.copyOf(addr, pos);
			}
			iaddr = resolveHostname(Text.ascii(addr));
		} else {
			log("SOCKS", "ADDRESS TYPE ERROR: " + addressType);
			client.closeNow();
			return;
		}

		int portHi = client.inbound.get() & 0xFF;
		int portLo = client.inbound.get() & 0xFF;
		int port = (portHi << 8) | portLo;

		if (iaddr == null) {
			ByteBuffer packet = bufferPool.aquire();
			packet.clear();
			packet.put((byte) 0x05);
			packet.put((byte) 0x04); // unreachable
			packet.put((byte) 0x00); // reserved
			packet.put((byte) addressType); // addr-type
			packet.put((byte) 0x00); // EMPTY host
			packet.put((byte) 0x00); // NULL port hi
			packet.put((byte) 0x00); // NULL port lo
			packet.flip();
			client.writeLater(packet);
			bufferPool.release(packet);

			client.closeLater();
		}

		SelectionKey key;
		try {
			InetSocketAddress targetAddr = new InetSocketAddress(iaddr, port);
			key = super.nioConnect(client.key.selector(), targetAddr);
		} catch (IOException exc) {
			ByteBuffer packet = bufferPool.aquire();
			packet.clear();
			packet.put((byte) 0x05);
			packet.put((byte) 0x04); // unreachable
			packet.put((byte) 0x00); // reserved
			packet.put((byte) addressType); // addr-type
			packet.put((byte) 0x00); // EMPTY host
			packet.put((byte) 0x00); // NULL port hi
			packet.put((byte) 0x00); // NULL port lo
			packet.flip();
			client.writeLater(packet);
			bufferPool.release(packet);

			client.closeLater();
			return;
		}

		// OK
		{
			ByteBuffer packet = bufferPool.aquire();
			packet.clear();
			packet.put((byte) 0x05); // version
			packet.put((byte) 0x00); // ACCEPTED
			packet.put((byte) 0x00); // reserved
			packet.put((byte) addressType); // addr-type

			if (addressType == 3) {
				packet.put((byte) addr.length);
			}
			packet.put(addr); // host
			// if (addressType == 3) {
			// packet.put((byte) 0x00); // terminator
			// }
			packet.put((byte) (port >> 8)); // port hi
			packet.put((byte) (port >> 0)); // port lo
			packet.flip();

			client.writeLater(packet);
			bufferPool.release(packet);
		}

		couple(client, new NioForwardClient(this, key, false), port);
	}
}