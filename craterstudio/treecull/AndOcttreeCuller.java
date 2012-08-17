/*
 * Created on 9-apr-2007
 */

package craterstudio.treecull;

public class AndOcttreeCuller implements OcttreeCuller
{
   private OcttreeCuller[] checkers = new OcttreeCuller[0];

   public final void add(OcttreeCuller checker)
   {
      OcttreeCuller[] checkers0 = new OcttreeCuller[checkers.length + 1];
      System.arraycopy(checkers, 0, checkers0, 0, checkers.length);
      checkers0[checkers.length] = checker;
      checkers = checkers0;
   }

   public int feelIntersection(SpatiallyBound bound)
   {
      // AND = lowest type of intersection

      if (checkers.length == 0)
         return OcttreeCuller.FULLY_VISIBLE;

      int minimumIntersection = OcttreeCuller.FULLY_VISIBLE;

      for (int i = 0; i < checkers.length; i++)
      {
         int intersection = checkers[i].feelIntersection(bound);

         // can't get any lower, so stop here
         if (intersection == OcttreeCuller.NOT_VISIBLE)
            return intersection;

         // lower type of intersection
         if (intersection < minimumIntersection)
            minimumIntersection = intersection;
      }

      return minimumIntersection;
   }
}
