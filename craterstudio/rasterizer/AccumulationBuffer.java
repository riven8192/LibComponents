/*
 * Created on May 3, 2011
 */

package craterstudio.rasterizer;

import java.util.Arrays;

public class AccumulationBuffer
{
   private final int[] r, g, b;
   private int         counter;

   public AccumulationBuffer(int w, int h)
   {
      this.r = new int[w * h];
      this.g = new int[w * h];
      this.b = new int[w * h];
      this.counter = 0;
   }

   public void clear(int r, int g, int b)
   {
      Arrays.fill(this.r, r);
      Arrays.fill(this.g, g);
      Arrays.fill(this.b, b);

      this.counter = 0;
   }

   public void accumulate(PixelBuffer source, float mul)
   {
      this.accumulate(source.colorBuffer, mul);
   }

   public void mergeInto(PixelBuffer target)
   {
      this.mergeInto(target.colorBuffer);
   }

   //

   public void accumulate(int[] rgb, float mul)
   {
      for (int i = 0; i < rgb.length; i++)
      {
         int rgb1 = rgb[i];
         this.r[i] += ((rgb1 & 0xFF0000) >> (8 * 2)) * mul;
         this.g[i] += ((rgb1 & 0x00FF00) >> (8 * 1)) * mul;
         this.b[i] += ((rgb1 & 0x0000FF) >> (8 * 0)) * mul;
      }
      this.counter++;
   }

   public void mergeInto(int[] rgb)
   {
      for (int i = 0; i < rgb.length; i++)
      {
         int r = this.r[i] / this.counter;
         int g = this.g[i] / this.counter;
         int b = this.b[i] / this.counter;

         rgb[i] = (r << 16) | (g << 8) | (b << 0);
      }
   }
}
