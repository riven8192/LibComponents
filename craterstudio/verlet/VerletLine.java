/*
 * Created on Aug 8, 2009
 */

package craterstudio.verlet;

import craterstudio.math.EasyMath;
import craterstudio.math.Matrix4;
import craterstudio.math.Vec3;
import craterstudio.math.VecMath;

public class VerletLine
{
   private final VerletSphere[] many;
   private final VerletSphere   bound;
   private final VerletPlane    plane1, plane2;

   public VerletLine(Vec3 a, Vec3 b, float radius)
   {
      // a line is a plane with a series of spheres
      // that define the bounding area

      float d = VecMath.distance(a, b);

      this.bound = new VerletSphere(d * 0.5f);
      this.bound.particle.setPosition(VecMath.lerp(0.5f, a, b));

      // perpendicular
      Vec3 p1 = new Vec3(a.x, 0.0f, a.y);
      Vec3 p2 = new Vec3(b.x, 0.0f, b.y);
      float angle = VecMath.angleToFlat3D(p1, p2);
      Matrix4 mat = new Matrix4();
      mat.translate(p1);
      mat.rotY(-angle - 90.0f);

      Vec3 p3a = mat.transform(new Vec3(+d, 0, 0));
      Vec3 p3b = mat.transform(new Vec3(-d, 0, 0));
      p3a.load(p3a.x, p3a.z, 0.0f);
      p3b.load(p3b.x, p3b.z, 0.0f);

      this.plane1 = new VerletPlane();
      this.plane1.inferValues(a, p3a);

      this.plane2 = new VerletPlane();
      this.plane2.inferValues(a, p3b);
      
      // add backside

      this.many = new VerletSphere[(int) Math.ceil(d / radius)];
      for (int i = 0; i < this.many.length; i++)
      {
         this.many[i] = new VerletSphere(radius);
         float at = EasyMath.lerp((float) i / (this.many.length - 1), radius, d - radius) / d;
         this.many[i].particle.setPosition(VecMath.lerp(at, a, b));
      }
   }

   public boolean collides(VerletSphere sphere)
   {
      if (!VerletMath.collides(this.bound, sphere))
      {
         return false;
      }

      for (int i = 0; i < this.many.length; i++)
      {
         if (VerletMath.collides(this.many[i], sphere))
         {
            return true;
         }
      }

      return false;
   }

   public void collide(VerletSphere sphere)
   {
      VerletMath.collide(this.plane1, sphere);
   }
}
