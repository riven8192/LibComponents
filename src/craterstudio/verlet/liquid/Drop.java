/*
 * Created on Aug 15, 2009
 */

package craterstudio.verlet.liquid;

import craterstudio.math.Vec3;
import craterstudio.verlet.VerletSphere;

public class Drop
{
   private static int next_id = 1;

   public final int   id;

   public Drop()
   {
      this.id = next_id++;
   }

   public float xNow, yNow, zNow;
   public float xOld, yOld, zOld;

   public final void tick()
   {
      float xDiff = xNow - xOld;
      float yDiff = yNow - yOld;
      float zDiff = zNow - zOld;

      xOld = xNow;
      yOld = yNow;
      zOld = zNow;

      xNow += xDiff;
      yNow += yDiff;
      zNow += zDiff;
   }

   public final void set(float x, float y, float z)
   {
      xNow = xOld = x;
      yNow = yOld = y;
      zNow = zOld = z;
   }

   public final void addForce(float x, float y, float z)
   {
      xOld -= x;
      yOld -= y;
      zOld -= z;
   }

   public final void friction(float factor)
   {
      xOld = xNow - (xNow - xOld) * factor;
      yOld = yNow - (yNow - yOld) * factor;
      zOld = zNow - (zNow - zOld) * factor;
   }

   public static final void collideLiquid(Drop a, Drop b, float radius, float viscosity)
   {
      final float dx = b.xNow - a.xNow;
      final float dy = b.yNow - a.yNow;
      final float dz = b.zNow - a.zNow;
      final float d2 = dx * dx + dy * dy + dz * dz;

      final float outer = (radius * 3.0f);
      if (d2 > outer * outer)
      {
         return;
      }

      float force = viscosity / d2;

      final float rad2_doub = radius * radius * 4.00f;
      final float rad2_half = radius * radius * 0.25f;

      if (d2 < rad2_doub)
      {
         // prevent extreme collisions
         local: if (d2 > rad2_half)
         {
            float dMin = radius + radius;
            if (d2 > (dMin * dMin))
               break local;

            float d = (float) Math.sqrt(d2);
            float f = (d - dMin) / dMin * 0.25f;

            a.xNow += dx * f;
            a.yNow += dy * f;
            a.zNow += dz * f;

            b.xNow -= dx * f;
            b.yNow -= dy * f;
            b.zNow -= dz * f;
         }

         // this is a collision, flip the viscosity force (makes no sense, but it works)
         force *= -0.5f;
      }
      else
      {
         force *= 0.5f;
      }

      a.xOld -= dx * force;
      a.yOld -= dy * force;
      a.zOld -= dz * force;

      b.xOld += dx * force;
      b.yOld += dy * force;
      b.zOld += dz * force;
   }
}
