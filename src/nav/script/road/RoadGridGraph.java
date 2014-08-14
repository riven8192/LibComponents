package nav.script.road;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import nav.script.path.Edge;
import nav.script.path.Node;
import nav.script.road.RoadGrid.RoadType;
import nav.util.Vec2;

public class RoadGridGraph {

	public static Collection<Node> createGraph(RoadGrid grid) {
		Map<Long, Node> idx2node = new HashMap<>();
		for(int[] xy : grid.nodes()) {
			int x = xy[0];
			int y = xy[1];
			idx2node.put(RoadGrid.idxKey(x, y), new Node(new Vec2(x, y)));
		}

		for(int[] xy : grid.nodes()) {
			int x = xy[0];
			int y = xy[1];

			for(int i = 0; i < 4; i++) {
				// four directions
				int x2 = x + dx[i];
				int y2 = y + dy[i];

				if(grid.isSet(x2, y2)) {
					Node src = idx2node.get(RoadGrid.idxKey(x, y));
					Node dst = idx2node.get(RoadGrid.idxKey(x2, y2));

					RoadType srcRT = grid.getRoadType(x, y);
					RoadType dstRT = grid.getRoadType(x2, y2);
					RoadType rType = RoadType.slowest(srcRT, dstRT);

					new Edge(src, dst, rType);
				}
			}

			if(grid.getRoadType(x, y) == RoadType.INTERSECTION) {
				for(int i = 0; i < 2; i++) {
					// axis aligned
					int x1 = x + dx[i + 0];
					int y1 = y + dy[i + 0];
					int x2 = x + dx[i + 2];
					int y2 = y + dy[i + 2];

					if(grid.isSet(x1, y1) && grid.isSet(x2, y2)) {
						Node src = idx2node.get(RoadGrid.idxKey(x1, y1));
						Node dst = idx2node.get(RoadGrid.idxKey(x2, y2));

						new Edge(src, dst, RoadType.INTERSECTION_PASS);
					}
				}
			}
		}

		return idx2node.values();
	}

	private static final int[] dx = new int[] { -1, 0, +1, 0 };
	private static final int[] dy = new int[] { 0, -1, 0, +1 };
}