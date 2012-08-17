/*
 * Created on 28 jul 2009
 */

package craterstudio.verlet;

import craterstudio.math.EasyMath;

public class AnimatingVerletSpringActor implements VerletSpringActor
{
   final long                    t1, t2;
   final Animation<VerletSpring> anim;

   public AnimatingVerletSpringActor(long t1, long t2, Animation<VerletSpring> anim)
   {
      this.t1 = t1;
      this.t2 = t2;
      this.anim = anim;
   }

   int status;

   @Override
   public void act(VerletSpring spring)
   {
      float t = EasyMath.invLerp(System.currentTimeMillis(), t1, t2);

      switch (this.status)
      {
         case 0:
            if (t < 0.0f)
               return; // wait for start

            this.anim.begin(spring);
            this.status += 1;
            break;

         case 1:
            if (t <= 1.0f)
               this.anim.step(spring, t);
            else
               this.status += 1;
            break;

         case 2:
            this.anim.end(spring);
            spring.detach(this);
            this.status += 1;
            break;

         default:
            throw new IllegalStateException();
      }
   }
}
