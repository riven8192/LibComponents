package nav.script.road;

import nav.util.Vec2;

public class RoadJourneyStepper {
	public static enum State {
		MOVING, SUB_MOVE, BLOCKED, ARRIVED;
	}

	public final RoadJourney journey;

	private int tileIndex;
	private int tileCell;
	private final boolean doEnter;
	private int moveCounter;

	public RoadJourneyStepper(RoadJourney journey, boolean doEnter) {
		this.journey = journey;
		this.doEnter = doEnter;
		this.tileCell = -1;
	}

	public RoadTile getTile() {
		return journey.tiles.get(tileIndex);
	}

	public int getTileBit() {
		int[] bits = journey.tileBits.get(tileIndex);
		return bits[tileCell == -1 ? 0 : tileCell];
	}

	public Vec2 getCoords() {
		if(tileIndex == -1)
			return null;
		RoadTile tile = this.getTile();
		int bit = this.getTileBit();
		int x = RoadTile.bitToX(bit);
		int y = RoadTile.bitToY(bit);
		return new Vec2(//
				tile.x + 0.125f + x * 0.25f,//
				tile.y + 0.125f + y * 0.25f);
	}

	public int counter() {
		return moveCounter;
	}

	public State step() {
		int[] bits = journey.tileBits.get(tileIndex);
		if(tileCell == bits.length - 1) {
			if(++tileIndex == journey.tiles.size())
				if(true)
					tileIndex = 0;
				else
					return State.ARRIVED;
			bits = journey.tileBits.get(tileIndex);
			tileCell = -1;
		}
		RoadTile tile = journey.tiles.get(tileIndex);
		int nextCell = bits[tileCell + 1];
		if(doEnter) {
			if(!tile.isEmpty(nextCell))
				return State.BLOCKED;
			if(!tile.isEnterAllowed(nextCell))
				return State.BLOCKED;
			tile.enter(nextCell);
		}
		else {
			tile.leave(nextCell);
		}
		tileCell++;
		moveCounter++;
		return State.MOVING;
	}
}