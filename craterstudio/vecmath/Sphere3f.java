/*
 * Created on 8 jul 2008
 */

package craterstudio.vecmath;

public class Sphere3f
{
   public final Vector3f origin;
   public float          radius;

   public Sphere3f(Vector3f origin, float radius)
   {
      this.origin = origin;
      this.radius = radius;
   }

   public boolean isInRange(Sphere3f s, float d)
   {
      return this.origin.isInRange(s.origin, this.radius + s.radius + d);
   }

   public boolean isInRange(Vector3f p, float d)
   {
      return this.origin.isInRange(p, this.radius + d);
   }

}