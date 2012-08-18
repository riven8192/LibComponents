/*
 * Created on 3-nov-2007
 */

package craterstudio.verlet;

import craterstudio.math.Vec3;
import craterstudio.math.VecMath;
import craterstudio.util.Bag;

public class VerletMath
{
   // plane <--> sphere

   public static final boolean collides(VerletPlane a, VerletSphere b)
   {
      float dx = b.particle.now.x - a.nx * a.d;
      float dy = b.particle.now.y - a.ny * a.d;
      float dz = b.particle.now.z - a.nz * a.d;

      return ((a.nx * dx) + (a.ny * dy) + (a.nz * dz) - b.radius) < 0.0f;
   }

   public static final float collide(VerletPlane a, VerletSphere b)
   {
      float bx = b.particle.now.x;
      float by = b.particle.now.y;
      float bz = b.particle.now.z;
      float bd = b.radius;

      float dx = bx - a.nx * a.d;
      float dy = by - a.ny * a.d;
      float dz = bz - a.nz * a.d;

      float dst = (a.nx * dx) + (a.ny * dy) + (a.nz * dz) - bd;
      if (dst >= 0.0f)
         return 0.0f;

      // impl true bounce, using speed
      // push out along normal of plane

      b.particle.now.x = bx - dst * a.nx;
      b.particle.now.y = by - dst * a.ny;
      b.particle.now.z = bz - dst * a.nz;

      return -dst;
   }

   // sphere <--> sphere

   public static final void collide(VerletSphere target, Bag<VerletSphere> all)
   {
      int size = all.size();

      for (int i = 0; i < size; i++)
      {
         VerletSphere sphere = all.get(i);

         if (VerletMath.collides(sphere, target))
         {
            VerletMath.collide(sphere, target);
         }
      }
   }

   public static final boolean collides(VerletSphere a, VerletSphere b)
   {
      return VecMath.isInRange(a.particle.now, b.particle.now, a.radius + b.radius);
   }

   public static final float collide(VerletSphere a, VerletSphere b)
   {
      // if both particles are locked in place, don't apply any forces
      if (a.particle.invWeight == 0.0f && b.particle.invWeight == 0.0f)
         return 0.0f;

      Vec3 anow = a.particle.now;
      Vec3 bnow = b.particle.now;

      float ax = anow.x;
      float ay = anow.y;
      float az = anow.z;
      float aiw = a.particle.invWeight;

      float bx = bnow.x;
      float by = bnow.y;
      float bz = bnow.z;
      float biw = b.particle.invWeight;

      float dx = bx - ax;
      float dy = by - ay;
      float dz = bz - az;
      float d2 = dx * dx + dy * dy + dz * dz;

      if (d2 <= ulp_zero)
      {
         // sharing position, oh oh!
         // big problem! if we collide
         // it, it will explode 
         return 0.0f;
      }

      float dMin = a.radius + b.radius;
      if (d2 > (dMin * dMin))
         return 0.0f;

      // apply spring -> push out of eachother

      //final float tension = 1.0f;
      float d = (float) Math.sqrt(d2);
      float f = (d - dMin) / dMin * 0.5f;//* tension;

      float f1 = f * aiw / (aiw + biw);
      anow.x = ax + dx * f1;
      anow.y = ay + dy * f1;
      anow.z = az + dz * f1;

      float f2 = f * biw / (aiw + biw);
      bnow.x = bx - dx * f2;
      bnow.y = by - dy * f2;
      bnow.z = bz - dz * f2;

      return (dMin - d);
   }

   private static final float ulp_zero = Math.ulp(0.0f);

   public static final void collideLiquid(VerletSphere a, VerletSphere b, float viscosity)
   {
      final float rad = a.radius; // 'b' is just as big...
      final Vec3 aNow = a.particle.now;
      final Vec3 bNow = b.particle.now;
      final Vec3 aOld = a.particle.old;
      final Vec3 bOld = b.particle.old;

      final float dx = bNow.x - aNow.x;
      final float dy = bNow.y - aNow.y;
      final float dz = bNow.z - aNow.z;
      final float d2 = dx * dx + dy * dy + dz * dz;

      final float outer = (rad * 3.0f);
      if (d2 > outer * outer)
      {
         return;
      }

      float force = viscosity / d2;

      final float radTwicePow = rad * rad * 4.00f;
      final float radHalfPow = rad * rad * 0.25f;

      if (d2 < radTwicePow)
      {
         // prevent extreme collisions
         local: if (d2 > radHalfPow)
         {
            float dMin = a.radius + b.radius;
            if (d2 > (dMin * dMin))
               break local;

            float d = (float) Math.sqrt(d2);
            float f = (d - dMin) / dMin * 0.25f;

            aNow.x += dx * f;
            aNow.y += dy * f;
            aNow.z += dz * f;

            bNow.x -= dx * f;
            bNow.y -= dy * f;
            bNow.z -= dz * f;
         }

         // this is a collision, flip the viscosity force (makes no sense, but it works)
         force *= -0.5f;
      }
      else
      {
         force *= 0.5f;
      }

      aOld.x -= dx * force;
      aOld.y -= dy * force;
      aOld.z -= dz * force;

      bOld.x += dx * force;
      bOld.y += dy * force;
      bOld.z += dz * force;
   }
}
