/*
 * Created on Aug 1, 2009
 */

package craterstudio.verlet.liquid;

import craterstudio.math.EasyMath;
import craterstudio.math.Vec3;
import craterstudio.util.Bag;
import craterstudio.verlet.VerletSphere;

public class VerletLiquidGridXYZ implements VerletLiquidGrid
{
   private final float xMin, xMax;
   private final float yMin, yMax;
   private final float zMin, zMax;
   final float         size;
   private final int   w, h, d;

   private final Bag<VerletSphere>[][][] cells, filled;
   final Bag<VerletSphere>               outer;

   VerletLiquidGridXYZ(float xMin, float xMax, float yMin, float yMax, float zMin, float zMax, float size)
   {
      this.xMin = xMin;
      this.xMax = xMax;
      this.yMin = yMin;
      this.yMax = yMax;
      this.zMin = zMin;
      this.zMax = zMax;
      this.size = size;

      this.w = (int) Math.ceil((xMax - xMin) / size);
      this.h = (int) Math.ceil((yMax - yMin) / size);
      this.d = (int) Math.ceil((zMax - zMin) / size);

      this.cells = new Bag[w][h][d];
      this.filled = new Bag[w][h][d];

      for (int z = 0; z < d; z++)
      {
         for (int y = 0; y < h; y++)
         {
            for (int x = 0; x < w; x++)
            {
               this.cells[x][y][z] = new Bag<VerletSphere>();
               this.filled[x][y][z] = new Bag<VerletSphere>();
            }
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

   public VerletLiquidGridXYZ copy(float size)
   {
      return new VerletLiquidGridXYZ(xMin, xMax, yMin, yMax, zMin, zMax, size);
   }

   public void clear()
   {
      for (int z = 0; z < d; z++)
      {
         for (int y = 0; y < h; y++)
         {
            for (int x = 0; x < w; x++)
            {
               this.cells[x][y][z].clear();
            }
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
      int y1 = Math.round(EasyMath.invLerp(min.y, this.yMin, this.yMax) * this.h) - neighbours;
      int y2 = Math.round(EasyMath.invLerp(max.y, this.yMin, this.yMax) * this.h) + neighbours;
      int z1 = Math.round(EasyMath.invLerp(min.z, this.zMin, this.zMax) * this.d) - neighbours;
      int z2 = Math.round(EasyMath.invLerp(max.z, this.zMin, this.zMax) * this.d) + neighbours;

      this.fillFor(x1, x2, y1, y2, z1, z2, fill);
   }

   public void visit(VerletLiquidVisitor visitor)
   {
      for (int z = 0; z < this.d; z++)
      {
         for (int y = 0; y < this.h; y++)
         {
            for (int x = 0; x < this.w; x++)
            {
               if (this.cells[x][y][z].size() != 0)
               {
                  visitor.visit(x, y, z, this.cells[x][y][z], this.filled[x][y][z]);
               }
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
            for (int y = 0; y < this.h; y++)
            {
               this.filled[x][y][z].shrink();
               this.filled[x][y][z].clear();

               this.fillFor(x - 1, x + 1, y - 1, y + 1, z - 1, z + 1, this.filled[x][y][z]);
            }
         }
      }
   }

   private void fillFor(int x1, int x2, int y1, int y2, int z1, int z2, Bag<VerletSphere> fill)
   {
      boolean putOuter = false;

      for (int iz = z1; iz <= z2; iz++)
      {
         for (int iy = y1; iy <= y2; iy++)
         {
            for (int ix = x1; ix <= x2; ix++)
            {
               if ((ix | iy | iz) < 0 || (ix >= this.w) || (iy >= this.h) || (iz >= this.d))
                  putOuter = true;
               else
                  fill.putAll(this.cells[ix][iy][iz]);
            }
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
      return this.lookupCell(v.x, v.y, v.z);
   }

   private Bag<VerletSphere> lookupCell(float x, float y, float z)
   {
      int ix = Math.round(EasyMath.invLerp(x, this.xMin, this.xMax) * this.w);
      int iy = Math.round(EasyMath.invLerp(y, this.yMin, this.yMax) * this.h);
      int iz = Math.round(EasyMath.invLerp(z, this.zMin, this.zMax) * this.d);

      if ((ix | iy | iz) < 0 || (ix >= this.w) || (iy >= this.h) || (iz >= this.d))
         return this.outer;
      return this.cells[ix][iy][iz];
   }
}
