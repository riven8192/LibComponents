/*
 * Created on 25 jan 2010
 */

package craterstudio.net.smtp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import craterstudio.data.tuples.Pair;
import craterstudio.net.DNS;
import craterstudio.text.Text;
import craterstudio.util.HighLevel;

public class SmtpQueue
{
   static AtomicInteger queueCounter = new AtomicInteger();

   public SmtpQueue()
   {
      this(new RamSmtpStorage());
   }

   final SmtpStorage storage;

   public SmtpQueue(SmtpStorage storage)
   {
      this.storage = storage;

      String threadName = "SMTP Queue #" + queueCounter.incrementAndGet();

      new Thread(this.createTask(), threadName).start();
   }

   public SmtpQueue enqueue(SmtpMail mail)
   {
      final long now = System.currentTimeMillis();

      return this.enqueue(mail, now);
   }

   public SmtpQueue enqueue(SmtpMail mail, long at)
   {
      for (SmtpContact rcpt : mail.getToList())
      {
         this.enqueueRcptAt(mail, rcpt.address, at);
      }

      for (SmtpContact rcpt : mail.getCcList())
      {
         this.enqueueRcptAt(mail, rcpt.address, at);
      }

      for (SmtpContact rcpt : mail.getBccList())
      {
         this.enqueueRcptAt(mail, rcpt.address, at);
      }

      return this;
   }

   //

   volatile boolean shutdownWhenDone = false;

   public void shutdownWhenDone()
   {
      this.shutdownWhenDone = true;
   }

   //

   private void enqueueRcptAt(SmtpMail mail, String rcpt, long timestamp)
   {
      this.storage.push(new SmtpSendAction(-1, this, mail, rcpt, timestamp, 0));
   }

   //

   private Runnable createTask()
   {
      return new Runnable()
      {
         @Override
         public void run()
         {
            while (true)
            {
               SmtpSendAction action = storage.pop();

               if (action == null)
               {
                  if (storage.isEmpty() && shutdownWhenDone)
                     return;
                  HighLevel.sleep(storage.delay());
                  continue;
               }

               if (action.send())
               {
                  storage.sent(action);
                  continue;
               }

               if (action.attempt <= retryDelaysInSeconds.length)
               {
                  // try again later
                  long delay = retryDelaysInSeconds[action.attempt];
                  System.err.println("SMTP Queue: unsuccessful attempt #" + (action.attempt + 1) + " at sending email: [" + action.mail.getSubject() + "] to " + action.rcpt + " (delaying " + delay + " seconds)");
                  SmtpSendAction next = action.duplicateDelayed(delay);
                  storage.push(next);
               }
               else
               {
                  // give up
                  System.err.println("SMTP Queue: failed to send email: [" + action.mail.getSubject() + "] to " + action.rcpt);
                  storage.failed(action);
               }
            }
         }
      };
   }

   static final List<String> getHostnamesUsingMXRecords(String to)
   {
      if (Smtp.OVERRIDE_MX_RECORDS_WITH_HOST != null)
      {
         List<String> hosts = new ArrayList<String>();
         hosts.add(Smtp.OVERRIDE_MX_RECORDS_WITH_HOST);
         return hosts;
      }

      String domainname = Text.after(to, '@');
      List<Pair<String, Integer>> domainPriorities = DNS.getMXRecords(domainname);
      if (domainPriorities == null)
         return null;

      List<String> hostnames = new ArrayList<String>();
      for (Pair<String, Integer> domainPriority : domainPriorities)
         hostnames.add(domainPriority.first());
      return hostnames;
   }

   static final long[] retryDelaysInSeconds = new long[] { 15, 60, 5 * 60, 15 * 60, 3600, 4 * 3600, 24 * 3600 };
}
