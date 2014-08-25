package nav.script.road;

import nav.util.Vec2;

public class RoadJourneyVehicle {
	public final RoadJourney journey;
	private final RoadJourneyStepper head;
	private final RoadJourneyStepper tail;
	private int len, enterRem, leaveRem;

	public RoadJourneyVehicle(RoadJourney journey, int len) {
		this.journey = journey;
		this.head = new RoadJourneyStepper(journey, true);
		this.tail = new RoadJourneyStepper(journey, false);
		this.len = this.enterRem = this.leaveRem = len;
		tileTrail = new Vec2[len];
	}

	public RoadJourneyStepper head() {
		return head;
	}

	public RoadJourneyStepper tail() {
		return tail;
	}

	public boolean hasHistory(int age) {
		int size = Math.min(trailIndex, tileTrail.length);
		int index = (size - 1) - age;
		return (index >= 0);
	}

	public Vec2 getHistory(int age) {
		int size = Math.min(trailIndex, tileTrail.length);
		int index = (size - 1) - age;
		return (index >= 0) ? tileTrail[(trailIndex + tileTrail.length - age) % tileTrail.length] : null;
	}

	private Vec2[] tileTrail;
	private int trailIndex = -1;

	public float subTilesPerStep;
	private float subTilesTraversed;

	public RoadJourneyStepper.State step() {
		subTilesTraversed += subTilesPerStep;
		if(subTilesTraversed < 1.0f)
			return RoadJourneyStepper.State.SUB_MOVE;
		subTilesTraversed -= 1.0f;

		if(len == leaveRem) {
			switch (head.step()) {
			case MOVING:
				tileTrail[++trailIndex % tileTrail.length] = head.getCoords();
				enterRem--;
				break;
			case BLOCKED:
				return RoadJourneyStepper.State.BLOCKED;
			case ARRIVED:
				leaveRem--;
				break;
			case SUB_MOVE:
				throw new IllegalStateException();
			}
		}
		else if(--leaveRem < 0) {
			return RoadJourneyStepper.State.ARRIVED;
		}

		if(enterRem < 0) {
			tail.step();
		}
		return RoadJourneyStepper.State.MOVING;
	}
}
