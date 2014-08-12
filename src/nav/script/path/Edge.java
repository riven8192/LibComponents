package nav.script.path;

public class Edge {
	private static int id_gen;

	public final int id;
	public final Node from, to;
	public final float distance;

	public Edge(Node from, Node to) {
		this.id = ++id_gen;
		this.from = from;
		this.to = to;

		{
			float dx = from.x - to.x;
			float dy = from.y - to.y;
			this.distance = (float) Math.sqrt(dx * dx + dy * dy);
		}

		this.from.edges.add(this);
		this.to.trace.add(this);
	}

	public void destroy() {
		boolean found = this.from.edges.remove(this);
		if(!found) {
			throw new IllegalStateException();
		}
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Edge) && ((Edge) obj).id == this.id;
	}
}
