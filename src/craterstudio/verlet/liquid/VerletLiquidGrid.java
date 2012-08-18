/*
 * Created on Aug 1, 2009
 */

package craterstudio.verlet.liquid;

import craterstudio.util.Bag;
import craterstudio.verlet.VerletSphere;

public interface VerletLiquidGrid
{
   public float getCellSize();

   public VerletLiquidGrid copy(float size);

   public void clear();

   public void put(VerletSphere sphere);

   public void queryBoundingBox(VerletSphere bounds, Bag<VerletSphere> fill);

   public void visit(VerletLiquidVisitor visitor);

   public int outerCount();

   public Bag<VerletSphere> getOuterSpheres();
}
