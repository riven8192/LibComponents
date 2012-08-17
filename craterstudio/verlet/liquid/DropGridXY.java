/*
 * Created on Aug 1, 2009
 */

package craterstudio.verlet.liquid;

import craterstudio.util.Bag;

public class DropGridXY
{
   private final float         xMin, xMax, xInvDiffMulW;
   private final float         yMin, yMax, yInvDiffMulH;
   final float                 size;
   private final int           w, h;

   private final Bag<Drop>[][] cells;
   final Bag<Drop>             outer;

   DropGridXY(float xMin, float xMax, float yMin, float yMax, float size)
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

      for (int y = 0; y < h; y++)
      {
         for (int x = 0; x < w; x++)
         {
            this.cells[x][y] = new Bag<Drop>();
         }
      }

      this.outer = new Bag<Drop>();
   }

   public float getCellSize()
   {
      return this.size;
   }

   public Bag<Drop> getOuterDrops()
   {
      return this.outer;
   }

   public DropGridXY copy(float size)
   {
      return new DropGridXY(xMin, xMax, yMin, yMax, size);
   }

   public void clear()
   {
      for (int y = 0; y < h; y++)
      {
         for (int x = 0; x < w; x++)
         {
            this.cells[x][y].clear();
         }
      }

      this.outer.clear();
   }

   public void put(Drop drop)
   {
      this.lookupCell(drop.xNow, drop.yNow).put(drop);
   }

   public void visit(DropVisitor visitor)
   {
      Bag<Drop> near = new Bag<Drop>();

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

            visitor.visit(x, y, 0, this.cells[x][y], near);
            near.clear();
         }
      }
   }

   public int outerCount()
   {
      return this.outer.size();
   }

   private Bag<Drop> lookupCell(float x, float y)
   {
      int ix = (int) ((x - this.xMin) * this.xInvDiffMulW);
      int iy = (int) ((y - this.yMin) * this.yInvDiffMulH);
      if ((ix | iy) < 0 || (ix >= this.w) || (iy >= this.h))
         return this.outer;
      return this.cells[ix][iy];
   }
}