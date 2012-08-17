/*
 * Created on 22 jul 2010
 */

package craterstudio.vecmath.combo;

import craterstudio.vecmath.Line3f;
import craterstudio.vecmath.Sphere3f;
import craterstudio.vecmath.Vector3f;

public class SphereMath3D
{
   public static boolean intersects(Sphere3f sphere, Line3f line)
   {
      float x1 = line.p1.x;
      float y1 = line.p1.y;
      float z1 = line.p1.z;

      float x2 = line.p2.x;
      float y2 = line.p2.y;
      float z2 = line.p2.z;

      float x3 = sphere.origin.x;
      float y3 = sphere.origin.y;
      float z3 = sphere.origin.z;

      // diff between line-points
      float x21 = x2 - x1;
      float y21 = y2 - y1;
      float z21 = z2 - z1;

      // diff between first line-point and sphere origin
      float x13 = x1 - x3;
      float y13 = y1 - y3;
      float z13 = z1 - z3;

      // y = ax^2 + bx + c
      float a = x21 * x21 + y21 * y21 + z21 * z21;
      float b = 2.0f * ((x21 * x13) + (y21 * y13) + (z21 * z13));
      float c = (x3 * x3) + (y3 * y3) + (z3 * z3);
      c += (x1 * x1) + (y1 * y1) + (z1 * z1);
      c -= 2.0f * (x3 * x1 + y3 * y1 + z3 * z1);
      c -= sphere.radius * sphere.radius;

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

   public static float calcPathLength(Sphere3f sphere, Line3f line)
   {
      float[] result = new float[2];
      int intersections = calcIntersectionIntervals(sphere, line, result);
      if (intersections != 2)
         return 0.0f;

      Vector3f p1 = line.p1.lerp(result[0], line.p2);
      Vector3f p2 = line.p1.lerp(result[1], line.p2);
      return p1.distance(p2);
   }

   public static int calcIntersectionIntervals(Sphere3f sphere, Line3f line, float[] result)
   {
      float x1 = line.p1.x;
      float y1 = line.p1.y;
      float z1 = line.p1.z;

      float x2 = line.p2.x;
      float y2 = line.p2.y;
      float z2 = line.p2.z;

      float x3 = sphere.origin.x;
      float y3 = sphere.origin.y;
      float z3 = sphere.origin.z;

      // diff between line-points
      float x21 = x2 - x1;
      float y21 = y2 - y1;
      float z21 = z2 - z1;

      // diff between first line-point and sphere origin
      float x13 = x1 - x3;
      float y13 = y1 - y3;
      float z13 = z1 - z3;

      // y = ax^2 + bx + c
      float a = (x21 * x21) + (y21 * y21) + (z21 * z21);
      float b = 2.0f * ((x21 * x13) + (y21 * y13) + (z21 * z13));
      float c = (x3 * x3) + (y3 * y3) + (z3 * z3);
      c += (x1 * x1) + (y1 * y1) + (z1 * z1);
      c -= 2.0f * (x3 * x1 + y3 * y1 + z3 * z1);
      c -= sphere.radius * sphere.radius;

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
}
