package nav.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nav.util.Vec2;

public class Station
{
	public final String id;
	public final Vec2 pos;

	public Station(String id, Vec2 pos)
	{
		if(id == null)
			throw new NullPointerException();
		if(pos == null)
			throw new NullPointerException();
		this.id = id;
		this.pos = pos;
	}

	private final List<Passenger> passengers = new ArrayList<>();

	public List<Passenger> passengers()
	{
		return Collections.unmodifiableList(new ArrayList<Passenger>(passengers));
	}

	void onEnter(Passenger passenger)
	{
		if(passengers.indexOf(passenger) != -1)
			throw new IllegalStateException();

		passengers.add(passenger);

		System.out.println("" + passenger + " enters " + this + "");
	}

	void onLeave(Passenger passenger)
	{
		if(passengers.indexOf(passenger) == -1)
			throw new IllegalStateException();

		if(!passengers.remove(passenger))
			throw new IllegalStateException();

		System.out.println("" + passenger + " leaves " + this + "");
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "#" + id;
	}
}
