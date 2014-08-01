package nav.model;

import nav.Scripts;

public class Passenger {
	public final String id;
	private Station inStation;
	private Bus inBus;
	private TravelPlan travelPlan;
	private final PassengerAI ai;

	public Passenger(String id, Station station) {
		if(id == null)
			throw new NullPointerException();
		if(station == null)
			throw new NullPointerException();
		this.id = id;
		inStation = station;
		inStation.onEnter(this);
		ai = new PassengerAI(this, Scripts.getPassengerScript());
	}

	public void start() {
		ai.start();
	}

	public void setTravelPlan(TravelPlan plan) {
		if(plan == null)
			throw new NullPointerException();
		if(travelPlan != null)
			throw new IllegalStateException();
		travelPlan = plan;
	}

	public boolean shouldEnterBusAt(Bus bus, Station station) {
		if(inStation == null)
			throw new IllegalStateException();
		if(inBus != null)
			throw new IllegalStateException();

		if(bus.isFull())
			return false;
		if(travelPlan == null)
			return false;
		return travelPlan.shouldEnterBusAt(bus, station);
	}

	public boolean tryEnterBusAt(Bus bus, Station station) {
		if(bus.isFull())
			return false;

		if(!this.shouldEnterBusAt(bus, station))
			throw new IllegalStateException();

		if(bus.onTryEnter(this)) {
			inBus = bus;
			inStation.onLeave(this);
			travelPlan.onEnterBus(bus, station);
			inStation = null;
			return true;
		}
		return false;
	}

	public boolean shouldDepartBusAt(Bus bus, Station station) {
		if(inStation != null)
			throw new IllegalStateException();
		if(inBus == null)
			throw new IllegalStateException();
		if(bus == null)
			throw new IllegalStateException();
		if(inBus != bus)
			throw new IllegalStateException();

		if(travelPlan == null)
			return false;
		return travelPlan.shouldDepartBusAt(bus, station);
	}

	public void departBusAt(Bus bus, Station station) {
		if(!this.shouldDepartBusAt(bus, station))
			throw new IllegalStateException();

		bus.onLeave(this);
		inBus = null;

		station.onEnter(this);
		inStation = station;

		travelPlan.onDepartBus(bus, station);
		if(travelPlan.hasFinished()) {
			this.onFinishedTravelPlan();
			travelPlan = null;
		}
	}

	public void onFinishedTravelPlan() {
		ai.call("onReachedDestination");
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "#" + id;
	}
}
