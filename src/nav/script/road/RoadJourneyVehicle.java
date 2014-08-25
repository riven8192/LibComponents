package nav.script.road;

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
	}

	public RoadJourneyStepper head() {
		return head;
	}

	public RoadJourneyStepper tail() {
		return tail;
	}

	public RoadJourneyStepper.State step() {
		if(len == leaveRem) {
			switch (head.step()) {
			case MOVING:
				enterRem--;
				break;
			case BLOCKED:
				return RoadJourneyStepper.State.BLOCKED;
			case ARRIVED:
				leaveRem--;
				break;
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
