/*
 * Created on 27 jan 2010
 */

package craterstudio.net.udp;

public class UDPUtil
{
   static final byte DO_PING              = +1;
   static final byte OK_PONG              = -1;

   //

   static final byte DO_CREATE_GROUP      = +11;
   static final byte DO_JOIN_GROUP        = +12;
   static final byte DO_LEAVE_GROUP       = +13;
   static final byte DO_DESTROY_GROUP     = +14;

   static final byte OK_CREATED_GROUP     = -11;
   static final byte OK_JOINT_GROUP       = -12;
   static final byte OK_LEFT_GROUP        = -13;
   static final byte OK_DESTROYED_GROUP   = -14;

   static final byte ERR_INVALID_PASSWORD = -15;
   static final byte ERR_NO_SUCH_GROUP    = -16;
   static final byte ERR_NOT_IN_GROUP     = -17;
   static final byte ERR_GROUP_EXISTS     = -18;
   static final byte ERR_ALREADY_IN_GROUP = -19;
   static final byte ERR_GROUP_IS_FULL    = -20;

   //

   static byte[] wrap(int before, byte[] data, int after)
   {
      byte[] bigger = new byte[before + data.length + after];
      System.arraycopy(data, 0, bigger, before, data.length);
      return bigger;
   }

   static byte[][] unwrap(int before, byte[] data, int after)
   {
      byte[] a = new byte[before];
      byte[] b = new byte[data.length - before - after];
      byte[] c = new byte[after];
      System.arraycopy(data, 0, a, 0, a.length);
      System.arraycopy(data, before, b, 0, b.length);
      System.arraycopy(data, data.length - after, c, 0, c.length);
      return new byte[][] { a, b, c };
   }

}
