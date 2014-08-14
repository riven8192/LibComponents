package nav.script.road;

import java.util.ArrayList;
import java.util.List;

import nav.script.road.RoadTile.Dir;

public class RoadJourney {
	public static void main(String[] args) {
		RoadTile t1 = new RoadTile("A", 0, 0);
		RoadTile t2 = new RoadTile("B", 1, 0);
		RoadTile t3 = new RoadTile("C", 2, 0);
		RoadTile t4 = new RoadTile("D", 2, -1);
		RoadTile t5 = new RoadTile("E", 3, -1);

		List<RoadTile> route = new ArrayList<>();
		route.add(t1);
		route.add(t2);
		route.add(t3);
		route.add(t4);
		route.add(t5);

		route.get(4).enter(14);

		RoadJourney journey = new RoadJourney(route);
		RoadJourneyVehicle stepper = new RoadJourneyVehicle(journey, 5);

		for(;;) {
			if(stepper.step() != RoadJourneyStepper.State.MOVING)
				break;
			System.out.println(journey.toString());
		}
	}

	final List<RoadTile> tiles;
	final List<int[]> tileBits;

	public RoadJourney(List<RoadTile> tiles) {
		this.tiles = tiles;

		List<Dir> dirs = new ArrayList<>();
		for(int i = 1; i < tiles.size(); i++) {
			RoadTile prev = tiles.get(i - 1);
			RoadTile curr = tiles.get(i - 0);

			int dx = curr.x - prev.x;
			int dy = curr.y - prev.y;
			if(dx == 0 && dy == 0)
				throw new IllegalStateException();
			if(dx != 0 && dy != 0)
				throw new IllegalStateException();
			if(Math.abs(dx) > 1 || Math.abs(dy) > 1)
				throw new IllegalStateException();

			Dir dir;
			if(dx == 0)
				dir = (dy > 0) ? Dir.NORTH : Dir.SOUTH;
			else
				dir = (dx > 0) ? Dir.EAST : Dir.WEST;

			if(i == 1 || i == tiles.size() - 1)
				dirs.add(dir);
			dirs.add(dir);
		}

		tileBits = new ArrayList<>();
		for(int i = 1; i < dirs.size(); i++) {
			Dir prev = dirs.get(i - 1);
			Dir curr = dirs.get(i - 0);
			tileBits.add(RoadTile.getBits(prev, curr));
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < tiles.size(); i++) {
			if(sb.length() != 0) {
				sb.append(' ');
			}
			for(int bit : tileBits.get(i)) {
				sb.append(tiles.get(i).isEmpty(bit) ? '_' : 'X');
			}
		}
		return sb.toString();
	}
}
