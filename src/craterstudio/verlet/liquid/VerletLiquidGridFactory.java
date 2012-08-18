/*
 * Created on Aug 1, 2009
 */

package craterstudio.verlet.liquid;

import java.awt.Rectangle;

public class VerletLiquidGridFactory
{
   public static VerletLiquidGrid createGridForXY(Rectangle rect)
   {
      return createGridForXY(rect.x, rect.x + rect.width, rect.y, rect.y + rect.height);
   }

   public static VerletLiquidGrid createGridForXZ(Rectangle rect)
   {
      return createGridForXZ(rect.x, rect.x + rect.width, rect.y, rect.y + rect.height);
   }

   //   

   public static VerletLiquidGrid createGridForXY(float xMin, float xMax, float yMin, float yMax)
   {
      float size = initialSize(xMin, xMax, yMin, yMax, 0.0f, 0.0f);

      return new VerletLiquidGridXY(xMin, xMax, yMin, yMax, size);
   }

   public static VerletLiquidGrid createGridForXZ(float xMin, float xMax, float zMin, float zMax)
   {
      float size = initialSize(xMin, xMax, 0.0f, 0.0f, zMin, zMax);

      return new VerletLiquidGridXZ(xMin, xMax, zMin, zMax, size);
   }

   public static VerletLiquidGrid createGridForXYZ(float xMin, float xMax, float yMin, float yMax, float zMin, float zMax)
   {
      float size = initialSize(xMin, xMax, yMin, yMax, zMin, zMax);

      return new VerletLiquidGridXYZ(xMin, xMax, yMin, yMax, zMin, zMax, size);
   }

   //

   private static final float initialSize(float xMin, float xMax, float yMin, float yMax, float zMin, float zMax)
   {
      float xDim = xMax - xMin;
      float yDim = yMax - yMin;
      float zDim = zMax - zMin;

      float max = Math.max(xDim, Math.max(yDim, zDim));

      return max / 10.0f;
   }

}
