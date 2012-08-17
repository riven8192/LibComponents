/*
 * Created on 26 feb 2010
 */

package craterstudio.rasterizer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MultiThreadPixelBuffer extends PixelBuffer
{
   private final PixelBuffer[] buffers;
   private final Executor[]    workers;

   public MultiThreadPixelBuffer(int w, int h)
   {
      this(Runtime.getRuntime().availableProcessors(), w, h);
   }

   public MultiThreadPixelBuffer(int threads, int w, int h)
   {
      super(w, h);

      this.buffers = new PixelBuffer[threads];
      this.workers = new Executor[threads];

      for (int i = 0; i < threads; i++)
      {
         this.buffers[i] = new PixelBuffer(w, h);
         this.workers[i] = Executors.newSingleThreadExecutor();
      }
   }

   @Override
   public void clearColorBuffer(int rgb)
   {
      this.barrier();

      super.clearColorBuffer(rgb);

      for (int i = 0; i < this.buffers.length; i++)
      {
         this.buffers[i].clearColorBuffer(rgb);
      }
   }

   @Override
   public void clearDepthBuffer()
   {
      this.barrier();

      super.clearDepthBuffer();

      for (int i = 0; i < this.buffers.length; i++)
      {
         this.buffers[i].clearDepthBuffer();
      }
   }

   private int p;

   public void renderTriangles(final Iterable<Triangle> triangles)
   {
      int i = (this.p++) % this.buffers.length;
      final PixelBuffer buffer = this.buffers[i];
      this.workers[i].execute(new Runnable()
      {
         @Override
         public void run()
         {
            buffer.renderTriangles(triangles);
         }
      });
   }

   @Override
   public void renderTriangles(final Triangle[] triangles)
   {
      int i = (this.p++) % this.buffers.length;
      final PixelBuffer buffer = this.buffers[i];
      this.workers[i].execute(new Runnable()
      {
         @Override
         public void run()
         {
            buffer.renderTriangles(triangles);
         }
      });
   }

   @Override
   public void renderTriangle(final Triangle triangle)
   {
      int i = (this.p++) % this.buffers.length;
      final PixelBuffer buffer = this.buffers[i];
      this.workers[i].execute(new Runnable()
      {
         @Override
         public void run()
         {
            System.out.println("renderTriangle");
            buffer.renderTriangle(triangle);
         }
      });
   }

   public void merge()
   {
      this.barrier();

      for (PixelBuffer buffer : this.buffers)
      {
         for (int i = 0; i < this.colorBuffer.length; i++)
         {
            if (buffer.depthBuffer[i] < this.depthBuffer[i])
            {
               this.colorBuffer[i] = buffer.colorBuffer[i];
               this.depthBuffer[i] = buffer.depthBuffer[i];
            }
         }
      }
   }

   private void barrier()
   {
      CountDownLatch[] latches = new CountDownLatch[this.buffers.length];

      for (int i = 0; i < this.buffers.length; i++)
      {
         final CountDownLatch latch = new CountDownLatch(1);

         this.workers[i].execute(new Runnable()
         {
            @Override
            public void run()
            {
               latch.countDown();
            }
         });

         latches[i] = latch;
      }

      for (int i = 0; i < this.buffers.length; i++)
      {
         try
         {
            latches[i].await();
         }
         catch (InterruptedException exc)
         {
            exc.printStackTrace();
         }
      }
   }
}