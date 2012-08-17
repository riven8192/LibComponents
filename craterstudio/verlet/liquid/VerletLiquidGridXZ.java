/*
 * Created on Aug 1, 2009
 */

package craterstudio.verlet.liquid;

import craterstudio.math.EasyMath;
import craterstudio.math.Vec3;
import craterstudio.util.Bag;
import craterstudio.verlet.VerletSphere;

public class VerletLiquidGridXZ implements VerletLiquidGrid
{
   private final float xMin, xMax;
   private final float zMin, zMax;
   final float         size;
   private final int   w, d;

   private final Bag<VerletSphere>[][] cells, filled;
   final Bag<VerletSphere>             outer;

   VerletLiquidGridXZ(float xMin, float xMax, float zMin, float zMax, float size)
   {
      this.xMin = xMin;
      this.xMax = xMax;
      this.zMin = zMin;
      this.zMax = zMax;
      this.size = size;

      this.w = (int) Math.ceil((xMax - xMin) / size);
      this.d = (int) Math.ceil((zMax - zMin) / size);

      this.cells = new Bag[w][d];
      this.filled = new Bag[w][d];

      for (int z = 0; z < d; z++)
      {
         for (int x = 0; x < w; x++)
         {
            this.cells[x][z] = new Bag<VerletSphere>();
            this.filled[x][z] = new Bag<VerletSphere>();
         }
      }

      this.outer = new Bag<VerletSphere>();
   }

   @Override
   public float getCellSize()
   {
      return this.size;
   }

   @Override
   public Bag<VerletSphere> getOuterSpheres()
   {
      return this.outer;
   }

   public VerletLiquidGridXZ copy(float size)
   {
      return new VerletLiquidGridXZ(xMin, xMax, zMin, zMax, size);
   }

   public void shrink()
   {
      for (int z = 0; z < d; z++)
      {
         for (int x = 0; x < w; x++)
         {
            this.cells[x][z].shrink();
         }
      }

      this.outer.shrink();
   }

   public void clear()
   {
      for (int z = 0; z < d; z++)
      {
         for (int x = 0; x < w; x++)
         {
            this.cells[x][z].clear();
         }
      }

      this.outer.clear();
   }

   public void put(VerletSphere sphere)
   {
      this.lookupCell(sphere.particle.now).put(sphere);
   }

   public void queryBoundingBox(VerletSphere bounds, Bag<VerletSphere> fill)
   {
      Vec3 center = bounds.particle.now;

      Vec3 min = new Vec3(center).sub(bounds.radius);
      Vec3 max = new Vec3(center).add(bounds.radius);

      int neighbours = 1;
      int x1 = Math.round(EasyMath.invLerp(min.x, this.xMin, this.xMax) * this.w) - neighbours;
      int x2 = Math.round(EasyMath.invLerp(max.x, this.xMin, this.xMax) * this.w) + neighbours;
      int z1 = Math.round(EasyMath.invLerp(min.y, this.zMin, this.zMax) * this.d) - neighbours;
      int z2 = Math.round(EasyMath.invLerp(max.y, this.zMin, this.zMax) * this.d) + neighbours;

      this.fillFor(x1, x2, z1, z2, fill);
   }

   public void visit(VerletLiquidVisitor visitor)
   {
      for (int z = 0; z < this.d; z++)
      {
         for (int x = 0; x < this.w; x++)
         {
            if (this.cells[x][z].size() != 0)
            {
               visitor.visit(x, 0, z, this.cells[x][z], this.filled[x][z]);
            }
         }
      }
   }

   public int outerCount()
   {
      return this.outer.size();
   }

   public void findNeighbours()
   {
      for (int z = 0; z < this.d; z++)
      {
         for (int x = 0; x < this.w; x++)
         {
            this.filled[x][z].shrink();
            this.filled[x][z].clear();

            this.fillFor(x - 1, x + 1, z - 1, z + 1, this.filled[x][z]);
         }
      }
   }

   private void fillFor(int x1, int x2, int z1, int z2, Bag<VerletSphere> fill)
   {
      boolean putOuter = false;

      for (int iz = z1; iz <= z2; iz++)
      {
         for (int ix = x1; ix <= x2; ix++)
         {
            if ((ix | iz) < 0 || (ix >= this.w) || (iz >= this.d))
               putOuter = true;
            else
               fill.putAll(this.cells[ix][iz]);
         }
      }

      if (putOuter)
      {
         fill.putAll(this.outer);
      }
   }

   //

   private Bag<VerletSphere> lookupCell(Vec3 v)
   {
      return this.lookupCell(v.x, v.z);
   }

   private Bag<VerletSphere> lookupCell(float x, float z)
   {
      int ix = Math.round(EasyMath.invLerp(x, this.xMin, this.xMax) * this.w);
      int iz = Math.round(EasyMath.invLerp(z, this.zMin, this.zMax) * this.d);

      if ((ix | iz) < 0 || (ix >= this.w) || (iz >= this.d))
         return this.outer;
      return this.cells[ix][iz];
   }
}
