package craterstudio.encryption.ssl.nio;

import java.io.File;
import java.util.Arrays;

import craterstudio.io.FileUtil;
import craterstudio.time.Clock;

public abstract class LiveFile {
	private final File file;
	private final long interval;

	public LiveFile(File file, long interval) {
		if (!file.exists()) {
			throw new IllegalStateException("file not found: " + file.getAbsolutePath());
		}
		this.file = file;
		this.interval = interval;
	}

	private long lastcheck;
	private long lastmod;
	private byte[] lastdata;

	public synchronized void poll() {
		if (this.needsUpdate()) {
			this.onUpdate(lastdata);
		}
	}

	public abstract void onUpdate(byte[] data);

	private boolean needsUpdate() {
		long now = Clock.now();

		if (now - lastcheck < interval) {
			return false;
		}

		if (!file.exists() || file.isDirectory()) {
			return false;
		}

		long lastmod = file.lastModified();
		if (lastmod == this.lastmod) {
			return false;
		}

		byte[] data = FileUtil.readFile(file);
		if (Arrays.equals(data, lastdata)) {
			return false;
		}

		this.lastcheck = now;
		this.lastmod = lastmod;
		lastdata = data;

		return true;
	}
}
