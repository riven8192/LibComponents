/*
 * Created on 28 jul 2009
 */

package craterstudio.verlet;

import java.util.LinkedList;

public class AnimatingQueueVerletSpringActor implements VerletSpringActor
{
   private AnimatingVerletSpringActor current;

   public AnimatingQueueVerletSpringActor()
   {
      this.queue = new LinkedList<Animation<VerletSpring>>();
      this.durs = new LinkedList<Long>();
   }

   public AnimatingQueueVerletSpringActor copy()
   {
      AnimatingQueueVerletSpringActor copy = new AnimatingQueueVerletSpringActor();
      for (int i = 0; i < this.queue.size(); i++)
      {
         copy.queue.addLast(this.queue.get(i).copy());
         copy.durs.addLast(this.durs.get(i));
      }
      return copy;
   }

   private final LinkedList<Animation<VerletSpring>> queue;
   private final LinkedList<Long>                    durs;

   public void enqueue(Animation<VerletSpring> anim, long dur)
   {
      this.queue.addLast(anim);
      this.durs.addLast(Long.valueOf(dur));
   }

   private AnimatingQueueVerletSpringActor loop;

   public void loop()
   {
      this.loop = this.copy();
   }

   @Override
   public void act(VerletSpring spring)
   {
      if (this.current == null)
      {
         if (this.queue.isEmpty())
         {
            if (this.loop == null)
            {
               return;
            }

            this.queue.addAll(this.loop.queue);
            this.durs.addAll(this.loop.durs);
         }

         Animation<VerletSpring> anim = this.queue.removeFirst();
         long t1 = System.currentTimeMillis();
         long t2 = t1 + this.durs.removeFirst().longValue();
         this.current = new AnimatingVerletSpringActor(t1, t2, anim);
      }

      this.current.act(spring);

      if (this.current.status == 3)
      {
         this.current = null;
      }
   }
}
