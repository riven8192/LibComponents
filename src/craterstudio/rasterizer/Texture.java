/*
 * Created on 26 feb 2010
 */

package craterstudio.rasterizer;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import craterstudio.math.EasyMath;
import craterstudio.misc.ImageUtil;

public class Texture
{
   public final int    w, h;
   public final float  fw1, fh1;
   private final int[] data;
   private Texture     mipmap;

   public Texture(BufferedImage img)
   {
      this.w = img.getWidth();
      this.h = img.getHeight();
      this.fw1 = w - 1;
      this.fh1 = h - 1;

      this.data = ImageUtil.accessRasterIntArray(img);

      if (this.data.length != this.w * this.h)
      {
         throw new IllegalStateException("incorrect backing int[] length, probably some subimage");
      }
   }

   public void createMipmaps(int minSize, boolean gamma)
   {
      // creates two images, one at 50% and one at 70.7% (=1/sqrt(2)),
      // creates mipmaps from those, and merges the result

      List<BufferedImage> mipmaps = new ArrayList<BufferedImage>();

      BufferedImage copy = new BufferedImage(this.w, this.h, BufferedImage.TYPE_INT_RGB);
      int[] currRGB = ImageUtil.accessRasterIntArray(copy);
      System.arraycopy(this.data, 0, currRGB, 0, this.data.length);

      BufferedImage sqrt = ImageUtil.scale(copy, INV_SQRT_TWO);
      BufferedImage half = copy;//ImageUtil.scaleHalfIntRGB(copy);

      mipmaps.add(sqrt);
      //  mipmaps.add(half);

      int i;
      do
      {
         i = mipmaps.size();

         if (Math.min(half.getWidth(), half.getHeight()) >= minSize)
            // if (gamma)
            // mipmaps.add(half = ImageUtil.mipmapGammaCorrected(half));
            //else
            mipmaps.add(half = ImageUtil.scaleHalfIntRGB(half));

         if (Math.min(sqrt.getWidth(), sqrt.getHeight()) >= minSize)
            //if (gamma)
            //   mipmaps.add(sqrt = ImageUtil.mipmapGammaCorrected(sqrt));
            //else
            mipmaps.add(sqrt = ImageUtil.scaleHalfIntRGB(sqrt));
      }
      while (i != mipmaps.size());

      Texture t = this;
      for (BufferedImage mipmap : mipmaps)
         t = (t.mipmap = new Texture(mipmap));
   }

   public static float INV_SQRT_TWO      = 1.0f / (float) Math.sqrt(2.0);
   public static float INV_MIPMAP_FACTOR = 1.0f / INV_SQRT_TWO;

   public Texture getMipmap()
   {
      return this.mipmap;
   }

   //

   public float repeat(float t)
   {
      return ((t % 1.0f) + 1.0f) % 1.0f;
   }

   public float clamp(float t)
   {
      return EasyMath.clamp(t, 0.0f, 1.0f);
   }

   //

   public boolean clamp;

   //

   public int sampleNearestClamp(float x, float y)
   {
      float xx = this.clamp(x) * this.fw1;
      float yy = this.clamp(y) * this.fh1;
      return this.data[(((int) yy) * this.w) + (int) xx];
   }

   public int sampleNearestRepeat(float x, float y)
   {
      float xx = this.repeat(x) * this.fw1;
      float yy = this.repeat(y) * this.fh1;
      return this.data[(((int) yy) * this.w) + (int) xx];
   }

   public int sampleLinear(float x, float y)
   {
      if (this.clamp)
      {
         x = this.clamp(x);
         y = this.clamp(y);
      }
      else
      {
         x = this.repeat(x);
         y = this.repeat(y);
      }

      float xx = x * (this.w - 1);
      float yy = y * (this.h - 1);

      // integers and fractions

      int ix0 = (int) xx;
      int iy0 = (int) yy;
      int ix1 = (ix0 >= w - 1) ? 0 : (ix0 + 1);
      int iy1 = (iy0 >= h - 1) ? 0 : (iy0 + 1);

      float fx = xx - ix0;
      float fy = yy - iy0;

      // read data

      int rgb00 = this.data[iy0 * this.w + ix0];
      int rgb01 = this.data[iy1 * this.w + ix0];
      int rgb10 = this.data[iy0 * this.w + ix1];
      int rgb11 = this.data[iy1 * this.w + ix1];

      // interpolate

      int rTop = (int) EasyMath.lerp(fx, (rgb00 >> s2) & 0xFF, (rgb10 >> s2) & 0xFF);
      int gTop = (int) EasyMath.lerp(fx, (rgb00 >> s1) & 0xFF, (rgb10 >> s1) & 0xFF);
      int bTop = (int) EasyMath.lerp(fx, (rgb00 >> s0) & 0xFF, (rgb10 >> s0) & 0xFF);

      int rBot = (int) EasyMath.lerp(fx, (rgb01 >> s2) & 0xFF, (rgb11 >> s2) & 0xFF);
      int gBot = (int) EasyMath.lerp(fx, (rgb01 >> s1) & 0xFF, (rgb11 >> s1) & 0xFF);
      int bBot = (int) EasyMath.lerp(fx, (rgb01 >> s0) & 0xFF, (rgb11 >> s0) & 0xFF);

      int rMid = (int) EasyMath.lerp(fy, rTop, rBot);
      int gMid = (int) EasyMath.lerp(fy, gTop, gBot);
      int bMid = (int) EasyMath.lerp(fy, bTop, bBot);

      //

      return (rMid << 16) | (gMid << 8) | (bMid << 0);
   }

   private static final int s2 = 16;
   private static final int s1 = 8;
   private static final int s0 = 0;
}
