/*
 * Created on 8 jul 2008
 */

package craterstudio.vecmath;

import craterstudio.math.Matrix3;

public class Vector2f {
	public float x, y;

	public Vector2f() {
		this(0, 0);
	}

	public Vector2f(Vector2f p) {
		this(p.x, p.y);
	}

	public Vector2f(float x, float y) {
		this.x = x;
		this.y = y;
	}

	//

	public Vector2f copy() {
		return new Vector2f(this);
	}

	//

	public Vector2f load(Vector2f p) {
		return this.load(p.x, p.y);
	}

	public Vector2f load(float x, float y) {
		this.x = x;
		this.y = y;
		return this;
	}

	//

	public Vector2f transform(Matrix3 mat) {
		float x = mat.m00 * this.x + mat.m01 * this.y + mat.m02;
		float y = mat.m10 * this.x + mat.m11 * this.y + mat.m12;
		return new Vector2f(x, y);
	}

	//

	public static Vector2f add(Vector2f a, Vector2f b) {
		return new Vector2f(a.x + b.x, a.y + b.y);
	}

	public static Vector2f sub(Vector2f a, Vector2f b) {
		return new Vector2f(a.x - b.x, a.y - b.y);
	}

	public static Vector2f mul(Vector2f a, Vector2f b) {
		return new Vector2f(a.x * b.x, a.y * b.y);
	}

	public static Vector2f div(Vector2f a, Vector2f b) {
		return new Vector2f(a.x / b.x, a.y / b.y);
	}

	public static float dot(Vector2f a, Vector2f b) {
		return (a.x * b.x) + (a.y * b.y);
	}

	public static float cross(Vector2f a, Vector2f b) {
		return (a.x * b.y) - (a.y * b.x);
	}

	//

	public Vector2f add(float x, float y) {
		return new Vector2f(this.x + x, this.y + y);
	}

	public Vector2f sub(float x, float y) {
		return new Vector2f(this.x - x, this.y - y);
	}

	public Vector2f mul(float s) {
		return this.mul(s, s);
	}

	public Vector2f mul(float x, float y) {
		return new Vector2f(this.x * x, this.y * y);
	}

	public Vector2f div(float s) {
		return this.div(s, s);
	}

	public Vector2f div(float x, float y) {
		return new Vector2f(this.x / x, this.y / y);
	}

	//

	public Vector2f lerp(float t, Vector2f p) {
		return new Vector2f(x + t * (p.x - x), y + t * (p.y - y));
	}

	//

	public float squaredDistance(Vector2f p) {
		float x = this.x - p.x;
		float y = this.y - p.y;

		return x * x + y * y;
	}

	public float distance(Vector2f p) {
		return (float) Math.sqrt(this.squaredDistance(p));
	}

	public Vector2f perpendicular() {
		return new Vector2f(this.y, -this.x);
	}

	public boolean isInRange(Vector2f p, float d) {
		float x = this.x - p.x;
		float y = this.y - p.y;

		return (x * x + y * y) < (d * d);
	}

	//

	public float squaredLength() {
		return (x * x) + (y * y);
	}

	public float length() {
		return (float) Math.sqrt(this.squaredLength());
	}

	public Vector2f length(float val) {
		float li = val / this.length();
		return new Vector2f(x * li, y * li);
	}

	public Vector2f normalize() {
		return this.length(1.0f);
	}

	//

	@Override
	public int hashCode() {
		int hash = 0;
		hash |= Float.floatToRawIntBits(x) * 17;
		hash |= Float.floatToRawIntBits(y) * 37;
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Vector2f) {
			Vector2f that = (Vector2f) obj;
			return this.x == that.x && this.y == that.y;
		}
		return false;
	}

	public String toString() {
		return "v2f[" + x + "," + y + "]";
	}
}