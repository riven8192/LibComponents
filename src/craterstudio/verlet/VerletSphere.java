/*
 * Created on 3-nov-2007
 */

package craterstudio.verlet;

public class VerletSphere
{
   public final VerletParticle particle;
   public float                radius;
   public float                invFriction = 0.1f;
   public float                drag        = 0.0f;

   public VerletSphere(float rad)
   {
      this(new VerletParticle(), rad);
   }

   public VerletSphere(VerletParticle p, float rad)
   {
      this.particle = p;
      this.radius = rad;
   }

   public void setFriction(float friction)
   {
      this.invFriction = 1.0f - friction;
   }

   public VerletParticle getParticle()
   {
      return particle;
   }

   @Override
   public String toString()
   {
      return "Sphere[" + this.particle + ", " + this.radius + "]";
   }
}