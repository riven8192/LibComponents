/*
 * Created on 8 jul 2008
 */

package craterstudio.verlet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import craterstudio.math.Vec3;

public class VerletBody
{
   private static final Set<VerletSphere> usedSpheres;

   static
   {
      usedSpheres = new HashSet<VerletSphere>();
   }

   private final List<VerletSphere>       spheres;
   private final List<VerletSpring>       springs;
   public final VerletSphere              boundingSphere;

   public VerletBody()
   {
      this.spheres = new ArrayList<VerletSphere>();
      this.springs = new ArrayList<VerletSpring>();
      this.boundingSphere = new VerletSphere(new VerletParticle(), 0.0f);
   }

   public boolean                interacts = true;

   //

   private List<VerletBodyActor> actors;

   public void attach(VerletBodyActor actor)
   {
      if (this.actors == null)
         this.actors = new ArrayList<VerletBodyActor>();
      this.actors.add(actor);
   }

   public void detach(VerletBodyActor actor)
   {
      if (this.actors != null)
      {
         this.actors.remove(actor);
      }
   }

   //

   public void translate(Vec3 vec)
   {
      this.translate(vec.x, vec.y, vec.z);
   }

   public void translate(float x, float y, float z)
   {
      for (VerletSphere sphere : this.listSpheres())
      {
         sphere.getParticle().translate(x, y, z);
      }
   }

   public void setPosition(Vec3 vec)
   {
      this.setPosition(vec.x, vec.y, vec.z);
   }

   public void setPosition(float x, float y, float z)
   {
      Vec3 tmp = new Vec3();

      Vec3 center = new Vec3();
      int counter = 0;
      for (VerletSphere sphere : this.listSpheres())
      {
         sphere.getParticle().getPosition(tmp);
         center.add(tmp);
         counter++;
      }
      center.mul(1.0f / counter);

      tmp.load(x, y, z).sub(center);

      this.translate(tmp);
   }

   //

   public final void setVelocity(Vec3 vec)
   {
      this.setVelocity(vec.x, vec.y, vec.z);
   }

   public final void setVelocity(float x, float y, float z)
   {
      for (VerletSphere sphere : this.listSpheres())
      {
         sphere.getParticle().setVelocity(x, y, z);
      }
   }

   //

   public void setDrag(float drag)
   {
      for (VerletSphere sphere : this.listSpheres())
      {
         sphere.drag = drag;
      }
   }

   //

   public final void addForce(Vec3 vec)
   {
      this.addForce(vec.x, vec.y, vec.z);
   }

   public final void addForce(float x, float y, float z)
   {
      for (VerletSphere sphere : this.listSpheres())
      {
         sphere.getParticle().addForce(x, y, z);
      }
   }

   //

   public void addSphere(VerletSphere s)
   {
      if (!usedSpheres.add(s))
         throw new IllegalStateException("Sphere already used in a Body");
      this.spheres.add(s);
   }

   public List<VerletSphere> listSpheres()
   {
      return this.spheres;
      //return Collections.unmodifiableList(this.spheres);
   }

   public List<VerletSpring> listLocalSprings()
   {
      return this.springs;
      //return Collections.unmodifiableList(this.springs);
   }

   public void removeSphere(VerletSphere s)
   {
      if (!this.spheres.remove(s))
         throw new IllegalStateException("No such sphere in Body");
      usedSpheres.remove(s);
   }

   //

   public VerletSpring createLocalSpring(VerletSphere a, VerletSphere b, float stiffness, int how)
   {
      return this.createLocalSpring(a.particle, b.particle, stiffness, how);
   }

   public VerletSpring createLocalSpring(VerletParticle a, VerletParticle b, float stiffness, int how)
   {
      if (!this.hasParticle(a))
         throw new IllegalStateException();
      if (!this.hasParticle(b))
         throw new IllegalStateException();

      VerletSpring spring = new VerletSpring(a, b);
      spring.setCurrentDistanceAsLength();
      spring.stf = stiffness;
      spring.how = how;

      this.springs.add(spring);
      return spring;
   }

   public void removeLocalSpring(VerletSpring spring)
   {
      if (!this.springs.remove(spring))
         throw new NoSuchElementException();
   }

   void stepLocalSprings(VerletFeedback feedback)
   {
      for (int i = 0; i < springs.size(); i++)
      {
         float tension = springs.get(i).tick();
         feedback.springUpdate(this, springs.get(i), tension);
      }
   }

   //

   private boolean hasParticle(VerletParticle p)
   {
      for (int i = 0; i < this.spheres.size(); i++)
         if (spheres.get(i).particle.equals(p))
            return true;
      return false;
   }

   //

   public boolean isPotentialHit(VerletBody that)
   {
      return VerletMath.collides(that.boundingSphere, this.boundingSphere);
   }

   public boolean isPotentialHit(VerletPlane that)
   {
      return VerletMath.collides(that, this.boundingSphere);
   }

   //

   public void collide(VerletPlane that, VerletFeedback feedback)
   {
      for (VerletSphere s : this.spheres)
      {
         float depth = VerletMath.collide(that, s);
         if (depth == 0.0f)
            continue;

         feedback.collision(this, s, that, depth);
      }
   }

   public void collide(VerletBody that, VerletFeedback feedback)
   {
      if (this == that)
      {
         throw new IllegalArgumentException();
      }

      for (VerletSphere a : this.spheres)
      {
         for (VerletSphere b : that.spheres)
         {
            float depth = VerletMath.collide(a, b);
            if (depth == 0.0f)
               continue;

            feedback.collision(this, a, that, b, depth);
         }
      }
   }

   public void updateBoundingSphere()
   {
      if (spheres.isEmpty())
      {
         boundingSphere.particle.now.x = 0.0f;
         boundingSphere.particle.now.y = 0.0f;
         boundingSphere.particle.now.z = 0.0f;

         boundingSphere.radius = 0.0f;
         return;
      }

      float maxRadius = 0.0f;

      float minX = Integer.MAX_VALUE;
      float minY = Integer.MAX_VALUE;
      float minZ = Integer.MAX_VALUE;

      float maxX = Integer.MIN_VALUE;
      float maxY = Integer.MIN_VALUE;
      float maxZ = Integer.MIN_VALUE;

      for (VerletSphere s : spheres)
      {
         if (s.radius > maxRadius)
            maxRadius = s.radius;

         float px = s.particle.now.x;
         float py = s.particle.now.y;
         float pz = s.particle.now.z;

         if (px < minX)
            minX = px;
         if (py < minY)
            minY = py;
         if (pz < minZ)
            minZ = pz;

         if (px > maxX)
            maxX = px;
         if (py > maxY)
            maxY = py;
         if (pz > maxZ)
            maxZ = pz;
      }

      minX -= maxRadius;
      minY -= maxRadius;
      minZ -= maxRadius;

      maxX += maxRadius;
      maxY += maxRadius;
      maxZ += maxRadius;

      float dimX = maxX - minX;
      float dimY = maxY - minY;
      float dimZ = maxZ - minZ;

      float dim = Math.max(Math.max(dimX, dimY), dimZ);// * (float) Math.sqrt(3);

      boundingSphere.particle.now.x = (minX + maxX) * 0.5f;
      boundingSphere.particle.now.y = (minY + maxY) * 0.5f;
      boundingSphere.particle.now.z = (minZ + maxZ) * 0.5f;
      boundingSphere.radius = dim * 0.5f;
   }

   //

   public void tick()
   {
      if (this.actors != null)
      {
         for (int i = this.actors.size() - 1; i >= 0; i--)
         {
            this.actors.get(i).act(this);
         }
      }
   }
}
