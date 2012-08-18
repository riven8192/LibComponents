package craterstudio.encryption.ssl.nio;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import craterstudio.text.Text;

public abstract class LiveMapFile extends LiveFile {

	public LiveMapFile(File file, long interval) {
		super(file, interval);
	}

	@Override
	public final void onUpdate(byte[] data) {
		Map<String, String> map = new HashMap<>();
		for (String line : Text.splitOnLines(Text.utf8(data))) {
			if (!(line = Text.beforeIfAny(line, '#').trim()).isEmpty()) {
				String[] pair = Text.splitPair(line, '=');
				if (pair != null) {
					map.put(pair[0].trim(), pair[1].trim());
				}
			}
		}
		this.onMapUpdate(map);
	}

	public abstract void onMapUpdate(Map<String, String> map);

}
