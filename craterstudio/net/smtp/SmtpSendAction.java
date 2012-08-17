/*
 * Created on 25 jan 2010
 */

package craterstudio.net.smtp;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class SmtpSendAction
{
   static AtomicInteger    mailid_source = new AtomicInteger(1337);

   private final SmtpQueue queue;
   public final int        mailid;
   public final SmtpMail   mail;
   public final String     rcpt;
   public final long       timestamp;
   public final int        attempt;

   SmtpSendAction(int mailid, SmtpQueue queue, SmtpMail mail, String rcpt, long timestamp, int attempt)
   {
      this.queue = queue;
      this.mailid = mailid == -1 ? mailid_source.incrementAndGet() : mailid;
      this.mail = mail;
      this.rcpt = rcpt;
      this.timestamp = timestamp;
      this.attempt = attempt;
   }

   public SmtpSendAction duplicateDelayed(long delayInSeconds)
   {
      int mailid = -1;
      long timestamp = this.timestamp + delayInSeconds * 1000;
      int attempt = this.attempt + 1;
      return new SmtpSendAction(mailid, this.queue, this.mail, this.rcpt, timestamp, attempt);
   }

   public boolean send()
   {
      System.out.println("SMTP <--> querying MX records for: " + rcpt);

      List<String> hosts = SmtpQueue.getHostnamesUsingMXRecords(rcpt);

      if (hosts == null)
      {
         // could not query MX records

         System.out.println("SMTP <--> failed to query MX records for: " + rcpt);

         return false;
      }

      for (String host : hosts)
      {
         Smtp smtp = new Smtp();
         try
         {
            System.out.println("SMTP <--> connecting to " + host + "@" + Smtp.DEFAULT_PORT);

            Socket socket = new Socket(host, Smtp.DEFAULT_PORT);
            smtp.open(socket);
            smtp.send(mail, this.rcpt);
            smtp.quit();

            return true;
         }
         catch (IllegalStateException exc)
         {
            exc.printStackTrace();
         }
         catch (IOException exc)
         {
            exc.printStackTrace();
         }

         smtp.quit();
      }

      return false;
   }
}