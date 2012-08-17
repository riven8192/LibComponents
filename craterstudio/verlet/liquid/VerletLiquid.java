/*
 * Created on Aug 2, 2009
 */

package craterstudio.verlet.liquid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import craterstudio.math.Vec3;
import craterstudio.misc.Prof;
import craterstudio.util.Bag;
import craterstudio.verlet.VerletBody;
import craterstudio.verlet.VerletBodyActor;
import craterstudio.verlet.VerletMath;
import craterstudio.verlet.VerletParticle;
import craterstudio.verlet.VerletPlane;
import craterstudio.verlet.VerletSphere;

public class VerletLiquid extends VerletBody implements VerletBodyActor
{
   private final float                dropRadius, dropWeight, viscosity;
   private final VerletLiquidAnalyzer analyzer;
   private final VerletLiquidWorker   worker;

   public VerletLiquid(float dropRadius, float dropWeight, float viscosity, VerletLiquidGrid grid)
   {
      this.dropRadius = dropRadius;
      this.dropWeight = dropWeight;
      this.viscosity = viscosity;

      this.interacts = false;
      this.interacting = new ArrayList<VerletBody>();

      this.attach(this);

      float cellSize = dropRadius * 8.1234f; // ?
      this.analyzer = new VerletLiquidAnalyzer(this, grid.copy(cellSize));
      this.worker = new VerletLiquidWorker(4);
   }

   public float getDropRadius()
   {
      return this.dropRadius;
   }

   public void addDrop(float x, float y, float z)
   {
      VerletParticle part = new VerletParticle();
      part.invWeight = 1.0f / this.dropWeight;
      part.translate(x, y, z);

      VerletSphere drop = new VerletSphere(part, this.dropRadius);
      this.addSphere(drop);
   }

   private final List<VerletBody> interacting;

   public void interact(VerletBody body)
   {
      this.interacting.add(body);
   }

   @Override
   public void act(VerletBody self)
   {
      if (this != self)
      {
         throw new IllegalStateException();
      }

      this.analyzer.tick();
   }

   public VerletLiquidVisitor createVisitor()
   {
      // TODO

      //VerletPlane p1 = VerletPlane.infer(new Vec3(0, 0, 0), new Vec3(0, 1, 0));
      final VerletPlane p2 = VerletPlane.infer(new Vec3(0, 0, 0), new Vec3(1, 0, 0));
      final VerletPlane p3 = VerletPlane.infer(new Vec3(1280, 0, 0), new Vec3(0, 0, 0));

      return new VerletLiquidVisitor()
      {
         @Override
         public void visit(int x, int y, int z, Bag<VerletSphere> local, Bag<VerletSphere> surround)
         {
            float viscosity = VerletLiquid.this.viscosity;

            for (int i = 0; i < local.size(); i++)
            {
               VerletSphere drop = local.get(i);

               if (VerletMath.collides(p2, drop))
                  VerletMath.collide(p2, drop);
               if (VerletMath.collides(p3, drop))
                  VerletMath.collide(p3, drop);

               final int size = surround.size();
               for (int k = 0; k < size; k++)
               {
                  VerletSphere other = surround.get(k);
                  if (other.hashCode() < drop.hashCode())
                  {
                     VerletMath.collideLiquid(drop, other, viscosity);
                  }
               }
            }
         }
      };
   }

   void tickImpl(VerletLiquidGrid liquidGrid)
   {
      List<VerletSphere> drops = this.listSpheres();

      Prof.start("liquid.grid.clear");
      liquidGrid.clear();
      Prof.stop("liquid.grid.clear");

      Prof.start("liquid.grid.put");
      for (int i = 0; i < drops.size(); i++)
         liquidGrid.put(drops.get(i));
      Prof.stop("liquid.grid.put");

      Prof.start("liquid.worker.collide");
      this.worker.collide(this, liquidGrid);
      Prof.stop("liquid.worker.collide");

      //

      Bag<VerletSphere> fill = new Bag<VerletSphere>();

      int c = 0;
      for (VerletBody body : this.interacting)
      {
         liquidGrid.queryBoundingBox(body.boundingSphere, fill);

         for (int i = 0; i < fill.size(); i++)
         {
            VerletSphere b = fill.get(i);

            for (VerletSphere a : body.listSpheres())
            {
               if (VerletMath.collides(a, b))
               {
                  VerletMath.collide(a, b);
                  c++;
               }
            }
         }

         fill.clear();
      }

      //

      Random r = new Random();

      // ground

      Prof.start("liquid.ground");
      VerletSphere ground = new VerletSphere(-1);
      ground.particle.setWeight(10.0f);

      for (int i = 0; i < 10; i++)
      {
         ground.radius = 150 + (i * 25);
         ground.particle.setPosition(200 + i * 133 - (i == 0 ? 150 : i == 1 ? 100 : 0), +200 + (i == 0 ? -250 : i == 1 ? 75 : 0) + i * 5, 0);

         liquidGrid.queryBoundingBox(ground, fill);
         VerletMath.collide(ground, fill);
         fill.clear();
      }
      Prof.stop("liquid.ground");

      // relocate outers as sprinklers...

      int size = liquidGrid.getOuterSpheres().size();
      for (int i = 0; i < size; i++)
      {
         VerletSphere drop = liquidGrid.getOuterSpheres().get(i);
         drop.getParticle().setPosition(1100 + r.nextFloat() * 128, 1200 + r.nextFloat() * 128, 0);
         drop.getParticle().setVelocity(0, 0, 0);
      }
   }
}