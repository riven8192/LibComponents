/*
 * Created on 22 jul 2010
 */

package craterstudio.vecmath.combo;

import craterstudio.vecmath.Line2f;
import craterstudio.vecmath.Plane2f;
import craterstudio.vecmath.Ray2f;

public interface RayFeedback
{
   public void feedback(Area2D area, Ray2f incoming, Plane2f hitPlane, Line2f hitLine, Ray2f outgoing, float distance, int depth);
}
