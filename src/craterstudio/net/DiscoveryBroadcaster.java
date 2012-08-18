/*
 * Created on 23 jun 2009
 */

package craterstudio.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import craterstudio.text.Text;
import craterstudio.util.HighLevel;
import craterstudio.util.NetworkUtil;

public class DiscoveryBroadcaster
{
   private final String         serviceName;
   private final int            sendPort;
   private final DatagramSocket socket;

   public DiscoveryBroadcaster(String serviceName, int sendPort) throws IOException
   {
      this(serviceName, sendPort, 0);
   }

   public DiscoveryBroadcaster(String serviceName, int sendPort, int bindPort) throws IOException
   {
      this.serviceName = serviceName;
      this.sendPort = sendPort;
      this.socket = new DatagramSocket(bindPort);
   }

   public void broadcast(int times, long interval) throws IOException
   {
      byte[] data = Text.utf8(serviceName);

      for (int i = 0; (times == -1) || (i < times); i++)
      {
         for (InetAddress sendAddr : NetworkUtil.getBroadcastAddresses())
         {
            this.socket.send(new DatagramPacket(data, 0, data.length, sendAddr, sendPort));
         }

         HighLevel.sleep(interval);
      }
   }

   public void close()
   {
      this.socket.close();
   }
}