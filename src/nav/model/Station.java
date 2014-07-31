package nav.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nav.util.Vec2;

public class Station {
	public final String id;

	public Station(String id) {
		this.id = id;
	}

	public final Vec2 pos = new Vec2();

	@Override
	public String toString() {
		return "Station[id=" + id + "]";
	}

	private final List<Passenger> passengers = new ArrayList<>();

	public List<Passenger> passengers() {
		return Collections.unmodifiableList(new ArrayList<Passenger>(passengers));
	}

	public void enter(Passenger passenger) {
		if (passengers.indexOf(passenger) != -1)
			throw new IllegalStateException();

		passengers.add(passenger);
	}

	public void leave(Passenger passenger) {
		if (passengers.indexOf(passenger) == -1)
			throw new IllegalStateException();

		if (!passengers.remove(passenger))
			throw new IllegalStateException();
	}
}
