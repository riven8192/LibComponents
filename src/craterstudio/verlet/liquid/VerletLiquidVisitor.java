/*
 * Created on Aug 8, 2009
 */

package craterstudio.verlet.liquid;

import craterstudio.util.Bag;
import craterstudio.verlet.VerletSphere;

public interface VerletLiquidVisitor
{
   public void visit(int x, int y, int z, Bag<VerletSphere> local, Bag<VerletSphere> surround);
}
