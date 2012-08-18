/*
 * Created on 9-apr-2007
 */

package craterstudio.treecull;


import craterstudio.math.FastMath;
import craterstudio.math.Matrix4;
import craterstudio.math.Plane;
import craterstudio.math.Vec3;

public class PerspectiveCuller implements OcttreeCuller
{
   public final void update(float fov, float ratio, Vec3 position, Vec3 orientation)
   {
      Matrix4 mat = new Matrix4();

      float vScope = FastMath.sinDeg(fov * 0.5f * 1.25f);
      float hScope = vScope * ratio;

      Vec3 leftNormal = new Vec3(+1, 0, +hScope).normalize();
      Vec3 rightNormal = new Vec3(-1, 0, +hScope).normalize();
      Vec3 upNormal = new Vec3(0, +1, +vScope).normalize();
      Vec3 downNormal = new Vec3(0, -1, +vScope).normalize();

      mat.identity();
      mat.rotY(-orientation.y);
      mat.rotX(-orientation.x);

      mat.transform(leftNormal);
      mat.transform(rightNormal);
      mat.transform(upNormal);
      mat.transform(downNormal);

      leftNormal.inv();
      rightNormal.inv();
      upNormal.inv();
      downNormal.inv();

      culler = new AndOcttreeCuller();
      culler.add(new PlaneCuller(new Plane(position, leftNormal)));
      culler.add(new PlaneCuller(new Plane(position, rightNormal)));
      culler.add(new PlaneCuller(new Plane(position, upNormal)));
      culler.add(new PlaneCuller(new Plane(position, downNormal)));
   }

   private AndOcttreeCuller culler;

   public int feelIntersection(SpatiallyBound bound)
   {
      return culler.feelIntersection(bound);
   }
}