package craterstudio.encryption.ssl.nio;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import craterstudio.io.Streams;

class NioForwardClient {

	public ByteBuffer inbound, outbound1, outbound2;

	public final NioForwardTCP tcp;
	public final SelectionKey key;
	public final SocketChannel channel;
	public int state = NioForwardTCP.PUMP_STATE;
	public NioForwardClient other;
	public long lastIO, closingIO;
	public final boolean fromAccept;

	public NioForwardClient(NioForwardTCP tcp, SelectionKey key, boolean fromAccept) {
		this.tcp = tcp;
		this.key = key;
		this.channel = (SocketChannel) key.channel();
		this.fromAccept = fromAccept;
	}

	public boolean isClosing() {
		return closingIO > 0;
	}

	public void writeLater(ByteBuffer data) {
		if (closingIO != 0 || !data.hasRemaining()) {
			return;
		}

		this.verifyOutboundState();

		if (outbound2 != null) {
			if (!putAfter(outbound2, data)) {
				if (putAfter(outbound1, outbound2)) {
					if (!putAfter(outbound2, data)) {
						throw new IllegalStateException();
					}
				} else {
					putMergeOutbound(data);
					this.tidyOutbound();
				}
			}
		} else {
			if (outbound1 == null) {
				outbound1 = newPut(data);
			} else if (!putAfter(outbound1, data)) {
				outbound2 = newPut(data);
			}
		}

		this.verifyOutboundState();
	}

	public int onRead() {
		if (inbound != null && getUnusedRoom(inbound) == 0) {
			NioForwardTCP.log("NIO", "WARNING: inbound buffer full");
			return 0;
		}

		if (inbound == null) {
			inbound = tcp.bufferPool.aquire();
		}

		inbound.compact();
		if (inbound.remaining() < 1600) {
			NioForwardTCP.log("NIO", "WARNING: inbound buffer nearly full (" + inbound.remaining() + " bytes remaining)");
		}

		int got;
		try {
			got = channel.read(inbound);
		} catch (IOException exc) {
			got = -1;
		}
		inbound.flip();

		if (got > 0) {
			lastIO = tcp.tickTime;
		} else if (got == 0) {
			NioForwardTCP.log("NIO", "WARNING: read 0 bytes from socket, closing connection");
			got = -1;
		}

		this.tidyInbound();

		return got;
	}

	public int onWrite() {

		if (outbound1 == null) {
			return 0;
		}

		this.verifyOutboundState();

		int sent;

		try {
			if (outbound2 != null) {
				tcp.DUO[0] = outbound1;
				tcp.DUO[1] = outbound2;
				sent = (int) channel.write(tcp.DUO);
			} else {
				sent = channel.write(outbound1);
			}
		} catch (IOException exc) {
			sent = -1;
		}

		if (sent > 0) {
			lastIO = tcp.tickTime;
		} else if (sent == 0) {
			NioForwardTCP.log("NIO", "WARNING: wrote 0 bytes");
		}

		this.tidyOutbound();

		return sent;
	}

	public boolean hasDataPending() {
		return (this.inbound != null) || (this.outbound1 != null) || (this.outbound2 != null);
	}

	public void closeLater() {
		this.closingIO = tcp.tickTime;

		if (!this.hasDataPending()) {
			this.closeNow();
		}
	}

	public static void close(SelectionKey key) {
		Streams.safeClose(key.channel());
		key.cancel();
	}

	public void closeNow() {
		this.closingIO = tcp.tickTime;

		if (this.other != null) {
			this.other.other = null; // remove reference to self
			this.other.closeLater();
		}

		close(key);

		if (inbound != null) {
			inbound = tcp.bufferPool.release(inbound);
		}
		if (outbound1 != null) {
			outbound1 = tcp.bufferPool.release(outbound1);
		}
		if (outbound2 != null) {
			outbound2 = tcp.bufferPool.release(outbound2);
		}
	}

	public void setReadOp(boolean enabled) {
		this.setOp(SelectionKey.OP_READ, enabled);
	}

	public void setWriteOp(boolean enabled) {
		this.setOp(SelectionKey.OP_WRITE, enabled);
	}

	private int cachedInterestOps = -1;

	public void setOp(int interest, boolean enabled) {
		int ops = (cachedInterestOps == -1) ? (cachedInterestOps = key.interestOps()) : cachedInterestOps;

		try {
			if (enabled) {
				if ((ops & interest) == 0) {
					key.interestOps(cachedInterestOps = (ops | interest));
				}
			} else {
				if ((ops & interest) != 0) {
					key.interestOps(cachedInterestOps = (ops & ~interest));
				}
			}
		} catch (CancelledKeyException exc) {
			NioForwardTCP.log("NIO", "ERROR: " + exc.getMessage());
		}
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NioForwardClient) {
			return this.key == ((NioForwardClient) obj).key;
		}
		return false;
	}

	public String getAddress() {
		return channel.socket().getInetAddress().getHostAddress();
	}

	@Override
	public String toString() {
		return "Client[" + this.getAddress() + " @ " + channel.socket().getPort() + "]";
	}

	public String toDetailedString() {
		return "[" + this.toString() + ", recv=" + toStringBuffer(inbound) + ", send=" + toStringBuffer(outbound1) + ", over=" + toStringBuffer(outbound2) + ", ops=" + toStringOps(this.key) + "]";
	}

	// ----

	public void verifyOutboundState() {
		if (outbound1 == null && outbound2 != null) {
			throw new IllegalStateException("outbound logic b0rked");
		}

		if (outbound1 != null && !outbound1.hasRemaining()) {
			throw new IllegalStateException("outbound logic b0rked");
		}

		if (outbound2 != null && !outbound2.hasRemaining()) {
			throw new IllegalStateException("outbound logic b0rked");
		}
	}

	public void verifyInboundState() {
		if (inbound != null && !inbound.hasRemaining()) {
			throw new IllegalStateException("inbound logic b0rked");
		}
	}

	public void tidyInbound() {
		if (inbound != null && !inbound.hasRemaining()) {
			inbound = tcp.bufferPool.release(inbound);
		}

		this.verifyInboundState();
	}

	public void tidyOutbound() {
		if (outbound1 != null && !outbound1.hasRemaining()) {
			outbound1 = tcp.bufferPool.release(outbound1);
		}

		if (outbound2 != null && !outbound2.hasRemaining()) {
			outbound2 = tcp.bufferPool.release(outbound2);
		}

		if (outbound1 == null && outbound2 != null) {
			outbound1 = outbound2;
			outbound2 = null;
		}

		this.verifyOutboundState();
	}

	// ----

	private static String toStringBuffer(ByteBuffer bb) {
		if (bb == null)
			return null;
		return "[" + bb.position() + "," + bb.limit() + "," + bb.capacity() + "]";
	}

	private static String toStringOps(SelectionKey key) {
		return toStringOps(key, key.readyOps()) + "/" + toStringOps(key, key.interestOps());
	}

	static String toStringOps(SelectionKey key, int ops) {
		if (!key.isValid())
			return "INVALID";
		StringBuilder sb = new StringBuilder("[");
		if ((ops & SelectionKey.OP_ACCEPT) != 0)
			sb.append("ACCEPT");
		if ((ops & SelectionKey.OP_CONNECT) != 0)
			sb.append("CONNECT");
		if ((ops & SelectionKey.OP_READ) != 0)
			sb.append("READ");
		if ((ops & SelectionKey.OP_WRITE) != 0)
			sb.append("WRITE");
		return sb.append(']').toString();
	}

	private static int getRoomLeft(ByteBuffer bb) {
		return (bb.capacity() - bb.limit());
	}

	private static int getUnusedRoom(ByteBuffer bb) {
		return bb.position() + (bb.capacity() - bb.limit());
	}

	private ByteBuffer newPut(ByteBuffer data) {
		ByteBuffer bb = tcp.bufferPool.aquire();
		bb.clear();
		bb.put(data).flip();
		return bb;
	}

	private void putMergeOutbound(ByteBuffer data) {
		if (outbound1 == null || outbound2 == null) {
			throw new IllegalStateException("no need to merge if we should make new buffers");
		}
		if (getUnusedRoom(outbound2) >= data.remaining()) {
			throw new IllegalStateException("no need to merge if we can fit it");
		}
		if (getUnusedRoom(outbound1) + getUnusedRoom(outbound2) < data.remaining()) {
			throw new BufferOverflowException();
		}

		int room = getUnusedRoom(outbound1);
		if (room == 0) {
			throw new IllegalStateException("nothing to move between buffer 1 and 2");
		}

		ByteBuffer copy = outbound2.slice();
		int move = Math.min(room, copy.limit());
		copy.limit(move);
		if (!putAfter(outbound1, copy)) {
			throw new IllegalStateException("copy should fit in buffer 1");
		}

		outbound2.position(outbound2.position() + move);
		if (!putAfter(outbound2, data)) {
			throw new IllegalStateException("data should fit in buffer 2");
		}
	}

	private static boolean putAfter(ByteBuffer target, ByteBuffer data) {
		if (!target.hasRemaining()) {
			// reset and fill buffer
			target.clear();
			target.put(data).flip();
			return true;
		}

		if (getRoomLeft(target) >= data.remaining()) {
			// put data in 'remaining' range
			int pos = target.position();
			target.position(target.limit());
			target.limit(target.capacity());

			target.put(data);

			target.limit(target.position());
			target.position(pos);
			return true;
		}

		if (getUnusedRoom(target) >= data.remaining()) {
			// compact and fill buffer
			target.compact().put(data).flip();
			return true;
		}

		return false;
	}

}