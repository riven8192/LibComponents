/*
 * Created on 26 feb 2010
 */

package craterstudio.rasterizer;

import java.util.Arrays;

import craterstudio.math.Matrix4d;
import craterstudio.math.Vec3;

public class PixelBuffer
{
   public final Matrix4d modelView  = new Matrix4d();
   public final Matrix4d projection = new Matrix4d();
   public final int[]    colorBuffer;
   public final float[]  depthBuffer;
   public boolean        wireframe  = false;
   public final int      w, h;

   public PixelBuffer(int[] colorBuffer, int w)
   {
      if (colorBuffer.length % w != 0)
         throw new IllegalStateException();

      this.w = w;
      this.h = colorBuffer.length / w;

      this.colorBuffer = colorBuffer;
      this.depthBuffer = new float[w * h];
   }

   public PixelBuffer(int w, int h)
   {
      this.w = w;
      this.h = h;

      this.colorBuffer = new int[w * h];
      this.depthBuffer = new float[w * h];
   }

   public final Matrix4d modelViewProjection = new Matrix4d();

   public void updateTransform(boolean applyOpenGLNormalisation)
   {
      this.modelViewProjection.identity();

      if (applyOpenGLNormalisation)
      {
         this.modelViewProjection.scale(this.w * 0.5f, this.h * 0.5f, 1.0f);
         this.modelViewProjection.translate(1.0f, 1.0f, 1.0f);
      }

      this.modelViewProjection.mult(this.projection);
      this.modelViewProjection.mult(this.modelView);
   }

   private final double[] vValues   = new double[9];
   private final double[] cValues   = new double[9];
   private final double[] tValues   = new double[9];
   //private final double[] normalValues   = new double[9];
   private final Vec3     aPosition = new Vec3();
   private final Vec3     bPosition = new Vec3();
   private final Vec3     cPosition = new Vec3();

   public void clearColorBuffer(int rgb)
   {
      Arrays.fill(this.colorBuffer, rgb);
   }

   public void clearDepthBuffer()
   {
      Arrays.fill(this.depthBuffer, Float.MAX_VALUE);
   }

   //

   private Texture texture;

   public void setTexture(Texture texture)
   {
      this.texture = texture;
   }

   private boolean textureQuality = true;

   public void setTextureQuality(boolean highQuality)
   {
      this.textureQuality = highQuality;
   }

   //

   public void renderTriangles(Iterable<Triangle> triangles)
   {
      for (Triangle triangle : triangles)
      {
         this.renderTriangle(triangle);
      }
   }

   public void renderTriangles(Triangle[] triangles)
   {
      for (Triangle triangle : triangles)
      {
         this.renderTriangle(triangle);
      }
   }

   public void renderTriangle(Triangle triangle)
   {
      this.modelViewProjection.transform(triangle.a.position, aPosition);
      this.modelViewProjection.transform(triangle.b.position, bPosition);
      this.modelViewProjection.transform(triangle.c.position, cPosition);

      vValues[0] = aPosition.x;
      vValues[1] = aPosition.y;
      vValues[2] = aPosition.z;
      vValues[3] = bPosition.x;
      vValues[4] = bPosition.y;
      vValues[5] = bPosition.z;
      vValues[6] = cPosition.x;
      vValues[7] = cPosition.y;
      vValues[8] = cPosition.z;

      cValues[0] = triangle.a.color.x;
      cValues[1] = triangle.a.color.y;
      cValues[2] = triangle.a.color.z;
      cValues[3] = triangle.b.color.x;
      cValues[4] = triangle.b.color.y;
      cValues[5] = triangle.b.color.z;
      cValues[6] = triangle.c.color.x;
      cValues[7] = triangle.c.color.y;
      cValues[8] = triangle.c.color.z;

      tValues[0] = triangle.a.texcoord.x;
      tValues[1] = triangle.a.texcoord.y;
      tValues[2] = 0.0f;
      tValues[3] = triangle.b.texcoord.x;
      tValues[4] = triangle.b.texcoord.y;
      tValues[5] = 0.0f;
      tValues[6] = triangle.c.texcoord.x;
      tValues[7] = triangle.c.texcoord.y;
      tValues[8] = 0.0f;

      // normalValues[0] = triangle.a.normal.x;
      // normalValues[1] = triangle.a.normal.y;
      // normalValues[2] = triangle.a.normal.z;
      // normalValues[3] = triangle.b.normal.x;
      // normalValues[4] = triangle.b.normal.y;
      // normalValues[5] = triangle.b.normal.z;
      // normalValues[6] = triangle.c.normal.x;
      // normalValues[7] = triangle.c.normal.y;
      // normalValues[8] = triangle.c.normal.z;

      double yA = vValues[0 * 3 + 1];
      double yB = vValues[1 * 3 + 1];
      double yC = vValues[2 * 3 + 1];

      int top, mid, bot;

      // figure out top/mid/bot vertices
      if (yA < yB)
      {
         if (yC < yA)
         {
            top = 6;
            mid = 0;
            bot = 3;
         }
         else if (yC < yB)
         {
            top = 0;
            mid = 6;
            bot = 3;
         }
         else
         {
            top = 0;
            mid = 3;
            bot = 6;
         }
      }
      else
      {
         if (yC > yA)
         {
            top = 3;
            mid = 0;
            bot = 6;
         }
         else if (yC < yB)
         {
            top = 6;
            mid = 3;
            bot = 0;
         }
         else
         {
            top = 3;
            mid = 6;
            bot = 0;
         }
      }

      double xTop = vValues[top + 0];
      double yTop = vValues[top + 1];
      double zTop = vValues[top + 2];
      double rTop = cValues[top + 0];
      double gTop = cValues[top + 1];
      double bTop = cValues[top + 2];
      double uTop = tValues[top + 0];
      double vTop = tValues[top + 1];

      double xMid = vValues[mid + 0];
      double yMid = vValues[mid + 1];
      double zMid = vValues[mid + 2];
      double rMid = cValues[mid + 0];
      double gMid = cValues[mid + 1];
      double bMid = cValues[mid + 2];
      double uMid = tValues[mid + 0];
      double vMid = tValues[mid + 1];

      double xBot = vValues[bot + 0];
      double yBot = vValues[bot + 1];
      double zBot = vValues[bot + 2];
      double rBot = cValues[bot + 0];
      double gBot = cValues[bot + 1];
      double bBot = cValues[bot + 2];
      double uBot = tValues[bot + 0];
      double vBot = tValues[bot + 1];

      double ratioMid = (yMid - yTop) / (yBot - yTop);
      double xNxt = xTop + ratioMid * (xBot - xTop);
      double zNxt = zTop + ratioMid * (zBot - zTop);
      double rNxt = rTop + ratioMid * (rBot - rTop);
      double gNxt = gTop + ratioMid * (gBot - gTop);
      double bNxt = bTop + ratioMid * (bBot - bTop);
      double uNxt = uTop + ratioMid * (uBot - uTop);
      double vNxt = vTop + ratioMid * (vBot - vTop);

      for (int i = 0; i < 2; i++)
      {
         double yOff, yEnd;
         double xMin2, xMin1, xMax2, xMax1;
         double zMin2, zMin1, zMax2, zMax1;
         double rMin2, rMin1, rMax2, rMax1;
         double gMin2, gMin1, gMax2, gMax1;
         double bMin2, bMin1, bMax2, bMax1;
         double uMin2, uMin1, uMax2, uMax1;
         double vMin2, vMin1, vMax2, vMax1;

         if (i == 0)
         {
            yOff = yTop;
            yEnd = yMid;

            xMin1 = xTop;
            xMin2 = xTop;
            xMax1 = xMid;
            xMax2 = xNxt;

            zMin1 = zTop;
            zMin2 = zTop;
            zMax1 = zMid;
            zMax2 = zNxt;

            //

            rMin1 = rTop;
            rMin2 = rTop;
            rMax1 = rMid;
            rMax2 = rNxt;

            gMin1 = gTop;
            gMin2 = gTop;
            gMax1 = gMid;
            gMax2 = gNxt;

            bMin1 = bTop;
            bMin2 = bTop;
            bMax1 = bMid;
            bMax2 = bNxt;

            //

            uMin1 = uTop;
            uMin2 = uTop;
            uMax1 = uMid;
            uMax2 = uNxt;

            vMin1 = vTop;
            vMin2 = vTop;
            vMax1 = vMid;
            vMax2 = vNxt;
         }
         else
         {
            yOff = yMid;
            yEnd = yBot;

            xMin1 = xNxt;
            xMin2 = xMid;
            xMax1 = xBot;
            xMax2 = xBot;

            zMin1 = zNxt;
            zMin2 = zMid;
            zMax1 = zBot;
            zMax2 = zBot;

            //

            rMin1 = rNxt;
            rMin2 = rMid;
            rMax1 = rBot;
            rMax2 = rBot;

            gMin1 = gNxt;
            gMin2 = gMid;
            gMax1 = gBot;
            gMax2 = gBot;

            bMin1 = bNxt;
            bMin2 = bMid;
            bMax1 = bBot;
            bMax2 = bBot;

            //

            uMin1 = uNxt;
            uMin2 = uMid;
            uMax1 = uBot;
            uMax2 = uBot;

            vMin1 = vNxt;
            vMin2 = vMid;
            vMax1 = vBot;
            vMax2 = vBot;
         }

         double y1capped = Math.max(0, yOff);
         double y2capped = Math.min(h, yEnd);

         for (double y = y1capped; y < y2capped; y += 1.0f)
         {
            double yRatio = (y - yOff) / (yEnd - yOff);

            double x1 = xMin1 + yRatio * (xMax1 - xMin1);
            double z1 = zMin1 + yRatio * (zMax1 - zMin1);
            double x2 = xMin2 + yRatio * (xMax2 - xMin2);
            double z2 = zMin2 + yRatio * (zMax2 - zMin2);

            double r1 = rMin1 + yRatio * (rMax1 - rMin1);
            double g1 = gMin1 + yRatio * (gMax1 - gMin1);
            double b1 = bMin1 + yRatio * (bMax1 - bMin1);
            double u1 = uMin1 + yRatio * (uMax1 - uMin1);
            double v1 = vMin1 + yRatio * (vMax1 - vMin1);

            double r2 = rMin2 + yRatio * (rMax2 - rMin2);
            double g2 = gMin2 + yRatio * (gMax2 - gMin2);
            double b2 = bMin2 + yRatio * (bMax2 - bMin2);
            double u2 = uMin2 + yRatio * (uMax2 - uMin2);
            double v2 = vMin2 + yRatio * (vMax2 - vMin2);

            if (x1 > x2) // swap x axis
            {
               double tt;

               tt = x1;
               x1 = x2;
               x2 = tt;

               tt = z1;
               z1 = z2;
               z2 = tt;

               //

               tt = r1;
               r1 = r2;
               r2 = tt;

               tt = g1;
               g1 = g2;
               g2 = tt;

               tt = b1;
               b1 = b2;
               b2 = tt;

               //

               tt = u1;
               u1 = u2;
               u2 = tt;

               tt = v1;
               v1 = v2;
               v2 = tt;
            }

            double x1capped = Math.floor(Math.max(0, x1));
            double x2capped = Math.ceil(Math.min(w, x2));

            Texture tex = this.texture;
            if (tex != null)
            {
               double uPerPixel = Math.abs(u1 - u2) / (x2 - x1);
               double vPerPixel = Math.abs(v1 - v2) / (x2 - x1);
               double mPerPixel = ((uPerPixel * tex.w) + (vPerPixel * tex.h)) * 0.5f;

               while (mPerPixel > Texture.INV_MIPMAP_FACTOR)
               {
                  Texture mipmap = tex.getMipmap();
                  if (mipmap == null)
                     break;
                  tex = mipmap;
                  mPerPixel *= Texture.INV_SQRT_TWO;
               }
            }

            // scanline
            double invConst = 1.0 / ((x2 - x1) + 1);
            for (double x = x1capped; x < x2capped; x += 1.0f)
            {
               double xRatio = (x - x1) * invConst;
               double z = z1 + xRatio * (z2 - z1);
               double r = r1 + xRatio * (r2 - r1);
               double g = g1 + xRatio * (g2 - g1);
               double b = b1 + xRatio * (b2 - b1);
               double u = u1 + xRatio * (u2 - u1);
               double v = v1 + xRatio * (v2 - v1);

               int xi = (int) x;
               int yi = (int) y;
               int p = yi * w + xi;

               // depth culling
               if (z < 0.0f || z > this.depthBuffer[p])
                  continue;

               if (tex != null)
               {
                  // texture sampling
                  int texel = tex.sampleNearestRepeat((float) u, (float) v);
                  int tr = (texel >> 16) & 0xFF;
                  int tg = (texel >> 8) & 0xFF;
                  int tb = (texel >> 0) & 0xFF;
                  r *= tr;
                  g *= tg;
                  b *= tb;
                  int ir = (int) (r) << 16;
                  int ig = (int) (g) << 8;
                  int ib = (int) (b) << 0;
                  this.colorBuffer[p] = ir | ig | ib;
               }
               else
               {
                  int ir = (int) (r * 0xFF) << 16;
                  int ig = (int) (g * 0xFF) << 8;
                  int ib = (int) (b * 0xFF) << 0;
                  this.colorBuffer[p] = ir | ig | ib;
               }
               this.depthBuffer[p] = (float) z;
            }
         }
      }
   }

   public void renderTriangle_old(Triangle triangle)
   {
      this.modelViewProjection.transform(triangle.a.position, aPosition);
      this.modelViewProjection.transform(triangle.b.position, bPosition);
      this.modelViewProjection.transform(triangle.c.position, cPosition);

      vValues[0] = aPosition.x;
      vValues[1] = aPosition.y;
      vValues[2] = aPosition.z;
      vValues[3] = bPosition.x;
      vValues[4] = bPosition.y;
      vValues[5] = bPosition.z;
      vValues[6] = cPosition.x;
      vValues[7] = cPosition.y;
      vValues[8] = cPosition.z;

      cValues[0] = triangle.a.color.x;
      cValues[1] = triangle.a.color.y;
      cValues[2] = triangle.a.color.z;
      cValues[3] = triangle.b.color.x;
      cValues[4] = triangle.b.color.y;
      cValues[5] = triangle.b.color.z;
      cValues[6] = triangle.c.color.x;
      cValues[7] = triangle.c.color.y;
      cValues[8] = triangle.c.color.z;

      tValues[0] = triangle.a.texcoord.x;
      tValues[1] = triangle.a.texcoord.y;
      tValues[2] = 0.0f;
      tValues[3] = triangle.b.texcoord.x;
      tValues[4] = triangle.b.texcoord.y;
      tValues[5] = 0.0f;
      tValues[6] = triangle.c.texcoord.x;
      tValues[7] = triangle.c.texcoord.y;
      tValues[8] = 0.0f;

      // normalValues[0] = triangle.a.normal.x;
      // normalValues[1] = triangle.a.normal.y;
      // normalValues[2] = triangle.a.normal.z;
      // normalValues[3] = triangle.b.normal.x;
      // normalValues[4] = triangle.b.normal.y;
      // normalValues[5] = triangle.b.normal.z;
      // normalValues[6] = triangle.c.normal.x;
      // normalValues[7] = triangle.c.normal.y;
      // normalValues[8] = triangle.c.normal.z;

      double yA = vValues[0 * 3 + 1];
      double yB = vValues[1 * 3 + 1];
      double yC = vValues[2 * 3 + 1];

      int top, mid, bot;

      // figure out top/mid/bot vertices
      if (yA < yB)
      {
         if (yC < yA)
         {
            top = 6;
            mid = 0;
            bot = 3;
         }
         else if (yC < yB)
         {
            top = 0;
            mid = 6;
            bot = 3;
         }
         else
         {
            top = 0;
            mid = 3;
            bot = 6;
         }
      }
      else
      {
         if (yC > yA)
         {
            top = 3;
            mid = 0;
            bot = 6;
         }
         else if (yC < yB)
         {
            top = 6;
            mid = 3;
            bot = 0;
         }
         else
         {
            top = 3;
            mid = 6;
            bot = 0;
         }
      }

      double xTop = vValues[top + 0];
      double yTop = vValues[top + 1];
      double zTop = vValues[top + 2];
      double rTop = cValues[top + 0];
      double gTop = cValues[top + 1];
      double bTop = cValues[top + 2];
      double uTop = tValues[top + 0];
      double vTop = tValues[top + 1];

      double xMid = vValues[mid + 0];
      double yMid = vValues[mid + 1];
      double zMid = vValues[mid + 2];
      double rMid = cValues[mid + 0];
      double gMid = cValues[mid + 1];
      double bMid = cValues[mid + 2];
      double uMid = tValues[mid + 0];
      double vMid = tValues[mid + 1];

      double xBot = vValues[bot + 0];
      double yBot = vValues[bot + 1];
      double zBot = vValues[bot + 2];
      double rBot = cValues[bot + 0];
      double gBot = cValues[bot + 1];
      double bBot = cValues[bot + 2];
      double uBot = tValues[bot + 0];
      double vBot = tValues[bot + 1];

      double ratioMid = (yMid - yTop) / (yBot - yTop);
      double xNxt = xTop + ratioMid * (xBot - xTop);
      double zNxt = zTop + ratioMid * (zBot - zTop);
      double rNxt = rTop + ratioMid * (rBot - rTop);
      double gNxt = gTop + ratioMid * (gBot - gTop);
      double bNxt = bTop + ratioMid * (bBot - bTop);
      double uNxt = uTop + ratioMid * (uBot - uTop);
      double vNxt = vTop + ratioMid * (vBot - vTop);

      for (int i = 0; i < 2; i++)
      {
         double yOff, yEnd;
         double xMin2, xMin1, xMax2, xMax1;
         double zMin2, zMin1, zMax2, zMax1;
         double rMin2, rMin1, rMax2, rMax1;
         double gMin2, gMin1, gMax2, gMax1;
         double bMin2, bMin1, bMax2, bMax1;
         double uMin2, uMin1, uMax2, uMax1;
         double vMin2, vMin1, vMax2, vMax1;

         if (i == 0)
         {
            yOff = yTop;
            yEnd = yMid;

            xMin1 = xTop;
            xMin2 = xTop;
            xMax1 = xMid;
            xMax2 = xNxt;

            zMin1 = zTop;
            zMin2 = zTop;
            zMax1 = zMid;
            zMax2 = zNxt;

            //

            rMin1 = rTop;
            rMin2 = rTop;
            rMax1 = rMid;
            rMax2 = rNxt;

            gMin1 = gTop;
            gMin2 = gTop;
            gMax1 = gMid;
            gMax2 = gNxt;

            bMin1 = bTop;
            bMin2 = bTop;
            bMax1 = bMid;
            bMax2 = bNxt;

            //

            uMin1 = uTop;
            uMin2 = uTop;
            uMax1 = uMid;
            uMax2 = uNxt;

            vMin1 = vTop;
            vMin2 = vTop;
            vMax1 = vMid;
            vMax2 = vNxt;
         }
         else
         {
            yOff = yMid;
            yEnd = yBot;

            xMin1 = xNxt;
            xMin2 = xMid;
            xMax1 = xBot;
            xMax2 = xBot;

            zMin1 = zNxt;
            zMin2 = zMid;
            zMax1 = zBot;
            zMax2 = zBot;

            //

            rMin1 = rNxt;
            rMin2 = rMid;
            rMax1 = rBot;
            rMax2 = rBot;

            gMin1 = gNxt;
            gMin2 = gMid;
            gMax1 = gBot;
            gMax2 = gBot;

            bMin1 = bNxt;
            bMin2 = bMid;
            bMax1 = bBot;
            bMax2 = bBot;

            //

            uMin1 = uNxt;
            uMin2 = uMid;
            uMax1 = uBot;
            uMax2 = uBot;

            vMin1 = vNxt;
            vMin2 = vMid;
            vMax1 = vBot;
            vMax2 = vBot;
         }

         double y1capped = Math.max(0, yOff);
         double y2capped = Math.min(h, yEnd);

         for (double y = y1capped; y < y2capped; y += 1.0f)
         {
            double yRatio = (y - yOff) / (yEnd - yOff);

            double x1 = xMin1 + yRatio * (xMax1 - xMin1);
            double z1 = zMin1 + yRatio * (zMax1 - zMin1);
            double x2 = xMin2 + yRatio * (xMax2 - xMin2);
            double z2 = zMin2 + yRatio * (zMax2 - zMin2);

            double r1 = rMin1 + yRatio * (rMax1 - rMin1);
            double g1 = gMin1 + yRatio * (gMax1 - gMin1);
            double b1 = bMin1 + yRatio * (bMax1 - bMin1);
            double u1 = uMin1 + yRatio * (uMax1 - uMin1);
            double v1 = vMin1 + yRatio * (vMax1 - vMin1);

            double r2 = rMin2 + yRatio * (rMax2 - rMin2);
            double g2 = gMin2 + yRatio * (gMax2 - gMin2);
            double b2 = bMin2 + yRatio * (bMax2 - bMin2);
            double u2 = uMin2 + yRatio * (uMax2 - uMin2);
            double v2 = vMin2 + yRatio * (vMax2 - vMin2);

            if (x1 > x2) // swap x axis
            {
               double tt;

               tt = x1;
               x1 = x2;
               x2 = tt;

               tt = z1;
               z1 = z2;
               z2 = tt;

               //

               tt = r1;
               r1 = r2;
               r2 = tt;

               tt = g1;
               g1 = g2;
               g2 = tt;

               tt = b1;
               b1 = b2;
               b2 = tt;

               //

               tt = u1;
               u1 = u2;
               u2 = tt;

               tt = v1;
               v1 = v2;
               v2 = tt;
            }

            double x1capped = Math.floor(Math.max(0, x1));
            double x2capped = Math.ceil(Math.min(w, x2));

            Texture tex = this.texture;
            if (tex != null)
            {
               double uPerPixel = Math.abs(u1 - u2) / (x2 - x1);
               double vPerPixel = Math.abs(v1 - v2) / (x2 - x1);
               double mPerPixel = ((uPerPixel * tex.w) + (vPerPixel * tex.h)) * 0.5f;

               while (mPerPixel > Texture.INV_MIPMAP_FACTOR)
               {
                  Texture mipmap = tex.getMipmap();
                  if (mipmap == null)
                     break;
                  tex = mipmap;
                  mPerPixel *= Texture.INV_SQRT_TWO;
               }
            }

            // scanline
            for (double x = x1capped; x < x2capped; x += 1.0f)
            {
               double xRatio = (x - x1) / ((x2 - x1) + 1);
               double z = z1 + xRatio * (z2 - z1);
               double r = r1 + xRatio * (r2 - r1);
               double g = g1 + xRatio * (g2 - g1);
               double b = b1 + xRatio * (b2 - b1);
               double u = u1 + xRatio * (u2 - u1);
               double v = v1 + xRatio * (v2 - v1);
               int xi = (int) x;
               int yi = (int) y;

               int p = yi * w + xi;

               // depth culling
               if (z < 0.0f || z > this.depthBuffer[p])
                  continue;

               if (tex != null)
               {
                  // texture sampling
                  int tRGB;
                  if (this.textureQuality)
                     tRGB = tex.sampleLinear((float) u, (float) v);
                  else
                     tRGB = tex.sampleNearestRepeat((float) u, (float) v);
                  r *= ((tRGB >> 16) & 0xFF) / 255.0f;
                  g *= ((tRGB >> 8) & 0xFF) / 255.0f;
                  b *= ((tRGB >> 0) & 0xFF) / 255.0f;
               }

               int ir = (int) (r * 0xFF) << 16;
               int ig = (int) (g * 0xFF) << 8;
               int ib = (int) (b * 0xFF) << 0;

               this.colorBuffer[p] = ir | ig | ib;
               this.depthBuffer[p] = (float) z;
            }

            if (this.wireframe)
            {
               this.colorBuffer[(int) y * w + (int) x1capped] = 0xFF000000;
               this.colorBuffer[(int) y * w + (int) x2capped] = 0xFF000000;
            }
         }
      }
   }
}