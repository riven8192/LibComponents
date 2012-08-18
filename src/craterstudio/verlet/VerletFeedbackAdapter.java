/*
 * Created on 4 mei 2011
 */

package craterstudio.verlet;

public class VerletFeedbackAdapter implements VerletFeedback
{
   @Override
   public void springUpdate(VerletBody bodyOfSpring, VerletSpring spring, float tension)
   {
      // 
   }

   @Override
   public void collision(VerletBody bodyOfSphere1, VerletSphere sphere1, VerletBody bodyOfSphere2, VerletSphere sphere2, float depth)
   {
      //
   }

   @Override
   public void collision(VerletBody bodyOfSphere, VerletSphere sphere, VerletPlane plane, float depth)
   {
      //
   }
}
