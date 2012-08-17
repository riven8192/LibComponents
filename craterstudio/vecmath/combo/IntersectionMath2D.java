/*
 * Created on Oct 10, 2009
 */

package craterstudio.vecmath.combo;

import craterstudio.vecmath.Line2f;
import craterstudio.vecmath.Rect2f;
import craterstudio.vecmath.Triangle2f;
import craterstudio.vecmath.Vector2f;

public class IntersectionMath2D
{
   public static boolean rectangleContainsPoints(Rect2f rect, Vector2f point)
   {
      if (point.x < rect.offset.x)
         return false;
      if (point.y < rect.offset.y)
         return false;
      if (point.x > rect.offset.x + rect.size.x)
         return false;
      if (point.y > rect.offset.y + rect.size.y)
         return false;
      return true;
   }


   //


   public static boolean isPointInTriangle(Triangle2f tri, Vector2f point)
   {
      Vector2f p1 = tri.p1;
      Vector2f p2 = tri.p2;
      Vector2f p3 = tri.p3;

      float p2p1x = p2.x - p1.x;
      float p2p1y = p2.y - p1.y;
      float p0p1x = point.x - p1.x;
      float p0p1y = point.y - p1.y;
      boolean baSide = (p2p1x * p0p1y) - (p2p1y * p0p1x) < 0.0f;

      float p3p2x = p3.x - p2.x;
      float p3p2y = p3.y - p2.y;
      float p0p2x = point.x - p2.x;
      float p0p2y = point.y - p2.y;
      boolean bbSide = (p3p2x * p0p2y) - (p3p2y * p0p2x) < 0.0f;

      if (baSide != bbSide)
         return false;

      float p1p3x = p1.x - p3.x;
      float p1p3y = p1.y - p3.y;
      float p0p3x = point.x - p3.x;
      float p0p3y = point.y - p3.y;
      boolean bcSide = (p1p3x * p0p3y) - (p1p3y * p0p3x) < 0.0f;

      if (baSide != bcSide)
         return false;

      return true;
   }

   public static boolean arePointsInTriangle(Triangle2f tri, Vector2f... points)
   {
      Vector2f p1 = tri.p1;
      Vector2f p2 = tri.p2;
      Vector2f p3 = tri.p3;

      float p2p1x = p2.x - p1.x;
      float p2p1y = p2.y - p1.y;
      float p3p2x = p3.x - p2.x;
      float p3p2y = p3.y - p2.y;
      float p1p3x = p1.x - p3.x;
      float p1p3y = p1.y - p3.y;

      for (Vector2f point : points)
      {
         float p0p1x = point.x - p1.x;
         float p0p1y = point.y - p1.y;
         boolean baSide = (p2p1x * p0p1y) - (p2p1y * p0p1x) < 0.0f;

         float p0p2x = point.x - p2.x;
         float p0p2y = point.y - p2.y;
         boolean bbSide = (p3p2x * p0p2y) - (p3p2y * p0p2x) < 0.0f;

         if (baSide != bbSide)
            return false;

         float p0p3x = point.x - p3.x;
         float p0p3y = point.y - p3.y;
         boolean bcSide = (p1p3x * p0p3y) - (p1p3y * p0p3x) < 0.0f;

         if (baSide != bcSide)
            return false;
      }

      return true;
   }

   public static boolean containsFully(Triangle2f tri, Rect2f rect)
   {
      Vector2f[] pnts = new Vector2f[4];

      pnts[0] = rect.offset;
      pnts[1] = new Vector2f(rect.offset.x + rect.size.x, rect.offset.y);
      pnts[2] = Vector2f.add(rect.offset, rect.size);
      pnts[3] = new Vector2f(rect.offset.x, rect.offset.y + rect.size.y);

      return arePointsInTriangle(tri, pnts);
   }

   public static boolean quadContainsRectangle(Triangle2f a, Triangle2f b, Rect2f rect)
   {
      Vector2f[] pnts = new Vector2f[4];

      pnts[0] = rect.offset;
      pnts[1] = new Vector2f(rect.offset.x + rect.size.x, rect.offset.y);
      pnts[2] = Vector2f.add(rect.offset, rect.size);
      pnts[3] = new Vector2f(rect.offset.x, rect.offset.y + rect.size.y);

      for (Vector2f pnt : pnts)
      {
         if (isPointInTriangle(a, pnt))
            continue;
         if (isPointInTriangle(b, pnt))
            continue;
         return false;
      }

      return true;
   }

   public static boolean intersects(Triangle2f tri, Rect2f rect)
   {
      /*
      boolean p1_xLess = tri.p1.x < rect.offset.x;
      boolean p2_xLess = tri.p2.x < rect.offset.x;
      boolean p3_xLess = tri.p3.x < rect.offset.x;
      if (p1_xLess && p2_xLess && p3_xLess)
         return false;

      boolean p1_yLess = tri.p1.y < rect.offset.y;
      boolean p2_yLess = tri.p2.y < rect.offset.y;
      boolean p3_yLess = tri.p3.y < rect.offset.y;
      if (p1_yLess && p2_yLess && p3_yLess)
         return false;

      boolean p1_xMore = tri.p1.x > rect.offset.x + rect.size.x;
      boolean p2_xMore = tri.p2.x > rect.offset.x + rect.size.x;
      boolean p3_xMore = tri.p3.x > rect.offset.x + rect.size.x;
      if (p1_xMore && p2_xMore && p3_xMore)
         return false;

      boolean p1_yMore = tri.p1.y > rect.offset.y + rect.size.y;
      boolean p2_yMore = tri.p2.y > rect.offset.y + rect.size.y;
      boolean p3_yMore = tri.p3.y > rect.offset.y + rect.size.y;
      if (p1_yMore && p2_yMore && p3_yMore)
         return false;
         */

      // any triangle point in rectangle 
      if (rectangleContainsPoints(rect, tri.p1))
         return true;
      if (rectangleContainsPoints(rect, tri.p2))
         return true;
      if (rectangleContainsPoints(rect, tri.p3))
         return true;

      //boolean any_xLess = (p1_xLess || p2_xLess || p3_xLess);
      //boolean any_yLess = (p1_yLess || p2_yLess || p3_yLess);
      //boolean any_xMore = (p1_xMore || p2_xMore || p3_xMore);
      //boolean any_yMore = (p1_yMore || p2_yMore || p3_yMore);

      // any rectangle point in triangle
      Vector2f[] pnts = new Vector2f[4];
      pnts[0] = rect.offset;
      pnts[1] = new Vector2f(rect.offset.x + rect.size.x, rect.offset.y);
      pnts[2] = Vector2f.add(rect.offset, rect.size);
      pnts[3] = new Vector2f(rect.offset.x, rect.offset.y + rect.size.y);
      if (arePointsInTriangle(tri, pnts))
         return true;

      // any triangle lines intersect any rectangle lines
      Line2f aLine3f = new Line2f(tri.p1, tri.p2);
      Line2f bLine3f = new Line2f(tri.p2, tri.p3);
      Line2f cLine3f = new Line2f(tri.p3, tri.p1);

      Line2f rLine1 = new Line2f(pnts[0], pnts[2]);
      if (LineLineMath2D.intersecting(aLine3f, rLine1))
         return true;
      if (LineLineMath2D.intersecting(bLine3f, rLine1))
         return true;
      if (LineLineMath2D.intersecting(cLine3f, rLine1))
         return true;

      Line2f rLine2 = new Line2f(pnts[1], pnts[2]);
      if (LineLineMath2D.intersecting(aLine3f, rLine2))
         return true;
      if (LineLineMath2D.intersecting(bLine3f, rLine2))
         return true;
      if (LineLineMath2D.intersecting(cLine3f, rLine2))
         return true;

      Line2f rLine3 = new Line2f(pnts[2], pnts[3]);
      if (LineLineMath2D.intersecting(aLine3f, rLine3))
         return true;
      if (LineLineMath2D.intersecting(bLine3f, rLine3))
         return true;
      if (LineLineMath2D.intersecting(cLine3f, rLine3))
         return true;

      Line2f rLine4 = new Line2f(pnts[3], pnts[0]);
      if (LineLineMath2D.intersecting(aLine3f, rLine4))
         return true;
      if (LineLineMath2D.intersecting(bLine3f, rLine4))
         return true;
      if (LineLineMath2D.intersecting(cLine3f, rLine4))
         return true;

      return false;
   }

   static boolean intersectsSlow(Triangle2f tri, Vector2f point)
   {
      Line2f aLine3f = new Line2f(tri.p1, tri.p2);
      boolean baSide = aLine3f.side(point);

      Line2f bLine3f = new Line2f(tri.p2, tri.p3);
      boolean bbSide = bLine3f.side(point);
      if (baSide != bbSide)
         return false;

      Line2f cLine3f = new Line2f(tri.p3, tri.p1);
      boolean bcSide = cLine3f.side(point);
      if (baSide != bcSide)
         return false;

      return true;
   }
}
