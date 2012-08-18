/*
 * Created on 3-nov-2007
 */

package craterstudio.verlet;

import craterstudio.math.Vec3;
import craterstudio.math.VecMath;

public class VerletPlane
{
   public float nx, ny, nz, d;

   public final void inferValues(Vec3 src, Vec3 dst)
   {
      float nx = dst.x - src.x;
      float ny = dst.y - src.y;
      float nz = dst.z - src.z;

      float d = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
      nx /= d;
      ny /= d;
      nz /= d;

      float d2 = VecMath.dot(src, src);
      d2 = (d2 == 0.0f) ? 1.0f : (float) Math.sqrt(d2);
      float nx2 = src.x / d2;
      float ny2 = src.y / d2;
      float nz2 = src.z / d2;

      d2 *= nx * nx2 + ny * ny2 + nz * nz2;

      this.nx = nx;
      this.ny = ny;
      this.nz = nz;
      this.d = d2;
   }

   public final static VerletPlane infer(Vec3 src, Vec3 dst)
   {
      VerletPlane plane = new VerletPlane();
      plane.inferValues(src, dst);
      return plane;
   }
}
