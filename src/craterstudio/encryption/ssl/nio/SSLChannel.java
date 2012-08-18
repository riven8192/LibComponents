/*
 * Created on 15 feb 2010
 */

package craterstudio.encryption.ssl.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

import javax.net.ssl.SSLEngine;

public class SSLChannel extends SelectableChannel
{
   private final SocketChannel backing;
   private final SSLEngine     engine;

   public SSLChannel(SocketChannel backing, SSLEngine engine)
   {
      this.backing = backing;
      this.engine = engine;

      new NioNonBlockingSSL(null, engine, 8 * 1024, null, null)
      {
         @Override
         public void onHandshakeSuccess()
         {

         }

         @Override
         public void onHandshakeFailure(Exception cause)
         {

         }

         @Override
         public void onInboundData(ByteBuffer decrypted)
         {

         }

         @Override
         public void onClosed()
         {

         }
      };
   }

   @Override
   public SelectableChannel configureBlocking(boolean block) throws IOException
   {
      return this.backing.configureBlocking(block);
   }

   @Override
   public Object blockingLock()
   {
      return this.backing.blockingLock();
   }

   @Override
   protected void implCloseChannel() throws IOException
   {
      this.backing.close();
   }

   @Override
   public boolean isBlocking()
   {
      return this.backing.isBlocking();
   }

   @Override
   public boolean isRegistered()
   {
      return this.backing.isRegistered();
   }

   @Override
   public SelectionKey keyFor(Selector sel)
   {
      SelectionKey key = this.backing.keyFor(sel);

      return key;
   }

   @Override
   public SelectorProvider provider()
   {
      return this.backing.provider();
   }

   @Override
   public SelectionKey register(Selector sel, int ops, Object att) throws ClosedChannelException
   {
      SelectionKey key = this.backing.register(sel, ops, att);

      return key;
   }

   @Override
   public int validOps()
   {
      return this.backing.validOps();
   }
}