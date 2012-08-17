/*
 * Created on 9-apr-2007
 */

package craterstudio.treecull;

import craterstudio.math.Plane;
import craterstudio.math.Sphere;

public class PlaneCuller implements OcttreeCuller
{
   private Plane plane;

   public PlaneCuller(Plane plane)
   {
      this.plane = plane;
   }

   public final void setPlane(Plane plane)
   {
      this.plane = plane;
   }

   public final Plane getPlane()
   {
      return plane;
   }

   public int feelIntersection(SpatiallyBound bound)
   {
      Sphere bounding = bound.getBoundingSphere();
      float signedDistance = plane.signedDistanceTo(bounding.origin);

      float above = signedDistance - bounding.radius;
      if (above >= 0.0f)
         return OcttreeCuller.FULLY_VISIBLE;

      float through = -above;
      if (through < bounding.radius)
         return OcttreeCuller.PARTIALLY_VISIBLE;

      return OcttreeCuller.NOT_VISIBLE;
   }
}
