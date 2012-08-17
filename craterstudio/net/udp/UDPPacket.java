/*
 * Created on 27 jan 2010
 */

package craterstudio.net.udp;

public class UDPPacket
{
   public final byte[]      data;
   public final UDPHostPort endpoint;

   public UDPPacket(byte[] data, UDPHostPort endpoint)
   {
      this.data = data;
      this.endpoint = endpoint;
   }
}
