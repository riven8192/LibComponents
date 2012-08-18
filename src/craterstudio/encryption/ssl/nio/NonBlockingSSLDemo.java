/*
 * Created on Feb 14, 2010
 */

package craterstudio.encryption.ssl.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

public class NonBlockingSSLDemo
{
   public static void main(String[] args) throws Exception
   {
      final String host = "www.paypal.com";
      final int port = 443;
      final String uri = "/nl/cgi-bin/webscr?cmd=_home&country_lang.x=true";

      // connect to the webservice
      SelectionKey key;
      {
         InetSocketAddress connectTo = new InetSocketAddress(host, port);
         Selector selector = Selector.open();
         SocketChannel channel = SocketChannel.open();
         channel.connect(connectTo);
         System.out.println("connected to: " + channel);

         channel.configureBlocking(false);
         channel.socket().setTcpNoDelay(true);

         int ops = SelectionKey.OP_CONNECT | SelectionKey.OP_READ;
         key = channel.register(selector, ops);
      }

      // setup the io/worker threads
      final Executor ioWorker = Executors.newSingleThreadExecutor();
      final Executor taskWorkers = Executors.newFixedThreadPool(4);

      // setup the SSLEngine
      final SSLEngine engine = SSLContext.getDefault().createSSLEngine();
      engine.setUseClientMode(true);
      engine.beginHandshake();
      final int ioBufferSize = 64 * 1024;

      final NioNonBlockingSSL ssl;
      ssl = new NioNonBlockingSSL(key, engine, ioBufferSize, ioWorker, taskWorkers)
      {
         @Override
         public void onHandshakeFailure(Exception cause)
         {
            System.out.println("handshake failure");

            cause.printStackTrace();
         }

         @Override
         public void onHandshakeSuccess()
         {
            System.out.println("handshake success");

            SSLSession session = engine.getSession();

            try
            {
               System.out.println("- local principal: " + session.getLocalPrincipal());
               System.out.println("- remote principal: " + session.getPeerPrincipal());
               System.out.println("- using cipher: " + session.getCipherSuite());
            }
            catch (Exception exc)
            {
               exc.printStackTrace();
            }

            // simple HTTP request to www.paypal.com
            StringBuilder http = new StringBuilder();
            http.append("GET " + uri + " HTTP/1.0\r\n");
            http.append("Connection: close\r\n");
            http.append("\r\n");

            byte[] data = http.toString().getBytes();
            ByteBuffer send = ByteBuffer.wrap(data);
            this.sendLater(send);
         }

         @Override
         public void onInboundData(ByteBuffer decrypted)
         {
            // this is where the HTTP response ends up

            byte[] dst = new byte[decrypted.remaining()];
            decrypted.get(dst);
            String response = new String(dst);

            System.out.println(response);
         }

         @Override
         public void onClosed()
         {
            System.out.println("<ssl session closed>");
         }
      };

      // simplistic NIO stuff

      while (true)
      {
         key.selector().select();

         Iterator<SelectionKey> keys = key.selector().selectedKeys().iterator();

         while (keys.hasNext())
         {
            key = keys.next(); // there is only one key, don't bother
            keys.remove();

            ssl.onReadyToRead();
         }
      }
   }
}