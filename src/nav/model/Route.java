package nav.model;

import java.util.ArrayList;
import java.util.List;

import nav.util.Vec2;

public class Route
{
	public final List<Station> stations = new ArrayList<>();
	public List<Bus> buses = new ArrayList<>();

	public Station nextFor(Station station)
	{
		int currIndex = stations.indexOf(station);
		if(currIndex == -1)
			throw new IllegalStateException();

		int nextIndex = currIndex + 1;
		if(nextIndex >= stations.size())
			nextIndex = 0;

		return stations.get(nextIndex);
	}

	public boolean visits(Station station)
	{
		return stations.indexOf(station) != -1;
	}

	public float distance(Station src, Station dst)
	{
		if(src == dst)
			throw new IllegalStateException();
		if(!this.visits(src))
			throw new IllegalStateException();
		if(!this.visits(dst))
			throw new IllegalStateException();

		float distance = 0.0f;

		Station at = src;
		do
		{
			Station to = this.nextFor(at);
			distance += Vec2.distance(at.pos, to.pos);
			at = to;
		}
		while (at != dst);

		return distance;
	}
}
