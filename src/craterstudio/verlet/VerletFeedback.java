/*
 * Created on 8 jul 2008
 */

package craterstudio.verlet;


public interface VerletFeedback
{
   public void springUpdate(VerletBody bodyOfSpring, VerletSpring spring, float tension);

   public void collision(VerletBody bodyOfSphere, VerletSphere sphere, VerletPlane plane, float depth);

   public void collision(VerletBody bodyOfSphere1, VerletSphere sphere1, VerletBody bodyOfSphere2, VerletSphere sphere2, float depth);

}