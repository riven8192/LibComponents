/*
 * Created on 8 jul 2008
 */

package craterstudio.vecmath;

public class Triangle2f
{
   public final Vector2f p1, p2, p3;

   public Triangle2f()
   {
      this.p1 = new Vector2f();
      this.p2 = new Vector2f();
      this.p3 = new Vector2f();
   }

   public Triangle2f(Vector2f a, Vector2f b, Vector2f c)
   {
      this.p1 = a;
      this.p2 = b;
      this.p3 = c;
   }
}
