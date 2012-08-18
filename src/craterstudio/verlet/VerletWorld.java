/*
 * Created on 8 jul 2008
 */

package craterstudio.verlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import craterstudio.math.Vec3;
import craterstudio.util.Bag;

public class VerletWorld
{
   private Bag<VerletBody>   bodies;
   private Bag<VerletPlane>  planes;
   private Bag<VerletSpring> springs;

   public VerletWorld()
   {
      this.bodies = new Bag<VerletBody>();
      this.planes = new Bag<VerletPlane>();
      this.springs = new Bag<VerletSpring>();
   }

   public void addPlane(VerletPlane plane)
   {
      planes.put(plane);
   }

   public void addBody(VerletBody body)
   {
      bodies.put(body);
   }

   //

   private float xGrav;
   private float yGrav;
   private float zGrav;

   public void setGravity(float x, float y, float z)
   {
      this.xGrav = x;
      this.yGrav = y;
      this.zGrav = z;
   }

   public void addForce(Vec3 vec)
   {
      this.addForce(vec.x, vec.y, vec.z);
   }

   public void addForce(float x, float y, float z)
   {
      for (int i = 0; i < bodies.size(); i++)
      {
         for (VerletSphere sphere : bodies.get(i).listSpheres())
         {
            sphere.getParticle().addForce(x, y, z);
         }
      }
   }

   //

   public VerletSpring createGlobalSpring(VerletSphere a, VerletSphere b, float stiffness, int how)
   {
      return this.createGlobalSpring(a.particle, b.particle, stiffness, how);
   }

   public VerletSpring createGlobalSpring(VerletParticle a, VerletParticle b, float stiffness, int how)
   {
      VerletSpring spring = new VerletSpring(a, b);
      spring.setCurrentDistanceAsLength();
      spring.stf = stiffness;
      spring.how = how;

      springs.put(spring);
      return spring;
   }

   public List<VerletBody> listBodies()
   {
      List<VerletBody> list = new ArrayList<VerletBody>();
      for (int i = 0; i < this.bodies.size(); i++)
         list.add(this.bodies.get(i));
      return list;
   }

   public List<VerletSpring> listGlobalSprings()
   {
      List<VerletSpring> list = new ArrayList<VerletSpring>();
      for (int i = 0; i < this.springs.size(); i++)
         list.add(this.springs.get(i));
      return list;
   }

   public void removeGlobalSpring(VerletSpring spring)
   {
      springs.take(spring);
   }

   private int[] createShuffledIndices(int len)
   {
      List<Integer> lst = new ArrayList<Integer>();
      for (int i = 0; i < len; i++)
         lst.add(Integer.valueOf(i));
      Collections.shuffle(lst);

      int[] arr = new int[len];
      for (int i = 0; i < len; i++)
         arr[i] = lst.get(i).intValue();
      return arr;
   }

   public void step(VerletFeedback feedback)
   {
      if (xGrav != 0.0f || yGrav != 0.0f || zGrav != 0.0f)
      {
         for (int i = 0; i < bodies.size(); i++)
         {
            for (VerletSphere sphere : bodies.get(i).listSpheres())
            {
               if (sphere.getParticle().invWeight != 0.0f)
               {
                  sphere.getParticle().addVelocity(xGrav, yGrav, zGrav);
               }
            }
         }
      }

      Bag<VerletBody> iaBodies = new Bag<VerletBody>();
      for (int i = 0; i < bodies.size(); i++)
         if (bodies.get(i).interacts)
            iaBodies.put(bodies.get(i));

      int[] iiBody = this.createShuffledIndices(iaBodies.size());
      int[] iBody = this.createShuffledIndices(bodies.size());
      int[] iPlane = this.createShuffledIndices(planes.size());
      int[] iSpring = this.createShuffledIndices(springs.size());

      // Update boundingSpheres of Bodies
      for (int i = 0; i < iaBodies.size(); i++)
      {
         iaBodies.get(i).updateBoundingSphere();
      }

      // Body <-> Body
      for (int i = 0; i < iaBodies.size(); i++)
      {
         VerletBody aBody = iaBodies.get(iiBody[i]);

         for (int k = i + 1; k < iaBodies.size(); k++)
         {
            VerletBody bBody = iaBodies.get(iiBody[k]);

            if (aBody.isPotentialHit(bBody))
            {
               aBody.collide(bBody, feedback);
            }
         }
      }

      // Body <-> Plane
      for (int k = 0; k < planes.size(); k++)
      {
         VerletPlane plane = planes.get(iPlane[k]);

         for (int i = 0; i < bodies.size(); i++)
         {
            VerletBody body = bodies.get(iBody[i]);

            if (body.isPotentialHit(plane))
            {
               body.collide(plane, feedback);
            }
         }
      }

      // step local Springs
      for (int i = 0; i < bodies.size(); i++)
      {
         bodies.get(iBody[i]).stepLocalSprings(feedback);
      }

      // step global Springs
      for (int k = 0; k < springs.size(); k++)
      {
         float tension = springs.get(iSpring[k]).tick();
         // if(tension > ...)
         {
            // break the spring?
         }
      }

      // Tick body / particles
      for (int i = 0; i < bodies.size(); i++)
      {
         VerletBody body = bodies.get(iBody[i]);

         body.tick();

         for (VerletSphere sphere : body.listSpheres())
         {
            sphere.particle.tick();

            // drag
            if (sphere.drag > 0.0f)
            {
               sphere.getParticle().mulVelocity(1.0f - sphere.drag);
            }
         }
      }
   }
}