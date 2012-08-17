/*
 * Created on 8 jul 2008
 */

package craterstudio.vecmath;

import craterstudio.vecmath.combo.CircleMath2D;

public class Circle2f {
	public final Vector2f origin;
	public float radius;

	public Circle2f(Vector2f origin, float radius) {
		this.origin = origin;
		this.radius = radius;
	}

	public boolean intersects(Circle2f s) {
		return this.origin.isInRange(s.origin, this.radius + s.radius);
	}

	public boolean contains(Circle2f s) {
		return this.origin.isInRange(s.origin, this.radius - s.radius);
	}

	public boolean isInRange(Circle2f s, float d) {
		return this.origin.isInRange(s.origin, this.radius + s.radius + d);
	}

	public boolean isInRange(Vector2f p, float d) {
		return this.origin.isInRange(p, this.radius + d);
	}

	public float reflect(Ray2f ray) {
		float dist = CircleMath2D.d(this, ray);

		float xHit = ray.origin.x + ray.normal.x * dist;
		float yHit = ray.origin.y + ray.normal.y * dist;

		float nxCircle = (xHit - this.origin.x) / this.radius;
		float nyCircle = (yHit - this.origin.y) / this.radius;

		float nxIncoming = -ray.normal.x;
		float nyIncoming = -ray.normal.y;
		float NdotL = ((nxCircle * nxIncoming) + (nyCircle * nyIncoming));

		float xProjected = nxCircle * NdotL;
		float yProjected = nyCircle * NdotL;

		float xDiff = xProjected - nxIncoming;
		float yDiff = yProjected - nyIncoming;

		float nxReflect = xProjected + xDiff;
		float nyReflect = yProjected + yDiff;

		ray.origin.load(xHit, yHit);
		ray.normal.load(nxReflect, nyReflect);

		return dist;
	}

	@Override
	public int hashCode() {
		return this.origin.hashCode() | (Float.floatToRawIntBits(radius) * 17);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Circle2f) {
			Circle2f that = (Circle2f) obj;
			return this.origin.equals(that.origin) && this.radius == that.radius;
		}
		return false;
	}
}