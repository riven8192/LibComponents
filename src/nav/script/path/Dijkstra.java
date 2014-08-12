package nav.script.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class Dijkstra {
	public static List<Node> path(Node origin, TargetFunction tf, CostFunction cf) {
		Set<Node> origins = new HashSet<>();
		origins.add(origin);
		return path(origins, tf, cf);
	}

	public static List<Node> path(Set<Node> origins, TargetFunction tf, CostFunction cf) {

		Map<Node, Float> open = new HashMap<>();
		Map<Node, Float> closed = new HashMap<>();

		for(Node origin : origins) {
			open.put(origin, Float.valueOf(0.0f));
		}

		while (true) {
			Node best = null;
			float cost = Integer.MAX_VALUE;
			for(Entry<Node, Float> entry : open.entrySet()) {
				if(best == null || entry.getValue().floatValue() < cost) {
					best = entry.getKey();
					cost = entry.getValue().floatValue();
				}
			}

			if(best == null) {
				return null;
			}

			if(tf.isTarget(best)) {
				return backtrack(best, open, closed, cf);
			}

			open.remove(best);
			closed.put(best, cost);

			for(Edge edge : best.edges) {
				float costTo = cost + cf.calculateCost(edge);
				if(closed.containsKey(edge.to))
					continue;
				if(!open.containsKey(edge.to) || costTo < open.get(edge.to).floatValue()) {
					open.put(edge.to, Float.valueOf(costTo));
				}
			}
		}
	}

	private static List<Node> backtrack(Node at, Map<Node, Float> open, Map<Node, Float> closed, CostFunction cf) {
		List<Node> path = new ArrayList<>();

		while (true) {
			path.add(at);

			Node to = null;
			float to1 = Integer.MAX_VALUE;
			float to2 = Integer.MAX_VALUE;
			for(Edge back : at.trace) {
				Float fromCost = closed.get(back.from);
				if(fromCost == null)
					continue;
				to1 = fromCost.floatValue();
				float backCost = to1 + cf.calculateCost(back);
				if(to == null || backCost < to2) {
					to = back.from;
					to2 = backCost;
				}
			}
			if(to == null)
				throw new IllegalStateException();
			at = to;

			if(to1 == 0.0f) {
				path.add(at);
				break;
			}
		}
		Collections.reverse(path);
		return path;
	}
}
