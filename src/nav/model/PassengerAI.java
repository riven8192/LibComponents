package nav.model;

import nav.util.Clock;
import net.indiespot.script.crude.AbstractContext;
import net.indiespot.script.crude.CrudeScript.Block;
import net.indiespot.script.crude.State;

public class PassengerAI extends AbstractContext {
	private final Passenger passenger;

	public PassengerAI(Passenger passenger, Block script) {
		super(script);

		if(passenger == null)
			throw new NullPointerException();
		this.passenger = passenger;
	}

	@Override
	public void schedule(Runnable task, long delay) {
		Game.eventQueue.insert(Clock.millis() + delay, task);
	}

	@Override
	public boolean query(String var) {
		throw new IllegalStateException("query var: '" + var + "'");
	}

	@Override
	public State signal(String text) {
		System.out.println("PASSENGER SIGNAL " + text);
		return State.RUNNING;
	}
}
