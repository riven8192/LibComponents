/*
 * Created on 9-apr-2007
 */

package craterstudio.treecull;

public class OrOcttreeCuller implements OcttreeCuller
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
      // OR = highest type of intersection

      if (checkers.length == 0)
         return OcttreeCuller.FULLY_VISIBLE;

      int maximumIntersection = OcttreeCuller.NOT_VISIBLE;

      for (int i = 0; i < checkers.length; i++)
      {
         int intersection = checkers[i].feelIntersection(bound);

         // can't get any higher, so stop here
         if (intersection == OcttreeCuller.FULLY_VISIBLE)
            return intersection;

         // lower type of intersection
         if (intersection > maximumIntersection)
            maximumIntersection = intersection;
      }

      return maximumIntersection;
   }
}
