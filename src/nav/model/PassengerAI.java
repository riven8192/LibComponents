package nav.model;

import java.util.Arrays;

import nav.Scripts;
import nav.script.AbstractContext;
import nav.script.BasicScript.Block;
import nav.script.State;

public class PassengerAI extends AbstractContext
{
	private final Passenger passenger;

	public PassengerAI(Passenger passenger, Block script)
	{
		super(script);

		if(passenger == null)
			throw new NullPointerException();
		this.passenger = passenger;
	}

	@Override
	public boolean query(String var)
	{
		throw new IllegalStateException("query var: '" + var + "'");
	}

	@Override
	public State signal(String[] words)
	{
		System.out.println("PASSENGER SIGNAL " + Arrays.toString(words));
		return State.RUNNING;
	}
}
