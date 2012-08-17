/*
 * Created on Oct 10, 2009
 */

package craterstudio.vecmath.combo;

import craterstudio.vecmath.Rect2f;
import craterstudio.vecmath.Triangle2f;
import craterstudio.vecmath.Vector2f;

public class BoundsMath2D
{
   public static Rect2f outer(Rect2f a, Rect2f b)
   {
      return outer(a, b, new Rect2f());
   }

   public static Rect2f outer(Rect2f a, Rect2f b, Rect2f dst)
   {
      float aEndX = a.offset.x + a.size.x;
      float bEndX = b.offset.x + b.size.x;
      float xMin = Math.min(a.offset.x, b.offset.x);
      float xMax = Math.max(aEndX, bEndX);

      float aEndY = a.offset.y + a.size.y;
      float bEndY = b.offset.y + b.size.y;
      float yMin = Math.min(a.offset.y, b.offset.y);
      float yMax = Math.max(aEndY, bEndY);

      dst.offset.x = xMin;
      dst.offset.y = yMin;
      dst.size.x = xMax - xMin;
      dst.size.y = yMax - yMin;
      return dst;
   }

   // clip

   public static Rect2f clip(Rect2f a, Rect2f b)
   {
      return clip(a, b, new Rect2f());
   }

   public static Rect2f clip(Rect2f a, Rect2f b, Rect2f dst)
   {
      float aEndX = a.offset.x + a.size.x;
      float bEndX = b.offset.x + b.size.x;
      float xMin = Math.max(a.offset.x, b.offset.x);
      float xMax = Math.min(aEndX, bEndX);
      if (xMax <= xMin)
         return null;

      float aEndY = a.offset.y + a.size.y;
      float bEndY = b.offset.y + b.size.y;
      float yMin = Math.max(a.offset.y, b.offset.y);
      float yMax = Math.min(aEndY, bEndY);
      if (yMax <= yMin)
         return null;

      dst.offset.x = xMin;
      dst.offset.y = yMin;
      dst.size.x = xMax - xMin;
      dst.size.y = yMax - yMin;
      return dst;
   }

   // aabb

   public static Rect2f aabb(Vector2f... points)
   {
      float xMin = Integer.MAX_VALUE;
      float yMin = Integer.MAX_VALUE;
      float xMax = Integer.MIN_VALUE;
      float yMax = Integer.MIN_VALUE;

      for (Vector2f point : points)
      {
         if (point.x < xMin)
            xMin = point.x;
         if (point.y < yMin)
            yMin = point.y;

         if (point.x > xMax)
            xMax = point.x;
         if (point.y > yMax)
            yMax = point.y;
      }

      Rect2f dst = new Rect2f();
      dst.offset.x = xMin;
      dst.offset.y = yMin;
      dst.size.x = xMax - xMin;
      dst.size.y = yMax - yMin;
      return dst;
   }

   public static Rect2f aabb(Triangle2f tri)
   {
      return aabb(tri, new Rect2f());
   }

   public static Rect2f aabb(Triangle2f tri, Rect2f dst)
   {
      // p1
      float xMin = tri.p1.x;
      float yMin = tri.p1.y;
      float xMax = tri.p1.x;
      float yMax = tri.p1.y;

      // p2
      if (tri.p2.x < xMin)
         xMin = tri.p2.x;
      if (tri.p2.y < yMin)
         yMin = tri.p2.y;

      if (tri.p2.x > xMax)
         xMax = tri.p2.x;
      if (tri.p2.y > yMax)
         yMax = tri.p2.y;

      // p3
      if (tri.p3.x < xMin)
         xMin = tri.p3.x;
      if (tri.p3.y < yMin)
         yMin = tri.p3.y;

      if (tri.p3.x > xMax)
         xMax = tri.p3.x;
      if (tri.p3.y > yMax)
         yMax = tri.p3.y;

      dst.offset.x = xMin;
      dst.offset.y = yMin;
      dst.size.x = xMax - xMin;
      dst.size.y = yMax - yMin;

      return dst;
   }

   public static Rect2f aabb(Triangle2f[] tris, Rect2f dst)
   {
      float xMin = Integer.MAX_VALUE;
      float yMin = Integer.MAX_VALUE;
      float xMax = Integer.MIN_VALUE;
      float yMax = Integer.MIN_VALUE;

      for (Triangle2f tri : tris)
      {
         // p1
         if (tri.p1.x < xMin)
            xMin = tri.p1.x;
         if (tri.p1.y < yMin)
            yMin = tri.p1.y;

         if (tri.p1.x > xMax)
            xMax = tri.p1.x;
         if (tri.p1.y > yMax)
            yMax = tri.p1.y;

         // p2
         if (tri.p2.x < xMin)
            xMin = tri.p2.x;
         if (tri.p2.y < yMin)
            yMin = tri.p2.y;

         if (tri.p2.x > xMax)
            xMax = tri.p2.x;
         if (tri.p2.y > yMax)
            yMax = tri.p2.y;

         // p3
         if (tri.p3.x < xMin)
            xMin = tri.p3.x;
         if (tri.p3.y < yMin)
            yMin = tri.p3.y;

         if (tri.p3.x > xMax)
            xMax = tri.p3.x;
         if (tri.p3.y > yMax)
            yMax = tri.p3.y;
      }

      dst.offset.x = xMin;
      dst.offset.y = yMin;
      dst.size.x = xMax - xMin;
      dst.size.y = yMax - yMin;

      return dst;
   }
}
