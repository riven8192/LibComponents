/*
 * Created on 8 jul 2008
 */

package craterstudio.vecmath;

import craterstudio.math.Matrix4;
import static craterstudio.vecmath.Vector3f.*;

public class Ray3f
{
   public final Vector3f origin;
   public final Vector3f normal;

   public Ray3f(Vector3f origin, Vector3f normal)
   {
      this.origin = origin;
      this.normal = normal;
   }

   public Vector3f traverse(float distance)
   {
      return add(normal.mul(distance), origin);
   }

   public Ray3f transform(Matrix4 mat)
   {
      Vector3f p1 = new Vector3f(this.origin);
      Vector3f p2 = this.traverse(1.0f);

      p1.transform(mat);
      p2.transform(mat);

      Vector3f diff = sub(p2, p1);
      Vector3f n = diff.normalize();

      return new Ray3f(p1, n);
   }
}
