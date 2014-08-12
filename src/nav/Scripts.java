package nav;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import net.indiespot.script.crude.CrudeScript;
import net.indiespot.script.crude.CrudeScript.Block;


public class Scripts {
	private static Block BUS_SCRIPT;
	private static Block PASSENGER_SCRIPT;
	static {
		try {
			BUS_SCRIPT = CrudeScript.compile("Bus.ai", readResource("/nav/model/Bus.ai"));
			PASSENGER_SCRIPT = CrudeScript.compile("Passenger.ai", readResource("/nav/model/Passenger.ai"));
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Block getBusScript() {
		return BUS_SCRIPT;
	}

	public static Block getPassengerScript() {
		return PASSENGER_SCRIPT;
	}

	private static String readResource(String res) throws IOException {
		InputStream in = Scripts.class.getResourceAsStream(res);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[4 * 1024];
		while (true) {
			int got = in.read(buf);
			if(got == -1)
				break;
			baos.write(buf, 0, got);
		}
		byte[] raw = baos.toByteArray();
		return new String(raw, "ASCII");
	}
}
