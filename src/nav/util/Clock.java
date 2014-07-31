package nav.util;

public class Clock {
	public static long millis() {
		return System.nanoTime() / 1000_000L;
	}
}
