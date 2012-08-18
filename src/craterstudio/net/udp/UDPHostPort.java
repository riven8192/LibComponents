/*
 * Created on 27 jan 2010
 */

package craterstudio.net.udp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

class UDPHostPort
{
   public final byte[] host;
   public final int    port;

   public UDPHostPort(InetSocketAddress addr)
   {
      this(addr.getAddress().getAddress(), addr.getPort());
   }

   public UDPHostPort(byte[] host, int port)
   {
      this.host = host;
      this.port = port;
   }

   public InetSocketAddress createSocketAddress()
   {
      try
      {
         return new InetSocketAddress(InetAddress.getByAddress(this.host), this.port);
      }
      catch (UnknownHostException exc)
      {
         throw new IllegalStateException("should never happen", exc);
      }
   }

   @Override
   public boolean equals(Object obj)
   {
      if (!(obj instanceof UDPHostPort))
         return false;

      UDPHostPort that = (UDPHostPort) obj;
      if (!this.host.equals(that.host))
         return false;
      if (this.port != that.port)
         return false;
      return true;
   }

   @Override
   public int hashCode()
   {
      return (this.port * 37) ^ Arrays.hashCode(this.host);
   }
}