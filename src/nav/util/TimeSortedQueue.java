package nav.util;

import java.util.Comparator;
import java.util.PriorityQueue;

public class TimeSortedQueue<T> {
	private final PriorityQueue<Slot<T>> queue;

	public TimeSortedQueue() {
		queue = new PriorityQueue<Slot<T>>(11, new TimeSlotComparator<T>());
	}

	public int size() {
		return queue.size();
	}

	public void clear() {
		queue.clear();
	}

	public void insert(long time, T item) {
		queue.add(new Slot<T>(time, item));
	}

	public T poll(long now) {
		Slot<T> peeked = queue.peek();
		if (peeked == null || peeked.time > now)
			return null;
		return queue.poll().item;
	}

	// --

	private static class TimeSlotComparator<T> implements Comparator<Slot<T>> {
		@Override
		public int compare(Slot<T> o1, Slot<T> o2) {
			int cmp = Long.signum(o1.time - o2.time);
			return (cmp == 0) ? -1 : cmp;
		}
	}

	private static class Slot<Q> {
		private final long time;
		private final Q item;

		public Slot(long time, Q item) {
			this.time = time;
			this.item = item;
		}
	}
}