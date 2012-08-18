/*
 * Created on 22 jul 2010
 */

package craterstudio.vecmath.combo;

import craterstudio.vecmath.Circle2f;
import craterstudio.vecmath.Line2f;
import craterstudio.vecmath.Ray2f;
import craterstudio.vecmath.Vector2f;

public class CircleMath2D
{
   public static boolean intersects(Circle2f circle, Line2f line)
   {
      float x1 = line.p1.x;
      float y1 = line.p1.y;

      float x2 = line.p2.x;
      float y2 = line.p2.y;

      float x3 = circle.origin.x;
      float y3 = circle.origin.y;

      // diff between line-points
      float x21 = x2 - x1;
      float y21 = y2 - y1;

      // diff between first line-point and sphere origin
      float x13 = x1 - x3;
      float y13 = y1 - y3;

      // y = ax^2 + bx + c
      float a = x21 * x21 + y21 * y21;
      float b = 2.0f * ((x21 * x13) + (y21 * y13));
      float c = (x3 * x3) + (y3 * y3);
      c += (x1 * x1) + (y1 * y1);
      c -= 2.0f * (x3 * x1 + y3 * y1);
      c -= circle.radius * circle.radius;

      // b^2 - 4ac
      float determ = b * b - 4.0f * a * c;

      // no hit
      if (determ < 0.0f)
         return false;

      // skip tangent hit (determ == 0.0)

      // intersection, 2 hits
      float inv_a2 = 0.5f / a;
      float sqrt_d = (float) Math.sqrt(determ);

      float u0 = (-b - sqrt_d) * inv_a2;
      if (u0 >= 0.0f && u0 <= 1.0f)
         return true;

      float u1 = (-b + sqrt_d) * inv_a2;
      if (u1 >= 0.0f && u1 <= 1.0f)
         return true;

      return false;
   }

   public static float calcPathLength(Circle2f circle, Line2f line)
   {
      float[] result = new float[2];
      int intersections = calcIntersectionIntervals(circle, line, result);
      if (intersections != 2)
         return 0.0f;

      Vector2f p1 = line.p1.lerp(result[0], line.p2);
      Vector2f p2 = line.p1.lerp(result[1], line.p2);
      return p1.distance(p2);
   }

   private static int calcIntersectionIntervals(Circle2f circle, Line2f line, float[] result)
   {
      float x1 = line.p1.x;
      float y1 = line.p1.y;

      float x2 = line.p2.x;
      float y2 = line.p2.y;

      float x3 = circle.origin.x;
      float y3 = circle.origin.y;

      // diff between line-points
      float x21 = x2 - x1;
      float y21 = y2 - y1;

      // diff between first line-point and sphere origin
      float x13 = x1 - x3;
      float y13 = y1 - y3;

      // y = ax^2 + bx + c
      float a = (x21 * x21) + (y21 * y21);
      float b = 2.0f * ((x21 * x13) + (y21 * y13));
      float c = (x3 * x3) + (y3 * y3);
      c += (x1 * x1) + (y1 * y1);
      c -= 2.0f * (x3 * x1 + y3 * y1);
      c -= circle.radius * circle.radius;

      // b^2 - 4ac
      float determ = b * b - 4.0f * a * c;

      // no hit
      if (determ < 0.0f)
         return 0;

      // tangent hit
      if (determ == 0.0f)
      {
         result[0] = -b / (2.0f * a);
         return 1;
      }

      // intersection, 2 hits
      float inv_a2 = 0.5f / a;
      float sqrt_d = (float) Math.sqrt(determ);
      result[0] = (-b - sqrt_d) * inv_a2;
      result[1] = (-b + sqrt_d) * inv_a2;

      return 2;
   }

   public static float d(Circle2f circle, Ray2f ray)
   {
      Vector2f rn = ray.normal;

      float dx = ray.origin.x - circle.origin.x;
      float dy = ray.origin.y - circle.origin.y;
      float dr = circle.radius;

      float B = (dx * rn.x) + (dy * rn.y);
      float C = (dx * dx) + (dy * dy) - (dr * dr);
      float D = B * B - C;
      float d = D > 0.0 ? -B - (float) Math.sqrt(D) : Float.POSITIVE_INFINITY;

      return d;
   }

   public static float nearestIntersectionDistance(Circle2f circle, Line2f line)
   {
      float x1 = line.p1.x;
      float y1 = line.p1.y;

      float x2 = line.p2.x;
      float y2 = line.p2.y;

      float x3 = circle.origin.x;
      float y3 = circle.origin.y;

      float r = circle.radius;
      float r2 = r * r;

      //

      //float a = (x2 - x1) * 2.0f + (y2 - y1) * 2.0f + (z2 - z1) * 2.0f;
      //float b = 2 * ((x2 - x1) * (x1 - x3) + (y2 - y1) * (y1 - y3) + (z2 - z1) * (z1 - z3));
      //float c = x3 * x3 + y3 * y3 + z3 * z3 + x1 * x1 + y1 * y1 + z1 * z1 - 2.0f * (x3 * x1 + y3 * y1 + z3 * z1) - r2;

      float a = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
      float b = 2 * ((x2 - x1) * (x1 - x3) + (y2 - y1) * (y1 - y3));
      float c = x3 * x3 + y3 * y3 + x1 * x1 + y1 * y1 - 2.0f * (x3 * x1 + y3 * y1) - r2;
      float d = b * b - 4.0f * a * c;

      // no hit
      if (d < 0.0f)
         return Float.NaN;

      // tangent hit
      if (d == 0.0f)
      {
         float u = -b / (2.0f * a);
         return (u < 0.0f) ? Float.NaN : u;
      }

      // intersection, 2 hits
      float sqrt_d = (float) Math.sqrt(d);
      float u1 = (-b - sqrt_d) / (2.0f * a);
      float u2 = (-b + sqrt_d) / (2.0f * a);

      //if (true)
      //  return Math.min(u1, u2);

      if (u1 < 0.0f)
         return (u2 < 0.0f) ? Float.NaN : u2;
      return (u2 < 0.0f) ? u1 : Math.min(u1, u2);
   }
}