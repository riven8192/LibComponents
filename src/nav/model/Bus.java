package nav.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nav.Scripts;
import nav.util.Clock;
import nav.util.Vec2;

public class Bus
{
	public final Route route;
	private final BusAI ai;
	public final String id;

	public Bus(String id, Route route)
	{
		if(id == null)
			throw new NullPointerException();
		if(route == null)
			throw new NullPointerException();

		this.id = id;
		this.route = route;
		this.route.buses.add(this);
		this.ai = new BusAI(this, Scripts.getBusScript());
	}

	public void start()
	{
		at = route.stations.get(0);
		to = route.stations.get(1);
		ai.start();
	}

	public void depart()
	{
		departedAt = Clock.millis();

		if(capacity <= 0)
			throw new IllegalStateException();
		if(velocity <= 0.0f)
			throw new IllegalStateException();

		float sec = Vec2.distance(at.pos, to.pos) / velocity;
		long travelTime = (long) (1000L * sec);

		System.out.println("" + this + " departs at " + at + " with " + passengers.size() + " passengers, destination " + to + ", expected time: " + (int) sec + "s");

		Game.eventQueue.insert(Clock.millis() + travelTime, new Runnable()
		{
			@Override
			public void run()
			{
				ai.resume();
			}
		});
	}

	public void onArrive()
	{
		System.out.println("" + this + " arrives at " + to + " with " + passengers.size() + " passengers");

		doDepartAt = Clock.millis() + 5000;
		departedAt = -1L;

		at = to;
		to = route.nextFor(at);
	}

	public Station at, to;
	public float velocity; // m/s
	public long doDepartAt = -1L;
	public long departedAt = -1L;

	public int capacity;
	private final List<Passenger> passengers = new ArrayList<>();

	public boolean shouldDepart()
	{
		if(this.isFull())
			return true; // depart earlier
		if(this.hasInboundPassenger())
			return this.hasDepartureDeadlineExpired(); // depart later
		return this.hasDepartureTimeExpired();
	}

	public boolean hasDepartureTimeExpired()
	{
		return Clock.millis() >= doDepartAt;
	}

	public boolean hasDepartureDeadlineExpired()
	{
		return Clock.millis() >= doDepartAt + 10_000L;
	}

	public boolean hasOutboundPassenger()
	{
		if(this.isEmpty())
			return false;
		for(Passenger passenger : this.passengers())
			if(passenger.shouldDepartBusAt(this, at))
				return true;
		return false;
	}

	public void outboundPassengerLeavesBus()
	{
		for(Passenger passenger : this.passengers())
		{
			if(passenger.shouldDepartBusAt(this, at))
			{
				passenger.departBusAt(this, at);
				return;
			}
		}
		throw new IllegalStateException();
	}

	public boolean hasInboundPassenger()
	{
		if(this.isFull())
			return false;
		for(Passenger passenger : at.passengers())
			if(passenger.shouldEnterBusAt(this, at))
				return true;
		return false;
	}

	public void inboundPassengerEntersBus()
	{
		for(Passenger passenger : at.passengers())
		{
			if(passenger.shouldEnterBusAt(this, at))
			{
				if(!passenger.tryEnterBusAt(this, at))
					throw new IllegalStateException();
				return;
			}
		}
		throw new IllegalStateException();
	}

	public List<Passenger> passengers()
	{
		return Collections.unmodifiableList(new ArrayList<Passenger>(passengers));
	}

	public boolean onTryEnter(Passenger passenger)
	{
		if(passengers.indexOf(passenger) != -1)
			throw new IllegalStateException();
		if(passengers.size() > capacity)
			throw new IllegalStateException();

		if(this.isFull())
		{
			System.out.println("" + passenger + " fails to enter " + this + "");
			return false;
		}

		passengers.add(passenger);
		System.out.println("" + passenger + " enters " + this + "");
		return true;
	}

	public void onLeave(Passenger passenger)
	{
		if(passengers.indexOf(passenger) == -1)
			throw new IllegalStateException();

		if(!passengers.remove(passenger))
			throw new IllegalStateException();

		System.out.println("" + passenger + " leaves " + this + "");
	}

	public boolean isEmpty()
	{
		return passengers.size() == 0;
	}

	public boolean isFull()
	{
		return passengers.size() == capacity;
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "#" + id;
	}
}
