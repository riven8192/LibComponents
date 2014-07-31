package nav;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import nav.script.BasicScript;
import nav.script.BasicScript.Block;
import nav.script.ScriptTest;

public class Scripts {
	private static Block BUS_SCRIPT;
	static {
		try {
			BUS_SCRIPT = BasicScript.compile(readResource("nav/model/Bus.ai"));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Block getBusScript() {
		return BUS_SCRIPT;
	}

	private static String readResource(String res) throws IOException {
		InputStream in = ScriptTest.class.getResourceAsStream(res);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[4 * 1024];
		while (true) {
			int got = in.read(buf);
			if (got == -1)
				break;
			baos.write(buf, 0, got);
		}
		byte[] raw = baos.toByteArray();
		return new String(raw, "ASCII");
	}
}
