/*
 * Created on 28 jul 2009
 */

package craterstudio.verlet;

public interface Animation<T>
{
   public void begin(T t);

   public void step(T t, float lerp);

   public void end(T t);
   
   public Animation<T> copy();
}
