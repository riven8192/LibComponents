/*
 * Created on 26 feb 2010
 */

package craterstudio.rasterizer;

import craterstudio.math.Vec2;
import craterstudio.math.Vec3;

public class Vertex
{
   public final Vec3 position, color; //, normal;
   public final Vec2 texcoord;

   public Vertex()
   {
      this.position = new Vec3();
      this.color = new Vec3(1.0f, 1.0f, 1.0f); // white
      this.texcoord = new Vec2();
      //  this.normal = new Vec3(0.0f, 1.0f, 0.0f); // up
   }

   public Vertex copy()
   {
      Vertex copy = new Vertex();
      copy.position.load(this.position);
      copy.color.load(this.color);
      copy.texcoord.load(this.texcoord);
      return copy;
   }
}
