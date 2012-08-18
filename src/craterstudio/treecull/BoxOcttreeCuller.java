/*
 * Created on 16-jul-2007
 */

package craterstudio.treecull;


import craterstudio.math.Plane;
import craterstudio.math.Vec3;

public class BoxOcttreeCuller implements OcttreeCuller
{
   public final void update(Vec3 min, Vec3 max)
   {
      culler = new AndOcttreeCuller();
      culler.add(new PlaneCuller(new Plane(min, new Vec3(+1, 0, 0))));
      culler.add(new PlaneCuller(new Plane(min, new Vec3(0, +1, 0))));
      culler.add(new PlaneCuller(new Plane(min, new Vec3(0, 0, +1))));
      culler.add(new PlaneCuller(new Plane(max, new Vec3(-1, 0, 0))));
      culler.add(new PlaneCuller(new Plane(max, new Vec3(0, -1, 0))));
      culler.add(new PlaneCuller(new Plane(max, new Vec3(0, 0, -1))));
   }

   private AndOcttreeCuller culler;

   public int feelIntersection(SpatiallyBound bound)
   {
      return culler.feelIntersection(bound);
   }
}
