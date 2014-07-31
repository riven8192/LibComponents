package nav.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nav.Scripts;
import nav.util.Clock;
import nav.util.Vec2;

public class Bus {
	public final Route route;
	private final BusAI ai;

	public Bus(Route route) {
		this.route = route;
		this.route.buses.add(this);
		this.at = route.stations.get(0);
		this.to = route.stations.get(1);
		this.ai = new BusAI(this, Scripts.getBusScript());
	}

	public void depart() {
		System.out.println("depart=" + at);
		departedAt = Clock.millis();

		if (capacity <= 0)
			throw new IllegalStateException();
		if (velocity <= 0.0f)
			throw new IllegalStateException();

		float sec = Vec2.distance(at.pos, to.pos) / velocity;
		long travelTime = (long) (1000L * sec);
		System.out.println("travelTime=" + sec);

		final Station dst = to;
		Game.eventQueue.insert(Clock.millis() + travelTime, new Runnable() {
			@Override
			public void run() {
				Bus.this.onArrive(dst);
			}
		});
	}

	public void onArrive(Station dst) {
		System.out.println("onArrive=" + dst);
		departedAt = -1L;

		int dstIndex = route.stations.indexOf(dst);
		if (dstIndex == -1)
			throw new IllegalStateException();
		int nxtIndex = dstIndex + 1;
		if (nxtIndex >= route.stations.size())
			nxtIndex = 0;

		this.at = dst;
		this.to = route.stations.get(nxtIndex);

		this.awaitPassengers();
	}

	private int letPassengersEnter(int max) {
		if (this.isFull())
			return 0;

		int got = 0;
		for (Passenger passenger : at.passengers()) {
			int pre = passengers.size();
			passenger.onArrivedAwaitedBus(this);
			int post = passengers.size();
			if (pre != post)
				got++;

			if (this.isFull() || max == got)
				break;
		}
		return got;
	}

	private int letPassengersLeave(int max) {
		if (this.isEmpty())
			return 0;

		int got = 0;
		for (Passenger passenger : this.passengers()) {
			int pre = passengers.size();
			passenger.onArrivedAtStationInBus(this, at);
			int post = passengers.size();
			if (pre != post)
				got++;

			if (this.isEmpty() || max == got)
				break;
		}
		return got;
	}

	private void letPassengersLeaveThenEnter() {
		Game.eventQueue.insert(Clock.millis() + passenger_leave_time, new Runnable() {
			@Override
			public void run() {
				if (Bus.this.letPassengersLeave(1) > 0) {
					letPassengersLeaveThenEnter();
				} else {
					letPassengersEnter();
				}
			}
		});
	}

	private void letPassengersEnter() {
		Game.eventQueue.insert(Clock.millis() + passenger_enter_time, new Runnable() {
			@Override
			public void run() {
				if (Bus.this.letPassengersEnter(1) > 0) {
					letPassengersEnter();
				} else {
					Game.eventQueue.insert(Clock.millis() + door_close_time, new Runnable() {
						@Override
						public void run() {
							Bus.this.depart();
						}
					});
				}
			}
		});
	}

	public void awaitPassengers() {
		Game.eventQueue.insert(Clock.millis() + door_open_time, new Runnable() {
			@Override
			public void run() {
				Bus.this.letPassengersLeaveThenEnter();
			}
		});
	}

	private static final int door_open_time = 100;
	private static final int door_close_time = 100;
	private static final int passenger_enter_time = 25;
	private static final int passenger_leave_time = 25;

	public Station at, to;
	public float velocity; // m/s
	public long departedAt = -1;

	public int capacity;
	private final List<Passenger> passengers = new ArrayList<>();

	public boolean hasOutboundPassenger() {
		for (Passenger passenger : passengers)
			if (passenger.shouldDepartBusAt(this, at))
				return true;
		return false;
	}

	public void outboundPassengerLeavesBus() {
		for (Passenger passenger : passengers) {
			if (passenger.shouldDepartBusAt(this, at)) {
				this.leave(passenger);
				return;
			}
		}
		throw new IllegalStateException();
	}

	public List<Passenger> passengers() {
		return Collections.unmodifiableList(new ArrayList<Passenger>(passengers));
	}

	public boolean tryEnter(Passenger passenger) {
		if (passengers.indexOf(passenger) != -1)
			throw new IllegalStateException();
		if (passengers.size() > capacity)
			throw new IllegalStateException();

		if (passengers.size() == capacity)
			return false;

		passengers.add(passenger);
		return true;
	}

	public void leave(Passenger passenger) {
		if (passengers.indexOf(passenger) == -1)
			throw new IllegalStateException();

		if (!passengers.remove(passenger))
			throw new IllegalStateException();
	}

	public boolean isEmpty() {
		return passengers.size() == 0;
	}

	public boolean isFull() {
		return passengers.size() == capacity;
	}
}
