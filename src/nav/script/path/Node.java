package nav.script.path;

import java.util.ArrayList;
import java.util.List;

public class Node {
	private static int id_gen;

	public int id;
	public final float x, y;
	public final List<Edge> edges;
	public final List<Edge> trace;

	public Node(float x, float y) {
		this.id = ++id_gen;
		this.x = x;
		this.y = y;
		this.edges = new ArrayList<>();
		this.trace = new ArrayList<>();
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Node) && ((Node) obj).id == id;
	}

	@Override
	public String toString() {
		return "Node#" + id;
	}
}
