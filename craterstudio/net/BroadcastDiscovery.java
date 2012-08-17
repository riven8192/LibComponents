/*
 * Created on 23 jun 2009
 */

package craterstudio.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import craterstudio.text.Text;

public class BroadcastDiscovery
{
   private final DatagramSocket socket;

   public BroadcastDiscovery(int port) throws IOException
   {
      this.socket = new DatagramSocket(port);
   }

   public void listen(long duration, BroadcastDiscoveryListener listener) throws IOException
   {
      byte[] buf = new byte[1024];
      long end = System.currentTimeMillis() + duration;

      while (true)
      {
         long rem = System.currentTimeMillis() - end;
         if (rem < 0)
            break;

         if (rem > Integer.MAX_VALUE)
            this.socket.setSoTimeout(0);
         else
            this.socket.setSoTimeout((int) rem);

         DatagramPacket packet = new DatagramPacket(buf, buf.length);
         this.socket.receive(packet);
         int len = packet.getLength();

         String serviceName = Text.utf8(buf, 0, len);

         listener.discoveredBroadcast(serviceName, packet.getAddress());
      }
   }

   public void close()
   {
      this.socket.close();
   }
}
