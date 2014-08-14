package nav.script.road;

public class RoadJourneyStepper {
	public static enum State {
		MOVING, BLOCKED, ARRIVED;
	}

	public final RoadJourney journey;

	private int tileIndex;
	private int tileCell;
	private final boolean doEnter;
	private int counter;

	public RoadJourneyStepper(RoadJourney journey, boolean doEnter) {
		this.journey = journey;
		this.doEnter = doEnter;
	}

	public int counter() {
		return counter;
	}

	public State step() {
		int[] bits = journey.tileBits.get(tileIndex);
		if(tileCell == bits.length) {
			if(++tileIndex == journey.tiles.size())
				return State.ARRIVED;
			bits = journey.tileBits.get(tileIndex);
			tileCell = 0;
		}
		RoadTile tile = journey.tiles.get(tileIndex);
		int bit = bits[tileCell++];
		if(doEnter) {
			if(!tile.isEmpty(bit))
				return State.BLOCKED;
			tile.enter(bit);
		}
		else {
			tile.leave(bit);
		}
		counter++;
		return State.MOVING;
	}
}
