/*
 * Created on 8 jul 2008
 */

package craterstudio.vecmath;

import craterstudio.math.Matrix3;
import static craterstudio.vecmath.Vector2f.*;

public class Ray2f
{
   public final Vector2f origin;
   public final Vector2f normal;

   public Ray2f(Vector2f origin, Vector2f normal)
   {
      this.origin = origin;
      this.normal = normal;
   }

   public Ray2f copy()
   {
      return new Ray2f(this.origin.copy(), this.normal.copy());
   }

   public Vector2f follow(float distance)
   {
      return add(normal.mul(distance), origin);
   }

   public Ray2f transform(Matrix3 mat)
   {
      Vector2f p1 = new Vector2f(this.origin);
      Vector2f p2 = this.follow(1.0f);

      p1.transform(mat);
      p2.transform(mat);

      Vector2f diff = sub(p2, p1);
      Vector2f n = diff.normalize();

      return new Ray2f(p1, n);
   }
}
