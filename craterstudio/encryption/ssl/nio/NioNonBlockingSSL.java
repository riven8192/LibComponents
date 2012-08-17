/*
 * Created on Feb 14, 2010
 */

package craterstudio.encryption.ssl.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLEngine;

public abstract class NioNonBlockingSSL extends NonBlockingSSL
{
   private final SelectionKey key;

   public NioNonBlockingSSL(SelectionKey key, SSLEngine engine, int bufferSize, Executor ioWorker, Executor taskWorkers)
   {
      super(engine, bufferSize, ioWorker, taskWorkers);

      this.key = key;
   }

   private final ByteBuffer big = ByteBuffer.allocateDirect(64 * 1024);

   public boolean onReadyToRead()
   {
      big.clear();
      int bytes;
      try
      {
         bytes = ((ReadableByteChannel) this.key.channel()).read(big);
      }
      catch (IOException exc)
      {
         bytes = -1;
      }
      if (bytes == -1)
         return false;
      big.flip();

      ByteBuffer copy = ByteBuffer.allocateDirect(bytes);
      copy.put(big);
      copy.flip();

      this.notifyReceived(copy);
      return true;
   }

   @Override
   public void onOutboundData(ByteBuffer encrypted)
   {
      try
      {
         ((WritableByteChannel) this.key.channel()).write(encrypted);

         if (encrypted.hasRemaining())
         {
            throw new IllegalStateException("failed to bulk-write");
         }
      }
      catch (IOException exc)
      {
         throw new IllegalStateException(exc);
      }
   }
}
