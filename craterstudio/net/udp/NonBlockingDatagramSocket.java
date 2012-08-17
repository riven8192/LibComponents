/*
 * Created on 27 jan 2010
 */

package craterstudio.net.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import craterstudio.io.Streams;

public class NonBlockingDatagramSocket
{
   final DatagramSocket                 socket;
   final LinkedBlockingQueue<UDPPacket> inbound;
   final LinkedBlockingQueue<UDPPacket> outbound;

   public NonBlockingDatagramSocket(DatagramSocket socket)
   {
      this.socket = socket;
      this.inbound = new LinkedBlockingQueue<UDPPacket>();
      this.outbound = new LinkedBlockingQueue<UDPPacket>();

      new Thread(new BlockingSocketReceiver()).start();
      new Thread(new BlockingSocketSender()).start();
   }

   public void sendLater(UDPPacket packet)
   {
      try
      {
         this.outbound.put(packet);
      }
      catch (InterruptedException exc)
      {
         throw new IllegalStateException("interrupted while enqueuing outbound UDP packet", exc);
      }
   }

   public UDPPacket pop()
   {
      if (this.socket.isClosed())
      {
         throw new IllegalStateException("socket is closed");
      }

      try
      {
         return this.inbound.take();
      }
      catch (InterruptedException exc)
      {
         throw new IllegalStateException("interrupted while waiting for inbound UDP packet", exc);
      }
   }

   public UDPPacket poll()
   {
      return this.inbound.poll();
   }

   public void close()
   {
      this.socket.close();
   }

   //

   UDPPacket popOutbound()
   {
      if (this.socket.isClosed())
      {
         throw new IllegalStateException("socket is closed");
      }

      try
      {
         return this.outbound.take();
      }
      catch (InterruptedException exc)
      {
         throw new IllegalStateException("interrupted while waiting for outbound UDP packet", exc);
      }
   }

   class BlockingSocketReceiver implements Runnable
   {
      public void run()
      {
         final byte[] buffer = new byte[2 * 1024]; // MRU = 1500

         try
         {
            while (!socket.isClosed())
            {
               DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
               socket.receive(packet);
               byte[] data = Arrays.copyOf(buffer, packet.getLength());
               UDPHostPort addr = new UDPHostPort((InetSocketAddress) packet.getSocketAddress());

               try
               {
                  inbound.put(new UDPPacket(data, addr));
               }
               catch (InterruptedException exc)
               {
                  break;
               }
            }
         }
         catch (IOException exc)
         {
            Streams.safeClose(socket);
         }
      }
   }

   class BlockingSocketSender implements Runnable
   {
      public void run()
      {
         try
         {
            while (!socket.isClosed())
            {
               UDPPacket outbound = popOutbound();

               DatagramPacket datagram = new DatagramPacket(outbound.data, 0, outbound.data.length);
               datagram.setSocketAddress(outbound.endpoint.createSocketAddress());
               socket.send(datagram);
            }
         }
         catch (IOException exc)
         {
            Streams.safeClose(socket);
         }
      }
   }
}
