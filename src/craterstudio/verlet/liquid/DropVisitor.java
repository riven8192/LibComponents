/*
 * Created on Aug 8, 2009
 */

package craterstudio.verlet.liquid;

import craterstudio.util.Bag;

public interface DropVisitor
{
   public void visit(int x, int y, int z, Bag<Drop> local, Bag<Drop> surround);
}
