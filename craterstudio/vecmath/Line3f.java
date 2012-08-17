/*
 * Created on 8 jul 2008
 */

package craterstudio.vecmath;

import static craterstudio.vecmath.Vector3f.*;

public class Line3f
{
   public final Vector3f p1, p2;

   public Line3f()
   {
      this.p1 = new Vector3f();
      this.p2 = new Vector3f();
   }

   public Line3f(Vector3f p1, Vector3f p2)
   {
      this.p1 = p1;
      this.p2 = p2;
   }

   public Vector3f lerp(float t)
   {
      return add(p1, sub(p2, p1).mul(t));
   }
}
