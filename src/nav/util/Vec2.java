package nav.util;

public class Vec2 {
	public float x, y;

	public static float distance(Vec2 a, Vec2 b) {
		float dx = a.x - b.x;
		float dy = a.y - b.y;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}
}
