package nav.model;

public class Passenger {
	private Bus inBus;

	public boolean shouldEnterBusAt(Bus bus, Station station) {
		if (inBus != null)
			throw new IllegalStateException();
		return true;
	}

	public boolean tryEnterBusAt(Bus bus, Station station) {
		if (!this.shouldEnterBusAt(bus, station))
			throw new IllegalStateException();

		if (bus.tryEnter(this)) {
			inBus = bus;
			return true;
		}
		return false;
	}

	public boolean shouldDepartBusAt(Bus bus, Station station) {
		if (inBus == null)
			throw new IllegalStateException();
		if (bus == null)
			throw new IllegalStateException();
		if (inBus != bus)
			throw new IllegalStateException();
		return true;
	}

	public void departBusAt(Bus bus, Station station) {
		if (!this.shouldDepartBusAt(bus, station))
			throw new IllegalStateException();

		bus.leave(this);
		inBus = null;
	}
}
