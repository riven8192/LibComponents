/*
 * Created on 8 jul 2008
 */

package craterstudio.vecmath;

import static craterstudio.vecmath.Vector2f.*;

public class Plane2f
{
   public static Plane2f create(Vector2f a, Vector2f b)
   {
      float neg = (b.x - a.x) * (a.y - 0.f) - (a.x - 0.f) * (b.y - a.y);
      float div = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y);
      float d = -neg / (float) Math.sqrt(div);
      return new Plane2f(sub(b, a).normalize().perpendicular(), d);
   }

   public final Vector2f normal;
   public float          distance;

   public Plane2f(Vector2f normal, float distance)
   {
      this.normal = normal;
      this.distance = distance;
   }

   public final boolean isAbove(Vector2f p)
   {
      return this.signedDistanceTo(p) >= 0.0f;
   }

   public final boolean isFrontFacingTo(Vector2f direction)
   {
      return dot(direction, normal) <= 0.0f;
   }

   public final float signedDistanceTo(Vector2f p)
   {
      return dot(normal, p) + distance;
   }

   public final Vector2f getPointAt(float t)
   {
      Vector2f origin = new Vector2f(this.normal).mul(this.distance);

      Vector2f traverse = this.normal.perpendicular().mul(t);
      return origin.add(traverse.x, traverse.y);
   }

   public final float reflect(Ray2f ray)
   {
      Vector2f origin = ray.origin;
      Vector2f normal = ray.normal;

      float nx = this.normal.x;
      float ny = this.normal.y;
      float d = this.distance;

      float px = nx * d;
      float py = ny * d;

      float rx = origin.x;
      float ry = origin.y;

      float src1x = rx - px;
      float src1y = ry - py;
      float d2_1a = (src1x * nx + src1y * ny) * 2.0f;
      float dst1x = px + nx * d2_1a - src1x;
      float dst1y = py + ny * d2_1a - src1y;

      float dx = normal.x;
      float dy = normal.y;

      float v0 = -src1x * nx - src1y * ny;
      float vd = dx * nx + dy * ny;
      if (vd == 0.0f)
         return Float.NaN;
      float v0_vd = v0 / vd;

      float src2x = src1x + dx;
      float src2y = src1y + dy;
      float d2_2a = (src2x * nx + src2y * ny) * 2.0f;
      float dst2x = px + nx * d2_2a - src2x;
      float dst2y = py + ny * d2_2a - src2y;

      normal.x = dst1x - dst2x;
      normal.y = dst1y - dst2y;

      origin.x = dx * v0_vd + rx;
      origin.y = dy * v0_vd + ry;

      return v0_vd;
   }
}