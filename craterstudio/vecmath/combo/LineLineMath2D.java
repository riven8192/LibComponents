/*
 * Created on 22 jul 2010
 */

package craterstudio.vecmath.combo;

import craterstudio.vecmath.Line2f;
import craterstudio.vecmath.Vector2f;

public class LineLineMath2D
{
   public static boolean intersecting(Line2f a, Line2f b)
   {
      Vector2f p1 = a.p1;
      Vector2f p2 = a.p2;
      Vector2f p3 = b.p1;
      Vector2f p4 = b.p2;

      float u;
      float d = (p4.y - p3.y) * (p2.x - p1.x) - (p4.x - p3.x) * (p2.y - p1.y);
      if (d == 0)
         return false;
      float inv_d = 1.0f / d;

      float p = (p4.x - p3.x) * (p1.y - p3.y) - (p4.y - p3.y) * (p1.x - p3.x);
      u = p * inv_d;
      if (u < 0.0f || u > 1.0f)
         return false;

      float q = (p2.x - p1.x) * (p1.y - p3.y) - (p2.y - p1.y) * (p1.x - p3.x);
      u = q * inv_d;
      if (u < 0.0f || u > 1.0f)
         return false;

      return true;
   }

   public static Vector2f intersectionPoint(Line2f a, Line2f b)
   {
      Vector2f p1 = a.p1;
      Vector2f p2 = a.p2;
      Vector2f p3 = b.p1;
      Vector2f p4 = b.p2;

      float u;
      float d = (p4.y - p3.y) * (p2.x - p1.x) - (p4.x - p3.x) * (p2.y - p1.y);
      if (d == 0)
         return null;
      float inv_d = 1.0f / d;

      float p = (p4.x - p3.x) * (p1.y - p3.y) - (p4.y - p3.y) * (p1.x - p3.x);
      u = p * inv_d;
      if (u < 0.0f || u > 1.0f)
         return null;

      float q = (p2.x - p1.x) * (p1.y - p3.y) - (p2.y - p1.y) * (p1.x - p3.x);
      u = q * inv_d;
      if (u < 0.0f || u > 1.0f)
         return null;

      Vector2f c = new Vector2f();
      c.x = p1.x + u * (p2.x - p1.x);
      c.y = p1.y + u * (p2.y - p1.y);
      return c;
   }

   public static boolean infiniteIntersecting(Line2f a, Line2f b)
   {
      Vector2f p1 = a.p1;
      Vector2f p2 = a.p2;
      Vector2f p3 = b.p1;
      Vector2f p4 = b.p2;

      float q = (p4.y - p3.y) * (p2.x - p1.x) - (p4.x - p3.x) * (p2.y - p1.y);

      return (q != 0.0f);
   }

   public static Vector2f infiniteIntersectionPoint(Line2f a, Line2f b)
   {
      Vector2f p1 = a.p1;
      Vector2f p2 = a.p2;
      Vector2f p3 = b.p1;
      Vector2f p4 = b.p2;

      float p = (p4.x - p3.x) * (p1.y - p3.y) - (p4.y - p3.y) * (p1.x - p3.x);
      float q = (p4.y - p3.y) * (p2.x - p1.x) - (p4.x - p3.x) * (p2.y - p1.y);
      if (q == 0.0f)
         return null;
      float u = p / q;

      Vector2f c = new Vector2f();
      c.x = p1.x + u * (p2.x - p1.x);
      c.y = p1.y + u * (p2.y - p1.y);
      return c;
   }
}
