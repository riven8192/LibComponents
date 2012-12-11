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
			this.onUpdate(this.lastdata);
		}
	}

	public abstract void onUpdate(byte[] data);

	private boolean needsUpdate() {
		long now = Clock.now();

		if (now - this.lastcheck < this.interval) {
			return false;
		}

		if (!this.file.exists() || this.file.isDirectory()) {
			return false;
		}

		long lastmod = this.file.lastModified();
		if (lastmod == this.lastmod) {
			return false;
		}

		this.lastmod = lastmod;
		this.lastcheck = now;

		byte[] data = FileUtil.readFile(this.file);
		if (Arrays.equals(data, this.lastdata)) {
			return false;
		}

		this.lastdata = data;

		return true;
	}
}
