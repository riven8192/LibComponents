/*
 * Created on 27 jan 2010
 */

package craterstudio.net.udp;

import java.net.DatagramSocket;
import java.nio.channels.IllegalSelectorException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UDPPunchThroughServer
{
   final NonBlockingDatagramSocket socket;

   public UDPPunchThroughServer(DatagramSocket socket)
   {
      this.socket = new NonBlockingDatagramSocket(socket);
   }

   public void close()
   {
      this.socket.close();
   }

   //

   class Group
   {
      public final String           password;
      public final Set<UDPHostPort> members;

      public Group(String password)
      {
         this.password = password;
         this.members = new HashSet<UDPHostPort>();
      }
   }

   final Map<String, Group> serialToGroup;
   {
      serialToGroup = new HashMap<String, Group>();
   }

   final void okPong(UDPPacket packet)
   {
      if (packet.data[0] != UDPUtil.DO_PING)
         throw new IllegalStateException();
      packet.data[0] = UDPUtil.OK_PONG;

      this.socket.sendLater(packet);
   }

   final void doCreateGroup(UDPHostPort endpoint, String serial, String password)
   {
      byte[] data;

      if (this.serialToGroup.containsKey(serial))
      {
         data = new byte[1];
         data[0] = UDPUtil.ERR_GROUP_EXISTS;
      }
      else
      {
         this.serialToGroup.put(serial, new Group(password));
         data = new byte[1];
         data[0] = UDPUtil.OK_CREATED_GROUP;
      }

      this.socket.sendLater(new UDPPacket(data, endpoint));
   }

   final void doJoinGroup(UDPHostPort endpoint, String password, Group group)
   {
      byte[] data;

      if (group == null)
      {
         data = new byte[1];
         data[0] = UDPUtil.ERR_NO_SUCH_GROUP;
      }
      else if (!group.password.equals(password))
      {
         data = new byte[1];
         data[0] = UDPUtil.ERR_INVALID_PASSWORD;
      }
      else if (group.members.size() == 16)
      {
         data = new byte[1];
         data[0] = UDPUtil.ERR_GROUP_IS_FULL;
      }
      else if (!group.members.add(endpoint))
      {
         data = new byte[1];
         data[0] = UDPUtil.ERR_ALREADY_IN_GROUP;
      }
      else
      {
         data = new byte[1 + group.members.size() * (16 + 2)];
         data[0] = UDPUtil.OK_JOINT_GROUP;
         data[1] = (byte) group.members.size();

         int p = 2;

         for (UDPHostPort client : group.members)
         {
            System.arraycopy(client.host, 0, data, p, client.host.length);
            p += client.host.length;
            data[p + 0] = (byte) ((client.port >> 8) & 0xFF);
            data[p + 1] = (byte) ((client.port >> 0) & 0xFF);
            p += 2;
         }
      }

      this.socket.sendLater(new UDPPacket(data, endpoint));
   }

   final void doLeaveGroup()
   {

   }

   final void doDestroyGroup()
   {

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

            switch (packet.data[0])
            {
               case UDPUtil.DO_PING:
               {
                  okPong(packet);
                  break;
               }

               case UDPUtil.DO_CREATE_GROUP:
               {
                  String serialAndPassword = new String(packet.data, 1, packet.data.length);
                  int io = serialAndPassword.indexOf(':');
                  if (io == -1)
                     throw new IllegalSelectorException();
                  String serial = serialAndPassword.substring(0, io);
                  String password = serialAndPassword.substring(io + 1);
                  doCreateGroup(packet.endpoint, serial, password);
                  break;
               }

               case UDPUtil.DO_JOIN_GROUP:
               {
                  String serialAndPassword = new String(packet.data, 1, packet.data.length);
                  int io = serialAndPassword.indexOf(':');
                  if (io == -1)
                     throw new IllegalSelectorException();
                  String serial = serialAndPassword.substring(0, io);
                  String password = serialAndPassword.substring(io + 1);
                  Group group = serialToGroup.get(serial);
                  doJoinGroup(packet.endpoint, password, group);

                  break;
               }

               case UDPUtil.DO_LEAVE_GROUP:
               {
                  String serial = new String(packet.data, 1, packet.data.length);

                  break;
               }

               case UDPUtil.DO_DESTROY_GROUP:
               {
                  String serial = new String(packet.data, 1, packet.data.length);

                  break;
               }
            }
         }
      }
   }
}
