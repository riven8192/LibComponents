package nav.model;

import nav.script.BasicScript.Block;
import nav.script.Context;
import nav.script.Eval;
import nav.script.State;
import nav.util.Clock;
import nav.util.TimeSortedQueue;

public class BusAI implements Context, Runnable {
	private final Bus bus;
	private Eval eval;

	public BusAI(Bus bus, Block script) {
		if (bus == null)
			throw new NullPointerException();
		if (script == null)
			throw new NullPointerException();
		this.bus = bus;
		this.eval = script.eval(this);
	}

	public void run() {
		if (eval == null) {
			return; // no reschedule
		}

		while (true)
			switch (eval.tick()) {
				case HALTED:
				case TERMINATED:
					eval = null;
					return;

				case SLEEPING:
					Game.eventQueue.insert(Clock.millis() + nextSleep, this);
					return;

				case RAISED:
					throw new IllegalStateException();

				case YIELDED:
					return; // no reschedule

				case RUNNING:
					break; // normal
			}
	}

	@Override
	public boolean query(String var) {
		if (var.equals("has_time_before_departure"))
			return true;
		if (var.equals("has_outbound_passenger"))
			return !bus.isEmpty() && bus.hasOutboundPassenger();
		if (var.equals("has_inbound_passenger"))
			return true;
		return false;
	}

	@Override
	public State signal(String[] words) {
		if (words[0].equals("onArrive()"))
			bus.onArrive(dst);
		else if (words[0].equals("depart()"))
			bus.depart();
		return State.RUNNING;
	}

	private long nextSleep;

	@Override
	public void nextSleep(String time) {
		if (time.endsWith("ms"))
			nextSleep = Long.parseLong(time.substring(0, time.length() - 2));
		else if (time.endsWith("s"))
			nextSleep = Long.parseLong(time.substring(0, time.length() - 1)) * 1000;
		else
			throw new IllegalStateException();
	}

}
