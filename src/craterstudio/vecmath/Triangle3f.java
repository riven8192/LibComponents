/*
 * Created on 8 jul 2008
 */

package craterstudio.vecmath;

import static craterstudio.vecmath.Vector3f.*;

public class Triangle3f
{
   public final Vector3f p1, p2, p3;

   public Triangle3f()
   {
      this.p1 = new Vector3f();
      this.p2 = new Vector3f();
      this.p3 = new Vector3f();
   }

   public Triangle3f(Vector3f a, Vector3f b, Vector3f c)
   {
      this.p1 = a;
      this.p2 = b;
      this.p3 = c;
   }

   public Vector3f normal()
   {
      return cross(sub(p2, p1), sub(p3, p1)).normalize();
   }

   public Plane3f plane()
   {
      Vector3f n = this.normal();
      return new Plane3f(n, dot(n, this.p1));
   }

   public Vector3f calcClosestPoint(Vector3f p)
   {
      Vector3f ba = sub(p2, p1);
      Vector3f ca = sub(p3, p1);
      Vector3f pa = sub(p, p1);

      float snom = dot(pa, ba);
      float tnom = dot(pa, ca);

      if (snom <= 0.0f && tnom <= 0.0f)
         return new Vector3f(p1);

      Vector3f cb = sub(p3, p2);
      Vector3f pb = sub(p, p2);

      float unom = dot(pb, cb);
      float sdenom = dot(pb, sub(p1, p2));

      if (sdenom <= 0.0f && unom <= 0.0f)
         return new Vector3f(p2);

      Vector3f pc = sub(p, p3);

      float tdenom = dot(pc, sub(p1, p3));
      float udenom = dot(pc, sub(p2, p3));

      if (tdenom <= 0.0f && udenom <= 0.0f)
         return new Vector3f(p3);

      Vector3f n = cross(ba, ca);

      //

      Vector3f ap_bp = cross(sub(p1, p), sub(p2, p));
      float vc = dot(n, ap_bp);

      if (vc <= 0.0f && snom >= 0.0f && sdenom >= 0.0f)
      {
         return add(p1, ba.mul(snom / (snom + sdenom)));
      }

      //

      Vector3f bp_cp = cross(sub(p2, p), sub(p3, p));
      float va = dot(n, bp_cp);

      if (va <= 0.0f && unom >= 0.0f && udenom >= 0.0f)
      {
         return add(p2, ba.mul(unom / (unom + udenom)));
      }

      //

      Vector3f cp_ap = cross(sub(p3, p), sub(p1, p));
      float vb = dot(n, cp_ap);

      if (vb <= 0.0f && tnom >= 0.0f && tdenom >= 0.0f)
      {
         return add(p1, ca.mul(tnom / (tnom + tdenom)));
      }

      float u = va / (va + vb + vc);
      float v = vb / (va + vb + vc);
      float w = 1.0f - u - v;

      // r = (u * a) + (v * b) + (w * c)
      Vector3f r = new Vector3f();
      r.x = (p1.x * u) + (p2.x * v) + (p3.x * w);
      r.y = (p1.y * u) + (p2.y * v) + (p3.y * w);
      r.z = (p1.z * u) + (p2.z * v) + (p3.z * w);
      return r;
   }

   public boolean pushOut(Sphere3f s, float factor, boolean alongFaceNormal)
   {
      Vector3f closest = this.calcClosestPoint(s.origin);

      Vector3f diff = sub(closest, new Vector3f(s.origin));

      float squaredDist = dot(diff, diff);
      float squaredRadius = s.radius * s.radius;
      if (squaredDist >= squaredRadius)
         return false;

      float intersection = s.radius - (float) Math.sqrt(squaredDist);

      if (alongFaceNormal)
         Vector3f.add(s.origin, this.normal().mul(intersection * factor));
      else
         Vector3f.add(s.origin, diff.normalize().mul(intersection * factor));

      return true;
   }
}
