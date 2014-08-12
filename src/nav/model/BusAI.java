package nav.model;

import nav.util.Clock;
import net.indiespot.script.crude.AbstractContext;
import net.indiespot.script.crude.CrudeScript.Block;
import net.indiespot.script.crude.State;

public class BusAI extends AbstractContext {
	private final Bus bus;

	public BusAI(Bus bus, Block script) {
		super(script);

		if(bus == null)
			throw new NullPointerException();
		this.bus = bus;

		eval.define("DOORS_OPEN_DURATION", Long.valueOf(1_000));
		eval.define("DOORS_CLOSE_DURATION", Long.valueOf(1_000));
		eval.define("PASSENGER_ENTER_DURATION", Long.valueOf(1_000));
		eval.define("PASSENGER_LEAVE_DURATION", Long.valueOf(1_000));
	}

	@Override
	public void schedule(Runnable task, long delay) {
		Game.eventQueue.insert(Clock.millis() + delay, task);
	}

	@Override
	public boolean query(String var) {
		if(var.equals("bus.shouldDepart()"))
			return bus.shouldDepart();
		if(var.equals("bus.hasOutboundPassenger()"))
			return bus.hasOutboundPassenger();
		if(var.equals("bus.hasInboundPassenger()"))
			return bus.hasInboundPassenger();
		throw new IllegalStateException("query var: '" + var + "'");
	}

	@Override
	public State signal(String text) {
		System.out.println("BUS SIGNAL " + text);
		if(text.equals("bus.onArrive()"))
			bus.onArrive();
		else if(text.equals("bus.depart()"))
			bus.depart();
		else if(text.equals("bus.outboundPassengerLeavesBus()"))
			bus.outboundPassengerLeavesBus();
		else if(text.equals("bus.inboundPassengerEntersBus()"))
			bus.inboundPassengerEntersBus();
		else
			throw new IllegalStateException("signal: " + text + "");
		return State.RUNNING;
	}
}
