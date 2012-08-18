/*
 * Created on Aug 2, 2009
 */

package craterstudio.verlet.liquid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VerletLiquidAnalyzer
{
   private final VerletLiquid liquid;

   public VerletLiquidAnalyzer(VerletLiquid liquid, VerletLiquidGrid grid)
   {
      this.liquid = liquid;

      this.smallerGrid = grid;
      this.largerGrid = grid.copy(grid.getCellSize());

      this.size2grid = new HashMap<Float, VerletLiquidGrid>();
      this.lastSizes = new HashSet<Float>();
   }

   private boolean                            toggler;
   private long                               tSmaller;
   private long                               tLarger;

   private VerletLiquidGrid                   smallerGrid, largerGrid;
   private final Map<Float, VerletLiquidGrid> size2grid;
   private final Set<Float>                   lastSizes;

   public void tick()
   {
      VerletLiquidGrid xy = this.toggler ? smallerGrid : largerGrid;

      this.lastSizes.add(Float.valueOf(xy.getCellSize()));

      long t0 = System.nanoTime();
      this.liquid.tickImpl(xy);
      long t1 = System.nanoTime();
      long t = t1 - t0;

      if (toggler)
         tSmaller = t;
      else
         tLarger = t;

      if (this.lastSizes.size() == 4)
      {
         Set<Float> remove = new HashSet<Float>();
         remove.addAll(this.size2grid.keySet());
         remove.removeAll(this.lastSizes);
         this.lastSizes.clear();

         for (Float f : remove)
            this.size2grid.remove(f);
      }

      toggler ^= true;
      if (!toggler)
      {
         return;
      }

      final float adjustFactor = 1.10f;

      if (tSmaller < tLarger)
      {
         tLarger = tSmaller;
         largerGrid = smallerGrid;

         float newSize = smallerGrid.getCellSize() / adjustFactor;

         // the step size must be greater than the drop radius!!
         if (newSize < this.liquid.getDropRadius())
         {
            return;
         }

         VerletLiquidGrid smaller = this.size2grid.get(Float.valueOf(newSize));
         if (smaller == null)
         {
            smaller = smallerGrid.copy(newSize);
            this.size2grid.put(Float.valueOf(newSize), smaller);
         }
         smallerGrid = smaller;
      }
      else if (tSmaller > tLarger)
      {
         tSmaller = tLarger;
         smallerGrid = largerGrid;

         float newStep = smallerGrid.getCellSize() * adjustFactor;
         VerletLiquidGrid larger = this.size2grid.get(Float.valueOf(newStep));
         if (larger == null)
         {
            larger = largerGrid.copy(newStep);
            this.size2grid.put(Float.valueOf(newStep), larger);
         }
         largerGrid = larger;
      }
   }
}