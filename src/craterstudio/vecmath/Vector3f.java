/*
 * Created on 8 jul 2008
 */

package craterstudio.vecmath;

import craterstudio.math.Matrix4;

public class Vector3f {
	public float x, y, z;

	public Vector3f() {
		this(0, 0, 0);
	}

	public Vector3f(Vector3f p) {
		this(p.x, p.y, p.z);
	}

	public Vector3f(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	//

	public Vector3f transform(Matrix4 mat) {
		float x = mat.m00 * this.x + mat.m01 * this.y + mat.m02 * this.z + mat.m03;
		float y = mat.m10 * this.x + mat.m11 * this.y + mat.m12 * this.z + mat.m13;
		float z = mat.m20 * this.x + mat.m21 * this.y + mat.m22 * this.z + mat.m23;

		return new Vector3f(x, y, z);
	}

	//

	public Vector3f load(Vector3f p) {
		return this.load(p.x, p.y, p.z);
	}

	public Vector3f load(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	//

	public static Vector3f add(Vector3f a, Vector3f b) {
		return new Vector3f(a.x + b.x, a.y + b.y, a.z + b.z);
	}

	public static Vector3f sub(Vector3f a, Vector3f b) {
		return new Vector3f(a.x - b.x, a.y - b.y, a.z - b.z);
	}

	public static Vector3f mul(Vector3f a, Vector3f b) {
		return new Vector3f(a.x * b.x, a.y * b.y, a.z * b.z);
	}

	public static Vector3f div(Vector3f a, Vector3f b) {
		return new Vector3f(a.x / b.x, a.y / b.y, a.z / b.z);
	}

	//

	public static float dot(Vector3f a, Vector3f b) {
		return (a.x * b.x) + (a.y * b.y) + (a.z * b.z);
	}

	public static Vector3f cross(Vector3f a, Vector3f b) {
		return new Vector3f(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x);
	}

	//

	public Vector3f add(float x, float y, float z) {
		return new Vector3f(this.x + x, this.y + y, this.z + z);
	}

	public Vector3f sub(float x, float y, float z) {
		return new Vector3f(this.x - x, this.y - y, this.z - z);
	}

	public Vector3f mul(float s) {
		return this.mul(s, s, s);
	}

	public Vector3f mul(float x, float y, float z) {
		return new Vector3f(this.x * x, this.y * y, this.z * z);
	}

	public Vector3f div(float s) {
		return this.div(s, s, s);
	}

	public Vector3f div(float x, float y, float z) {
		return new Vector3f(this.x / x, this.y / y, this.z / z);
	}

	//

	public Vector3f lerp(float t, Vector3f p) {
		return new Vector3f(x + t * (p.x - x), y + t * (p.y - y), z + t * (p.z - z));
	}

	//

	public float squaredDistance(Vector3f p) {
		float x = this.x - p.x;
		float y = this.y - p.y;
		float z = this.z - p.z;

		return x * x + y * y + z * z;
	}

	public float distance(Vector3f p) {
		return (float) Math.sqrt(this.squaredDistance(p));
	}

	public boolean isInRange(Vector3f that, float d) {
		float x = this.x - that.x;
		float y = this.y - that.y;
		float z = this.z - that.z;

		return (x * x + y * y + z * z) < (d * d);
	}

	//

	public float squaredLength() {
		return (x * x) + (y * y) + (z * z);
	}

	public float length() {
		return (float) Math.sqrt(this.squaredLength());
	}

	public Vector3f length(float val) {
		float li = val / this.length();
		return new Vector3f(x * li, y * li, z * li);
	}

	public Vector3f normalize() {
		return this.length(1.0f);
	}

	//
	@Override
	public int hashCode() {
		int hash = 0;
		hash |= Float.floatToRawIntBits(x) * 17;
		hash |= Float.floatToRawIntBits(y) * 37;
		hash |= Float.floatToRawIntBits(z) * 43;
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Vector3f) {
			Vector3f that = (Vector3f) obj;
			return this.x == that.x && this.y == that.y && this.z == that.z;
		}
		return false;
	}

	public String toString() {
		return this.getClass().getSimpleName() + "[" + x + ", " + y + ", " + z + "]";
	}
}