/*
 * Created on 9-apr-2007
 */

package craterstudio.treecull;

public interface OcttreeCuller
{
   public static final int NOT_VISIBLE = 0;
   public static final int PARTIALLY_VISIBLE = 1;
   public static final int FULLY_VISIBLE = 2;
   
   public int feelIntersection(SpatiallyBound bound);
}
