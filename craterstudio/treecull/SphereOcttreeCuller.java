/*
 * Created on 9-apr-2007
 */

package craterstudio.treecull;

import craterstudio.math.Sphere;
import craterstudio.math.VecMath;

public class SphereOcttreeCuller implements OcttreeCuller
{
   private final Sphere sphere;

   public SphereOcttreeCuller()
   {
      this.sphere = new Sphere();
   }

   public final void setSphere(Sphere sphere)
   {
      this.sphere.load(sphere);
   }

   public int feelIntersection(SpatiallyBound bound)
   {
      Sphere other = bound.getBoundingSphere();
      float dist2 = VecMath.squaredDistance(other.origin, this.sphere.origin);

      float maxDist;

      maxDist = this.sphere.radius - other.radius;
      if (maxDist > 0.0f && dist2 < maxDist * maxDist)
         return FULLY_VISIBLE;

      maxDist = this.sphere.radius + other.radius;
      if (dist2 < maxDist * maxDist)
         return PARTIALLY_VISIBLE;

      return NOT_VISIBLE;
   }
}
