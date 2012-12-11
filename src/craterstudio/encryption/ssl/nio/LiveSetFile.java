package craterstudio.encryption.ssl.nio;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import craterstudio.text.Text;

public abstract class LiveSetFile extends LiveFile {

	public LiveSetFile(File file, long interval) {
		super(file, interval);
	}

	@Override
	public final void onUpdate(byte[] data) {
		Set<String> set = new HashSet<>();
		for (String line : Text.splitOnLines(Text.utf8(data))) {
			if (!(line = Text.beforeIfAny(line, '#').trim()).isEmpty()) {
				set.add(line);
			}
		}
		this.onSetUpdate(set);
	}

	public abstract void onSetUpdate(Set<String> set);

}
