/*
 * Created on Aug 1, 2009
 */

package craterstudio.verlet.liquid;

import craterstudio.math.EasyMath;
import craterstudio.math.Vec3;
import craterstudio.util.Bag;
import craterstudio.verlet.VerletSphere;

public class VerletLiquidGridXY implements VerletLiquidGrid
{
   private final float xMin, xMax, xInvDiffMulW;
   private final float yMin, yMax, yInvDiffMulH;
   final float         size;
   private final int   w, h;

   private final Bag<VerletSphere>[][] cells, filled;
   final Bag<VerletSphere>             outer;

   VerletLiquidGridXY(float xMin, float xMax, float yMin, float yMax, float size)
   {
      this.xMin = xMin;
      this.xMax = xMax;
      this.yMin = yMin;
      this.yMax = yMax;
      this.size = size;

      this.w = (int) Math.ceil((xMax - xMin) / size);
      this.h = (int) Math.ceil((yMax - yMin) / size);

      this.xInvDiffMulW = this.w / (this.xMax - this.xMin);
      this.yInvDiffMulH = this.h / (this.yMax - this.yMin);

      this.cells = new Bag[w][h];
      this.filled = new Bag[w][h];

      for (int y = 0; y < h; y++)
      {
         for (int x = 0; x < w; x++)
         {
            this.cells[x][y] = new Bag<VerletSphere>();
            this.filled[x][y] = new Bag<VerletSphere>();
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

   public VerletLiquidGridXY copy(float size)
   {
      return new VerletLiquidGridXY(xMin, xMax, yMin, yMax, size);
   }

   public void clear()
   {
      for (int y = 0; y < h; y++)
      {
         for (int x = 0; x < w; x++)
         {
            this.cells[x][y].shrink();
            this.cells[x][y].clear();
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

      this.fillFor(x1, x2, y1, y2, fill);
   }

   public void visit(VerletLiquidVisitor visitor)
   {
      Bag<VerletSphere> near = new Bag<VerletSphere>();

      for (int y = 0; y < this.h; y++)
      {
         int yL = y == 0 ? 0 : y - 1;
         int yH = y == h - 1 ? h - 1 : y + 1;

         for (int x = 0; x < this.w; x++)
         {
            if (this.cells[x][y].size() == 0)
            {
               continue;
            }

            int xL = x == 0 ? 0 : x - 1;
            int xH = x == w - 1 ? w - 1 : x + 1;

            if ((x == 0) || (y == 0) || (x == this.w - 1) || (y == this.h - 1))
               near.putAll(this.outer);
            for (int yi = yL; yi <= yH; yi++)
               for (int xi = xL; xi <= xH; xi++)
                  near.putAll(this.cells[xi][yi]);

            Bag<VerletSphere> copy = new Bag<VerletSphere>(near.size());
            copy.putAll(near);
            near.clear();

            visitor.visit(x, y, 0, this.cells[x][y], copy);
         }
      }
   }

   public int outerCount()
   {
      return this.outer.size();
   }

   private void fillFor(int x1, int x2, int y1, int y2, Bag<VerletSphere> fill)
   {
      boolean hitsOuter = ((x1 | y1) < 0) || (x2 >= this.w) || (y2 >= this.h);

      if (hitsOuter)
      {
         if (x1 < 0)
            x1 = 0;
         if (y1 < 0)
            y1 = 0;

         if (x2 >= w)
            x2 = w - 1;
         if (y2 >= h)
            y2 = h - 1;
      }

      for (int iy = y1; iy <= y2; iy++)
      {
         for (int ix = x1; ix <= x2; ix++)
         {
            fill.putAll(this.cells[ix][iy]);
         }
      }

      if (hitsOuter)
      {
         fill.putAll(this.outer);
      }
   }

   //

   private Bag<VerletSphere> lookupCell(Vec3 v)
   {
      return this.lookupCell(v.x, v.y);
   }

   private Bag<VerletSphere> lookupCell(float x, float y)
   {
      int ix = (int) ((x - this.xMin) * this.xInvDiffMulW);
      int iy = (int) ((y - this.yMin) * this.yInvDiffMulH);
      if ((ix | iy) < 0 || (ix >= this.w) || (iy >= this.h))
         return this.outer;
      return this.cells[ix][iy];
   }
}
