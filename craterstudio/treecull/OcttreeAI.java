/*
 * Created on 9-apr-2007
 */

package craterstudio.treecull;

public interface OcttreeAI
{
   public boolean shouldSplit(Octtree node);

   public boolean shouldMerge(Octtree node);
}
