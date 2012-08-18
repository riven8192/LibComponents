/*
 * Created on 8 jul 2008
 */

package craterstudio.vecmath;

import static craterstudio.vecmath.Vector3f.*;

public class Plane3f
{
   public static Plane3f create(Vector3f a, Vector3f b, Vector3f c)
   {
      Vector3f ab = b.sub(a.x, a.y, a.z);
      Vector3f ac = c.sub(a.x, a.y, a.z);
      Vector3f cross = cross(ab, ac);
      float distance = dot(cross, a);
      return new Plane3f(cross.normalize(), distance);
   }

   public final Vector3f normal;
   public float          distance;

   public Plane3f(Vector3f normal, float distance)
   {
      this.normal = normal;
      this.distance = distance;
   }

   public final boolean isAbove(Vector3f p)
   {
      return this.signedDistanceTo(p) >= 0.0f;
   }

   public final boolean isFrontFacingTo(Vector3f direction)
   {
      return dot(direction, normal) <= 0.0f;
   }

   public final float signedDistanceTo(Vector3f p)
   {
      return dot(normal, p) + distance;
   }

   public final float reflect(Ray3f ray)
   {
      Vector3f origin = ray.origin;
      Vector3f normal = ray.normal;

      float nx = this.normal.x;
      float ny = this.normal.y;
      float nz = this.normal.z;
      float d = this.distance;

      float px = nx * d;
      float py = ny * d;
      float pz = nz * d;

      float rx = origin.x;
      float ry = origin.y;
      float rz = origin.z;

      float src1x = rx - px;
      float src1y = ry - py;
      float src1z = rz - pz;
      float d2_1 = (src1x * nx + src1y * ny + src1z * nz) * 2.0f;
      float dst1x = px + nx * d2_1 - src1x;
      float dst1y = py + ny * d2_1 - src1y;
      float dst1z = pz + nz * d2_1 - src1z;

      float dx = normal.x;
      float dy = normal.y;
      float dz = normal.z;

      float v0 = -src1x * nx - src1y * ny - src1z * nz;
      float vd = dx * nx + dy * ny + dz * nz;
      if (vd == 0.0f)
         return Float.NaN;
      float v0_vd = v0 / vd;

      float src2x = src1x + dx;
      float src2y = src1y + dy;
      float src2z = src1z + dz;
      float d2_2 = (src2x * nx + src2y * ny + src2z * nz) * 2.0f;
      float dst2x = px + nx * d2_2 - src2x;
      float dst2y = py + ny * d2_2 - src2y;
      float dst2z = pz + nz * d2_2 - src2z;

      normal.x = dst1x - dst2x;
      normal.y = dst1y - dst2y;
      normal.z = dst1z - dst2z;

      origin.x = (dx * v0_vd + rx);
      origin.y = (dy * v0_vd + ry);
      origin.z = (dz * v0_vd + rz);

      return v0_vd;
   }
}
