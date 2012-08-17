/*
 * Created on Oct 10, 2009
 */

package craterstudio.vecmath;

public class Rect2f
{
   public final Vector2f offset, size;

   public Rect2f()
   {
      this(new Vector2f(), new Vector2f());
   }

   public Rect2f(float x, float y, float w, float h)
   {
      this(new Vector2f(x, y), new Vector2f(w, h));
   }

   public Rect2f(Vector2f offset, Vector2f size)
   {
      if (size.x < 0.0f || size.y < 0.0f)
      {
         throw new IllegalStateException();
      }

      this.offset = offset;
      this.size = size;
   }

   public String toString()
   {
      return "rect[" + offset + " .. " + Vector2f.add(offset, size) + "]";
   }
}
