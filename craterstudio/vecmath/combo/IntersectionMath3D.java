/*
 * Created on Oct 10, 2009
 */

package craterstudio.vecmath.combo;

import static craterstudio.vecmath.Vector3f.add;
import static craterstudio.vecmath.Vector3f.dot;
import static craterstudio.vecmath.Vector3f.sub;
import craterstudio.vecmath.Line3f;
import craterstudio.vecmath.Plane3f;
import craterstudio.vecmath.Ray3f;
import craterstudio.vecmath.Sphere3f;
import craterstudio.vecmath.Triangle3f;
import craterstudio.vecmath.Vector3f;

public class IntersectionMath3D
{
   public static boolean intersects(Sphere3f a, float d, Sphere3f b)
   {
      return a.origin.isInRange(b.origin, a.radius + d + b.radius);
   }

   public static boolean intersects(Sphere3f sphere, Line3f line)
   {
      Vector3f diff = sub(line.p2, line.p1);
      Vector3f normal = diff.normalize();
      float t = IntersectionMath3D.intersection(sphere, new Ray3f(line.p1, normal));
      return (t > 0.0f && t < diff.length());
   }

   public static boolean intersects(Sphere3f sphere, Ray3f ray)
   {
      Vector3f dst = sub(ray.origin, sphere.origin);
      float B = dot(dst, ray.normal);
      float C = dot(dst, dst) - (sphere.radius * sphere.radius);
      float D = B * B - C;
      return D > 0.0f;
   }

   public static float intersection(Sphere3f sphere, Ray3f ray)
   {
      Vector3f dst = sub(ray.origin, sphere.origin);
      float B = dot(dst, ray.normal);
      float C = dot(dst, dst) - (sphere.radius * sphere.radius);
      float D = B * B - C;
      return (D > 0.0f) ? (-B - (float) Math.sqrt(D)) : Float.POSITIVE_INFINITY;
   }

   public static boolean intersectFast(Sphere3f sphere, Ray3f ray)
   {
      float dx = ray.origin.x - sphere.origin.x;
      float dy = ray.origin.y - sphere.origin.y;
      float dz = ray.origin.z - sphere.origin.z;

      float B = (dx * ray.normal.x) + (dy * ray.normal.y) + (dz * ray.normal.z);
      float C = (dx * dx) + (dy * dy) + (dz * dz) - (sphere.radius * sphere.radius);
      float D = B * B - C;
      return (D > 0.0);
   }

   public static float intersectionFast(Sphere3f sphere, Ray3f ray)
   {
      float dx = ray.origin.x - sphere.origin.x;
      float dy = ray.origin.y - sphere.origin.y;
      float dz = ray.origin.z - sphere.origin.z;

      float B = (dx * ray.normal.x) + (dy * ray.normal.y) + (dz * ray.normal.z);
      float C = (dx * dx) + (dy * dy) + (dz * dz) - (sphere.radius * sphere.radius);
      float D = B * B - C;
      return (D > 0.0) ? (-B - (float) Math.sqrt(D)) : Float.POSITIVE_INFINITY;
   }

   public static boolean intersects(Triangle3f tri, Ray3f ray)
   {
      Triangle3f triA = new Triangle3f(ray.origin, tri.p1, tri.p2);
      Triangle3f triB = new Triangle3f(ray.origin, tri.p2, tri.p3);
      Triangle3f triC = new Triangle3f(ray.origin, tri.p3, tri.p1);

      Vector3f p = IntersectionMath3D.findIntersection(tri.plane(), ray);
      boolean aAbove = triA.plane().isAbove(p);

      boolean bAbove = triB.plane().isAbove(p);
      if (aAbove != bAbove)
         return false;

      boolean cAbove = triC.plane().isAbove(p);
      if (aAbove != cAbove)
         return false;

      return true;
   }

   public static Vector3f findIntersection(Plane3f plane, Ray3f ray)
   {
      Vector3f origin = plane.normal.length(plane.distance);

      Vector3f diff = sub(origin, ray.origin);

      float dot1 = dot(plane.normal, diff);
      float dot2 = dot(plane.normal, ray.normal);

      diff.load(ray.normal).mul(dot1 / dot2);

      return add(ray.origin, diff);
   }
}