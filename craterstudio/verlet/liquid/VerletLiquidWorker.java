/*
 * Created on Aug 8, 2009
 */

package craterstudio.verlet.liquid;

import craterstudio.util.Bag;
import craterstudio.util.concur.TaskQueue;
import craterstudio.verlet.VerletSphere;

public class VerletLiquidWorker
{
   private final TaskQueue[] queues;

   public VerletLiquidWorker()
   {
      this(Runtime.getRuntime().availableProcessors());
   }

   public VerletLiquidWorker(int threads)
   {
      if (threads < 1)
      {
         throw new IllegalArgumentException();
      }

      if (threads == 1)
      {
         // don't use multi-threading code on 1 core
         threads = 0;
      }

      this.queues = new TaskQueue[threads];

      for (int i = 0; i < queues.length; i++)
      {
         queues[i] = new TaskQueue();
         queues[i].launch();
      }
   }

   public void collide(final VerletLiquid liquid, VerletLiquidGrid grid)
   {
      final VerletLiquidVisitor visitor = liquid.createVisitor();

      if (this.queues.length == 0)
      {
         grid.visit(visitor);
         return;
      }

      grid.visit(new VerletLiquidVisitor()
      {
         @Override
         public void visit(final int x, final int y, final int z, final Bag<VerletSphere> local, final Bag<VerletSphere> surround)
         {
            int grouping = 5;
            int thread = (x / grouping) % queues.length;

            queues[thread].later(new Runnable()
            {
               @Override
               public void run()
               {
                  visitor.visit(x, y, z, local, surround);
               }
            });
         }
      });

      // barrier
      for (int i = 0; i < queues.length; i++)
      {
         queues[i].getBarrier().yieldFor();
      }
   }
}
