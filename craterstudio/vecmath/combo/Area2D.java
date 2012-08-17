/*
 * Created on 22 jul 2010
 */

package craterstudio.vecmath.combo;

import java.util.ArrayList;
import java.util.List;

import craterstudio.data.tuples.Pair;
import craterstudio.vecmath.Circle2f;
import craterstudio.vecmath.Line2f;
import craterstudio.vecmath.Plane2f;
import craterstudio.vecmath.Ray2f;
import craterstudio.vecmath.Triangle2f;
import craterstudio.vecmath.Vector2f;

public class Area2D
{
   public Area2D()
   {
      this.circles = new ArrayList<Circle2f>();
      this.lines = new ArrayList<Pair<Line2f, Plane2f>>();
   }

   //

   private final List<Circle2f>              circles;
   private final List<Pair<Line2f, Plane2f>> lines;

   public void addCircle(Circle2f circle)
   {
      this.circles.add(circle);
   }

   public void addTriangle(Triangle2f tri)
   {
      this.addLine(new Line2f(tri.p1, tri.p2));
      this.addLine(new Line2f(tri.p2, tri.p3));
      this.addLine(new Line2f(tri.p3, tri.p1));
   }

   public void addLine(Line2f line)
   {
      this.addLine(line, Plane2f.create(line.p1, line.p2));
   }

   public void addLine(Line2f line, Plane2f plane)
   {
      line = new Line2f(new Vector2f(line.p1), new Vector2f(line.p2));
      this.lines.add(new Pair<Line2f, Plane2f>(line, plane));
   }

   //

   private final List<Ray2f> nextRays = new ArrayList<Ray2f>();

   public void trace(Ray2f ray)
   {
      this.nextRays.add(ray);
   }

   public void step(int maxDepth, int maxRays, RayFeedback feedback)
   {
      List<Ray2f> stepRays = new ArrayList<Ray2f>();

      for (int depth = 0; depth <= maxDepth && !this.nextRays.isEmpty() && maxRays > 0; depth++)
      {
         if (this.nextRays.size() <= maxRays)
         {
            stepRays.addAll(this.nextRays);
            maxRays -= stepRays.size();
         }
         else
         {
            for (int i = 0; i < maxRays; i++)
               stepRays.add(this.nextRays.get(i));
            maxRays = 0;
         }
         this.nextRays.clear();

         for (Ray2f ray : stepRays)
            this.traceImpl(ray, depth, feedback);
         stepRays.clear();
      }
   }

   private void traceImpl(Ray2f incoming, int depth, RayFeedback feedback)
   {
      Ray2f temp = new Ray2f(new Vector2f(), new Vector2f());
      Ray2f outgoing = new Ray2f(new Vector2f(), new Vector2f());

      Pair<Line2f, Plane2f> hit = null;
      float nearestValue = Integer.MAX_VALUE;

      for (Circle2f circle : this.circles)
      {
         temp.origin.load(incoming.origin);
         temp.normal.load(incoming.normal);

         float at = circle.reflect(temp);
         if (Float.isNaN(at))
            continue;

         outgoing.origin.load(temp.origin);
         outgoing.normal.load(temp.normal);
         outgoing.origin.load(outgoing.follow(0.01f)); // tiny
         nearestValue = at;
         hit = new Pair<Line2f, Plane2f>(new Line2f(), new Plane2f(null, 0)); // FIXME
      }

      for (Pair<Line2f, Plane2f> pair : this.lines)
      {
         Line2f line = pair.first();
         Plane2f side = pair.second();

         if (!RayLineMath2D.intersects(incoming, line))
            continue;

         temp.origin.load(incoming.origin);
         temp.normal.load(incoming.normal);

         float at = side.reflect(temp);
         if (Float.isNaN(at) || at >= nearestValue)
            continue;

         outgoing.normal.load(temp.normal);
         outgoing.origin.load(temp.origin);
         outgoing.origin.load(outgoing.follow(0.01f)); // tiny
         nearestValue = at;
         hit = pair;
      }

      if (hit == null)
      {
         nearestValue = Float.NaN;
         feedback.feedback(this, incoming, null, null, outgoing, nearestValue, depth);
      }
      else
      {
         feedback.feedback(this, incoming, hit.second(), hit.first(), outgoing, nearestValue, depth);
      }
   }
}