package nav.model;

import java.util.Arrays;

import nav.script.AbstractContext;
import nav.script.BasicScript.Block;
import nav.script.State;

public class BusAI extends AbstractContext
{
	private final Bus bus;

	public BusAI(Bus bus, Block script)
	{
		super(script);

		if(bus == null)
			throw new NullPointerException();
		this.bus = bus;

		eval.define("DOORS_OPEN_DURATION", Long.valueOf(3_000));
		eval.define("DOORS_CLOSE_DURATION", Long.valueOf(3_000));
		eval.define("PASSENGER_ENTER_DURATION", Long.valueOf(1_000));
		eval.define("PASSENGER_LEAVE_DURATION", Long.valueOf(1_000));
	}

	@Override
	public boolean query(String var)
	{
		if(var.equals("bus.shouldDepart()"))
			return bus.shouldDepart();
		if(var.equals("bus.hasOutboundPassenger()"))
			return bus.hasOutboundPassenger();
		if(var.equals("bus.hasInboundPassenger()"))
			return bus.hasInboundPassenger();
		throw new IllegalStateException("query var: '" + var + "'");
	}

	@Override
	public State signal(String[] words)
	{
		System.out.println("BUS SIGNAL " + Arrays.toString(words));
		if(words[0].equals("bus.onArrive()"))
			bus.onArrive();
		else if(words[0].equals("bus.depart()"))
			bus.depart();
		else if(words[0].equals("bus.outboundPassengerLeavesBus()"))
			bus.outboundPassengerLeavesBus();
		else if(words[0].equals("bus.inboundPassengerEntersBus()"))
			bus.inboundPassengerEntersBus();
		else
			throw new IllegalStateException("signal: " + Arrays.toString(words) + "");
		return State.RUNNING;
	}
}
