package nav.script;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import nav.script.BasicScript.Block;

public class ScriptTest {
	public static void main(String[] args) {
		Block block = BasicScript.compile("BEGIN \n HELLO WORLD \n IF NOT a HALT \n BOO \n END");

		Context ctx = new Context() {

			@Override
			public State signal(String[] words) {
				System.out.println(Arrays.asList(words));
				return State.RUNNING;
			}

			@Override
			public boolean query(String var) {
				return false;
			}

			@Override
			public void nextSleep(String time) {

			}
		};

		Eval eval = block.eval(ctx);

		while (true) {
			switch (eval.tick()) {
				case HALTED:
					return;
				case TERMINATED:
					return;
				case RAISED:
					return;
				case YIELDED:
					break;
				case SLEEPING:
					break;
				case RUNNING:
					break;
			}
		}
	}

}
