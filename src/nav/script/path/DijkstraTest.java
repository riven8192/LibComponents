package nav.script.path;

public class DijkstraTest {
	public static void main(String[] args) {
		Node a = new Node(0, 0);
		//Node b1 = new Node(1, 0);
		Node b2 = new Node(2.4f, 0);
		Node b3 = new Node(2.0f, 0);
		final Node c = new Node(5, 0);

		//new Edge(a, b1);
		new Edge(a, b2);
		new Edge(a, b3);
		//new Edge(b1, c);
		new Edge(b2, c);
		new Edge(b3, c);

		TargetFunction tf = new TargetFunction() {
			@Override
			public boolean isTarget(Node node) {
				return node == c;
			}
		};

		CostFunction cf = new CostFunction() {
			@Override
			public float calculateCost(Edge edge) {
				return (float) Math.pow(edge.distance, 1.6f);
			}
		};

		System.out.println(Dijkstra.path(a, tf, cf));
	}
}
