/*
 * Created on 8 jul 2008
 */

package craterstudio.vecmath;

import static craterstudio.vecmath.Vector2f.*;

public class Line2f
{
   public final Vector2f p1, p2;

   public Line2f()
   {
      this.p1 = new Vector2f();
      this.p2 = new Vector2f();
   }

   public Line2f(Vector2f p1, Vector2f p2)
   {
      this.p1 = p1;
      this.p2 = p2;
   }

   public Vector2f lerp(float t)
   {
      return add(p1, sub(p2, p1).mul(t));
   }

   public boolean side(Vector2f p)
   {
      return cross(sub(p2, p1), sub(p, p1)) < 0.0f;
   }
}