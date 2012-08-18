/*
 * Created on 26 feb 2010
 */

package craterstudio.rasterizer;

import java.util.ArrayList;
import java.util.List;
import craterstudio.math.VecMath;

public class Triangle
{
   public Vertex a, b, c;

   public Triangle()
   {
      this(null, null, null);
   }

   public Triangle(Vertex a, Vertex b, Vertex c)
   {
      this.a = a;
      this.b = b;
      this.c = c;
   }

   public Triangle(Triangle abc)
   {
      this.a = abc.a.copy();
      this.b = abc.b.copy();
      this.c = abc.c.copy();
   }

   //

   public double area()
   {
      double abx = a.position.x - b.position.x;
      double aby = a.position.y - b.position.y;
      double abz = a.position.z - b.position.z;
      double ab2 = abx * abx + aby * aby + abz * abz;
      double ab4 = ab2 * ab2;

      double bcx = b.position.x - c.position.x;
      double bcy = b.position.y - c.position.y;
      double bcz = b.position.z - c.position.z;
      double bc2 = bcx * bcx + bcy * bcy + bcz * bcz;
      double bc4 = bc2 * bc2;

      double cax = c.position.x - a.position.x;
      double cay = c.position.y - a.position.y;
      double caz = c.position.z - a.position.z;
      double ca2 = cax * cax + cay * cay + caz * caz;
      double ca4 = ca2 * ca2;

      double abc2 = ab2 + bc2 + ca2;
      double abc4 = ab4 + bc4 + ca4;

      return 0.25 * Math.sqrt((abc2 * abc2) - 2.0 * abc4);
   }

   //

   public static List<Triangle> split(List<Triangle> src)
   {
      List<Triangle> dst = new ArrayList<Triangle>();
      Triangle.split(src, dst);
      return dst;
   }

   public static void split(List<Triangle> src, List<Triangle> dst)
   {
      for (Triangle tri : src)
      {
         tri.splitInto(dst);
      }
   }

   public void splitInto(List<Triangle> list)
   {
      Vertex ab = new Vertex();
      Vertex bc = new Vertex();
      Vertex ca = new Vertex();

      //

      VecMath.lerp(0.5f, a.position, b.position, ab.position);
      VecMath.lerp(0.5f, b.position, c.position, bc.position);
      VecMath.lerp(0.5f, c.position, a.position, ca.position);

      VecMath.lerp(0.5f, a.color, b.color, ab.color);
      VecMath.lerp(0.5f, b.color, c.color, bc.color);
      VecMath.lerp(0.5f, c.color, a.color, ca.color);

      VecMath.lerp(0.5f, a.texcoord, b.texcoord, ab.texcoord);
      VecMath.lerp(0.5f, b.texcoord, c.texcoord, bc.texcoord);
      VecMath.lerp(0.5f, c.texcoord, a.texcoord, ca.texcoord);

      //

      list.add(new Triangle(a, ab, ca));
      list.add(new Triangle(ab, b, bc));
      list.add(new Triangle(ca, bc, c));
      list.add(new Triangle(ab, bc, ca));
   }
}
