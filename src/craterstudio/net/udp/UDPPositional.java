/*
 * Created on 27 jan 2010
 */

package craterstudio.net.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import craterstudio.data.tuples.Pair;
import craterstudio.text.Text;

public class UDPPositional
{
   public static void main(String[] args) throws IOException
   {
      if (args[0].equals("listen"))
      {
         final LinkedBlockingQueue<Pair<InetSocketAddress, byte[]>> queue;
         queue = new LinkedBlockingQueue<Pair<InetSocketAddress, byte[]>>();

         DatagramSocket socket = new DatagramSocket(8888);
         System.out.println(socket.getLocalAddress());

         new Thread()
         {
            public void run()
            {
               while (true)
               {
                  Pair<InetSocketAddress, byte[]> pair;

                  try
                  {
                     pair = queue.take();
                  }
                  catch (InterruptedException exc)
                  {
                     break;
                  }

                  System.out.println();
                  System.out.println(pair.first());
                  System.out.println(Text.ascii(pair.second()));
               }
            }
         }.start();

         byte[] buffer = new byte[4 * 1024];

         while (true)
         {
            DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
            socket.receive(packet);
            byte[] data = Arrays.copyOf(buffer, packet.getLength());
            InetSocketAddress addr = (InetSocketAddress) packet.getSocketAddress();

            Pair<InetSocketAddress, byte[]> pair;
            pair = new Pair<InetSocketAddress, byte[]>(addr, data);
            try
            {
               queue.put(pair);
            }
            catch (InterruptedException exc)
            {
               break;
            }
         }
      }
      else if (args[0].equals("shout"))
      {
         DatagramSocket socket = new DatagramSocket();
         System.out.println(socket.getLocalSocketAddress());

         byte[] data = Text.ascii("hello world");
         DatagramPacket packet = new DatagramPacket(data, 0, data.length);
         packet.setAddress(InetAddress.getByName("localhost"));
         packet.setPort(8888);
         socket.send(packet);
      }
   }
}
