package craterstudio.encryption.ssl.nio;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import craterstudio.text.Text;
import craterstudio.text.TextValues;

public class NioForwardHTTP extends NioForwardTCP {

	public static void main(String[] args) throws Exception {

		params.addProps("host", "port", "backlog", "conf", "timeout", "workers");
		params.parse(args);

		LOG_PREFIX = "HTTP";
		CONF_FILE = new LiveFile(new File(params.get("conf")), 10_000L) {
			@Override
			public void onUpdate(byte[] data) {
				log("HTTP", "Reloading configuration file...");
				Properties props = new Properties();

				try {
					props.load(new ByteArrayInputStream(data));
				} catch (IOException exc) {
					exc.printStackTrace();
				}

				CONF_PROPS = new HashMap<>();
				for (Entry<Object, Object> entry : props.entrySet()) {
					CONF_PROPS.put((String) entry.getKey(), (String) entry.getValue());
				}
			}
		};

		NioForwardTCP.loop(NioForwardHTTP.class, //
		   params.get("host"), //
		   params.getInt("port"), //
		   params.getInt("backlog"), //
		   params.getInt("timeout") * 1000, //
		   params.getInt("workers") //
		   );
	}

	public NioForwardHTTP() throws IOException {
		super();
	}

	private static LiveFile CONF_FILE;
	private static Map<String, String> CONF_PROPS;

	@Override
	public void loopHook() throws IOException {
		CONF_FILE.poll();
	}

	@Override
	public final InetSocketAddress getTarget(String httpHeaderHostname) {

		String hostport = CONF_PROPS.get(httpHeaderHostname);
		if (hostport == null) {
			if (CONF_PROPS.containsKey(httpHeaderHostname)) {
				return null;
			}

			String hostname = httpHeaderHostname;
			do {
				hostname = Text.after(hostname, '.');
				hostport = CONF_PROPS.get("*." + hostname);
				log("HTTP", "Resolving " + httpHeaderHostname + " => " + hostname);
			} while (hostport == null && hostname != null && hostname.contains("."));

			if (hostport == null) {
				log("HTTP", "Resolving failed for " + httpHeaderHostname);
				CONF_PROPS.put(httpHeaderHostname, null);
				return null;
			}

			log("HTTP", "Resolved " + httpHeaderHostname + " => " + hostname);
			CONF_PROPS.put(httpHeaderHostname, hostport);
		}

		String host = Text.beforeIfAny(hostport, ':');
		String port = Text.after(hostport, ":", "80");

		return new InetSocketAddress(host, TextValues.tryParseInt(port, 80));
	}

	static byte[] HTTP_END_OF_HEADER = Text.ascii("\r\n\r\n");

	@Override
	public void handleReadState(NioForwardClient client) {

		switch (client.state) {
			case PUMP_STATE:
				if (client.other != null) {
					client.other.writeLater(client.inbound);
				}
				break;

			case INIT_STATE:

				int pos = client.inbound == null ? -1 : indexOf(client.inbound, HTTP_END_OF_HEADER);
				if (pos == -1) {
					break;
				}

				String clientAddr = client.channel.socket().getInetAddress().getHostAddress();

				ByteBuffer newHeader = bufferPool.aquire();
				try {
					newHeader.clear();

					String httpHeaderHost = null;
					client.inbound.get(TMP_ARRAY, 0, pos);
					for (String line : Text.split(Text.ascii(TMP_ARRAY, 0, pos), "\r\n")) {
						if (line.startsWith("Host: ")) {
							httpHeaderHost = Text.beforeIfAny(Text.after(line, ' '), ':');
						} else if (line.startsWith("X-Forwarded-For: ")) {
							continue;
						}

						newHeader.put(Text.ascii(line + "\r\n"));
					}

					newHeader.put(Text.ascii("X-Forwarded-For: " + clientAddr));
					newHeader.flip();

					if (httpHeaderHost == null) {
						byte[] content = Text.ascii("PROXY: Host not specified");
						sendHTTPResponse(client, 404, "NotFound", content);
						client.closeLater();
						break;
					}

					InetSocketAddress targetAddr = getTarget(httpHeaderHost);
					log("HTTP", "Forward for " + httpHeaderHost + ": " + clientAddr + " => " + targetAddr);
					if (targetAddr == null) {
						byte[] content = Text.ascii("PROXY: Hostname not found: " + httpHeaderHost);
						sendHTTPResponse(client, 404, "NotFound", content);
						client.closeLater();
						break;
					}

					SelectionKey key;
					try {
						key = super.nioConnect(client.key.selector(), targetAddr);
					} catch (IOException exc) {
						byte[] content = Text.ascii("PROXY: Host not found: " + targetAddr);
						sendHTTPResponse(client, 404, "NotFound", content);
						client.closeLater();
						break;
					}

					NioForwardClient target = new NioForwardClient(this, key, false);
					target.other = client;
					client.other = target;
					target.key.attach(target);

					target.writeLater(newHeader);
				} finally {
					bufferPool.release(newHeader);
				}

				client.state = PUMP_STATE;
				break;

			default:
				throw new IllegalStateException("state=" + client.state);
		}
	}

	@Override
	public void onConnectFailed(SelectionKey key, IOException exc) {
		NioForwardClient client = (NioForwardClient) key.attachment();
		if (client != null && client.other != null) {
			byte[] content = Text.ascii("PROXY: " + exc.getMessage() + ": " + client.channel.socket().getInetAddress());
			sendHTTPResponse(client.other, 404, "NotFound", content);
			client.closeLater();
		} else {
			super.onConnectFailed(key, exc);
		}
	}

	private void sendHTTPResponse(NioForwardClient client, int statuscode, String statusmsg, byte[] content) {

		ByteBuffer bb = client.tcp.bufferPool.aquire();
		{
			bb.clear();
			bb.put(Text.ascii("HTTP/1.1 " + statuscode + " " + statusmsg + "\r\n"));
			bb.put(Text.ascii("Content-Type: text/plain\r\n"));
			bb.put(Text.ascii("Content-Length: " + content.length + "\r\n"));
			bb.put(Text.ascii("Connection: close\r\n"));
			bb.put(Text.ascii("\r\n"));
			bb.put(content);
			bb.flip();
			client.writeLater(bb);
		}
		client.tcp.bufferPool.release(bb);
	}
}