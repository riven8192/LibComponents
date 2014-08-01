package nav.model;

import nav.script.AbstractContext;
import nav.script.BasicScript.Block;
import nav.script.State;

public class PassengerAI extends AbstractContext {
	private final Passenger passenger;

	public PassengerAI(Passenger passenger, Block script) {
		super(script);

		if(passenger == null)
			throw new NullPointerException();
		this.passenger = passenger;
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
