/*
 * Created on 27 jan 2010
 */

package craterstudio.net.udp;

import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UDPPunchThroughClient
{
   final NonBlockingDatagramSocket socket;

   public UDPPunchThroughClient(DatagramSocket socket)
   {
      this.socket = new NonBlockingDatagramSocket(socket);
   }

   public void requestFrom(String serial, UDPHostPort publicServer)
   {
      byte[] data = serial.getBytes();

      UDPPacket packet = new UDPPacket(data, publicServer);

      this.socket.sendLater(packet);

      UDPPacket echo = this.socket.pop();
   }

   public void close()
   {
      this.socket.close();
   }

   //

   final Map<String, Set<UDPHostPort>> serialToGroup;
   {
      serialToGroup = new HashMap<String, Set<UDPHostPort>>();
   }

   final void doEcho(UDPPacket packet)
   {
      this.socket.sendLater(packet);
   }

   final void doPunchThrough(UDPHostPort a, UDPHostPort b)
   {
      this.doPunchThroughOneWay(a, b);
      this.doPunchThroughOneWay(b, a);
   }

   private final void doPunchThroughOneWay(UDPHostPort src, UDPHostPort dst)
   {
      // [2 byte port][n byte addr]

      byte[] data = new byte[2 + src.host.length];

      System.arraycopy(src.host, 0, data, 2, src.host.length);

      data[0] = (byte) ((src.port >> 8) & 0xFF);
      data[1] = (byte) ((src.port >> 0) & 0xFF);

      UDPPacket packet = new UDPPacket(data, dst);

      this.socket.sendLater(packet);
   }

   //

   class Processor implements Runnable
   {
      @Override
      public void run()
      {
         while (true)
         {
            UDPPacket packet = socket.pop();

            doEcho(packet); // ACK endpoint

            // figure out requested serial
            String serial = new String(packet.data);

            // lookup group of serial
            Set<UDPHostPort> group = serialToGroup.get(serial);

            if (group == null)
            {
               // create group

               group = new HashSet<UDPHostPort>();
               serialToGroup.put(serial, group);
            }
            else
            {
               // punch through to everybody in group (except self)

               for (UDPHostPort member : group)
               {
                  if (!member.equals(packet.endpoint))
                  {
                     doPunchThrough(packet.endpoint, member);
                  }
               }

               // cleanup ?
            }

            // join group

            group.add(packet.endpoint);
         }
      }
   }
}
