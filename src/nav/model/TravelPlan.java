package nav.model;

import java.util.ArrayList;
import java.util.List;

public class TravelPlan {
	public static class Travel {
		public final Route route;
		public final Station enterBusAt;
		public final Station departBusAt;

		public Travel(Route route, Station enterBusAt, Station departBusAt) {
			if(route == null)
				throw new NullPointerException();
			if(enterBusAt == null)
				throw new NullPointerException();
			if(departBusAt == null)
				throw new NullPointerException();

			if(!route.visits(enterBusAt))
				throw new IllegalStateException();
			if(!route.visits(departBusAt))
				throw new IllegalStateException();

			this.route = route;
			this.enterBusAt = enterBusAt;
			this.departBusAt = departBusAt;
		}
	}

	private final List<Travel> travels = new ArrayList<>();
	private int travelIndex;
	private boolean inTravel;

	public void addNextStation(Route route, Station departBusAt) {
		if(travels.isEmpty())
			throw new IllegalStateException();
		Travel last = travels.get(travels.size() - 1);
		this.addTravel(new Travel(route, last.departBusAt, departBusAt));
	}

	public void addTravel(Travel travel) {
		if(this.hasFinished())
			throw new IllegalStateException();

		if(!travels.isEmpty()) {
			Travel last = travels.get(travels.size() - 1);
			if(travel.enterBusAt != last.departBusAt)
				throw new IllegalStateException();
		}
		travels.add(travel);
	}

	public boolean hasFinished() {
		return travelIndex == -1;
	}

	public boolean shouldEnterBusAt(Bus bus, Station station) {
		if(inTravel)
			throw new IllegalStateException();
		if(this.hasFinished())
			throw new IllegalStateException();

		Travel travel = travels.get(travelIndex);
		if(bus.route == travel.route)
			if(station == travel.enterBusAt)
				return true;
		return false;
	}

	public void onEnterBus(Bus bus, Station station) {
		if(!this.shouldEnterBusAt(bus, station))
			throw new IllegalStateException();

		inTravel = true;
	}

	public boolean shouldDepartBusAt(Bus bus, Station station) {
		if(!inTravel)
			throw new IllegalStateException();
		if(this.hasFinished())
			throw new IllegalStateException();

		Travel travel = travels.get(travelIndex);
		if(bus.route == travel.route)
			if(station == travel.departBusAt)
				return true;
		return false;
	}

	public void onDepartBus(Bus bus, Station station) {
		if(!this.shouldDepartBusAt(bus, station))
			throw new IllegalStateException();

		inTravel = false;
		travelIndex += 1;
		if(travelIndex == travels.size())
			travelIndex = -1; // finished
	}
}
