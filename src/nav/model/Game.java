package nav.model;

import nav.util.Clock;
import nav.util.TimeSortedQueue;

public class Game {
	public static final TimeSortedQueue<Runnable> eventQueue = new TimeSortedQueue<>();

	public static void tick() {
		long now = Clock.millis();

		while (true) {
			Runnable task = eventQueue.poll(now);
			if(task == null)
				break;
			task.run();
		}
	}
}
