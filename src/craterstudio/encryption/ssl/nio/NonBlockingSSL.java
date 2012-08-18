/*
 * Created on Feb 14, 2010
 */

package craterstudio.encryption.ssl.nio;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

public abstract class NonBlockingSSL
{
   final ByteBuffer wrapSrc, unwrapSrc;
   final ByteBuffer wrapDst, unwrapDst;

   final SSLEngine  engine;
   final Executor   ioWorker, taskWorkers;

   public NonBlockingSSL(SSLEngine engine, int bufferSize, Executor ioWorker, Executor taskWorkers)
   {
      this.wrapSrc = ByteBuffer.allocateDirect(bufferSize);
      this.wrapDst = ByteBuffer.allocateDirect(bufferSize);

      this.unwrapSrc = ByteBuffer.allocateDirect(bufferSize);
      this.unwrapDst = ByteBuffer.allocateDirect(bufferSize);

      this.unwrapSrc.limit(0);

      this.engine = engine;
      this.ioWorker = ioWorker;
      this.taskWorkers = taskWorkers;

      this.ioWorker.execute(this.stepTask);
   }

   public abstract void onInboundData(ByteBuffer decrypted);

   public abstract void onOutboundData(ByteBuffer encrypted);

   public abstract void onHandshakeFailure(Exception cause);

   public abstract void onHandshakeSuccess();

   public abstract void onClosed();

   public void sendLater(final ByteBuffer data)
   {
      this.ioWorker.execute(new Runnable()
      {
         @Override
         public void run()
         {
            wrapSrc.put(data);

            NonBlockingSSL.this.stepTask.run();
         }
      });
   }

   public void notifyReceived(final ByteBuffer data)
   {
      this.ioWorker.execute(new Runnable()
      {
         @Override
         public void run()
         {
            unwrapSrc.put(data);

            NonBlockingSSL.this.stepTask.run();
         }
      });
   }

   class StepTask implements Runnable
   {
      @Override
      public void run()
      {
         // executes non-blocking tasks on the IO-Worker

         while (NonBlockingSSL.this.step())
         {
            continue;
         }

         // apparently we hit a blocking-task...
      }
   }

   final StepTask stepTask = new StepTask();

   boolean step()
   {
      switch (engine.getHandshakeStatus())
      {
         case NOT_HANDSHAKING:
            boolean anything = false;
            {
               if (wrapSrc.position() > 0)
                  anything |= this.wrap();
               if (unwrapSrc.position() > 0)
                  anything |= this.unwrap();
            }
            return anything;

         case NEED_WRAP:
            if (!this.wrap())
               return false;
            break;

         case NEED_UNWRAP:
            if (!this.unwrap())
               return false;
            break;

         case NEED_TASK:
            final Runnable sslTask = engine.getDelegatedTask();
            Runnable wrappedTask = new Runnable()
            {
               @Override
               public void run()
               {
                  sslTask.run();

                  // continue handling I/O
                  ioWorker.execute(NonBlockingSSL.this.stepTask);
               }
            };
            taskWorkers.execute(wrappedTask);
            return false;

         case FINISHED:
            throw new IllegalStateException("FINISHED");
      }

      return true;
   }

   private boolean wrap()
   {
      SSLEngineResult wrapResult;

      try
      {
         wrapSrc.flip();
         wrapResult = engine.wrap(wrapSrc, wrapDst);
         wrapSrc.compact();
      }
      catch (SSLException exc)
      {
         this.onHandshakeFailure(exc);
         return false;
      }

      switch (wrapResult.getStatus())
      {
         case OK:
            if (wrapDst.position() > 0)
            {
               wrapDst.flip();
               this.onOutboundData(wrapDst);
               wrapDst.compact();
            }
            break;

         case BUFFER_UNDERFLOW:
            // try again later
            break;

         case BUFFER_OVERFLOW:
            throw new IllegalStateException("failed to wrap");

         case CLOSED:
            this.onClosed();
            return false;
      }

      return true;
   }

   private boolean unwrap()
   {
      SSLEngineResult unwrapResult;

      try
      {
         unwrapSrc.flip();
         unwrapResult = engine.unwrap(unwrapSrc, unwrapDst);
         unwrapSrc.compact();
      }
      catch (SSLException exc)
      {
         this.onHandshakeFailure(exc);
         return false;
      }

      switch (unwrapResult.getStatus())
      {
         case OK:
            if (unwrapDst.position() > 0)
            {
               unwrapDst.flip();
               this.onInboundData(unwrapDst);
               unwrapDst.compact();
            }
            break;

         case CLOSED:
            this.onClosed();
            return false;

         case BUFFER_OVERFLOW:
            throw new IllegalStateException("failed to unwrap");

         case BUFFER_UNDERFLOW:
            return false;
      }

      switch (unwrapResult.getHandshakeStatus())
      {
         case FINISHED:
            this.onHandshakeSuccess();
            return false;
      }

      return true;
   }
}
