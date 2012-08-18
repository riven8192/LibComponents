/*
 * Created on 25 jan 2010
 */

package craterstudio.net.smtp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import craterstudio.io.FileUtil;
import craterstudio.text.Text;
import craterstudio.time.Interval;

public class DirectorySmtpStorage implements SmtpStorage
{
   private final File base, baseSoon, baseLater, baseSent, baseFailed;

   public DirectorySmtpStorage(File base)
   {
      FileUtil.ensurePathToDirectory(base);

      this.base = base;
      this.baseSoon = new File(this.base, "soon");
      this.baseLater = new File(this.base, "later");
      this.baseSent = new File(this.base, "sent");
      this.baseFailed = new File(this.base, "failed");

      FileUtil.ensurePathToDirectory(baseSoon);
      FileUtil.ensurePathToDirectory(baseLater);
      FileUtil.ensurePathToDirectory(baseSent);
      FileUtil.ensurePathToDirectory(baseFailed);
   }

   private final List<SmtpSendAction> pending       = new ArrayList<SmtpSendAction>();
   private final int                  soonThreshold = 10 * 1000;
   private final Interval             moveInterval  = new Interval(30 * 1000);

   @Override
   public long delay()
   {
      return soonThreshold;
   }

   @Override
   public synchronized boolean isEmpty()
   {
      if (this.baseSoon.list().length != 0)
         return false;
      if (this.baseLater.list().length != 0)
         return false;
      return true;
   }

   @Override
   public synchronized void push(SmtpSendAction action)
   {
      long toWait = action.timestamp - System.currentTimeMillis();
      boolean isSoon = (toWait < this.soonThreshold);

      System.out.println("SMTP Storage: inserting " + this.getFilename(action) + " into " + (isSoon ? "SOON" : "LATER"));
      File dir = isSoon ? this.baseSoon : this.baseLater;
      File file = new File(dir, this.getFilename(action));
      byte[] data = Text.ascii(this.wrap(action));
      FileUtil.writeFile(file, data);
   }

   @Override
   public synchronized SmtpSendAction pop()
   {
      if (this.pending.isEmpty())
      {
         if (moveInterval.hasPassedAndStepOver())
         {
            this.moveLaterToSoon();
         }

         this.pending.addAll(this.getSoonPendingActions());
      }

      if (this.pending.isEmpty())
      {
         return null;
      }

      SmtpSendAction action = this.pending.remove(0);

      try
      {
         FileUtil.deleteFile(new File(this.baseSoon, this.getFilename(action)));
      }
      catch (IOException exc)
      {
         exc.printStackTrace();
      }

      return action;
   }

   private final List<SmtpSendAction> getSoonPendingActions()
   {
      List<SmtpSendAction> pending = new ArrayList<SmtpSendAction>();
      for (String name : this.baseSoon.list())
      {
         long timestamp = Long.parseLong(Text.before(name, '_'));
         long toWait = timestamp - System.currentTimeMillis();

         if (toWait > 0)
            continue;

         System.out.println("SMTP Storage: moving " + name + " from SOON to PENDING");
         File file = new File(this.baseSoon, name);
         byte[] data = FileUtil.readFile(file);
         String text = Text.ascii(data);
         SmtpSendAction action = this.unwrap(text);
         pending.add(action);
      }
      return pending;
   }

   private final void moveLaterToSoon()
   {
      final long now = System.currentTimeMillis();

      for (String name : this.baseLater.list())
      {
         long timestamp = Long.parseLong(Text.before(name, '_'));
         long toWait = timestamp - now;

         if (toWait > this.soonThreshold)
            continue;

         System.out.println("SMTP Storage: moving " + name + " from LATER to SOON");
         File src = new File(this.baseLater, name);
         File dst = new File(this.baseSoon, name);

         try
         {
            FileUtil.moveFile(src, dst);
         }
         catch (IOException exc)
         {
            exc.printStackTrace();
         }
      }
   }

   //

   @Override
   public synchronized void sent(SmtpSendAction action)
   {
      System.out.println("SMTP Storage: sent " + this.getFilename(action));
      File file = new File(this.baseSent, this.getFilename(action));
      byte[] data = Text.ascii(this.wrap(action));
      FileUtil.writeFile(file, data);
   }

   @Override
   public synchronized void failed(SmtpSendAction action)
   {
      System.out.println("SMTP Storage: failed " + this.getFilename(action));
      File file = new File(this.baseFailed, this.getFilename(action));
      byte[] data = Text.ascii(this.wrap(action));
      FileUtil.writeFile(file, data);
   }

   //

   private String getFilename(SmtpSendAction action)
   {
      return action.timestamp + "_" + action.mailid + ".txt";
   }

   private String wrap(SmtpSendAction action)
   {
      SmtpMail mail = action.mail;

      StringBuilder builder = new StringBuilder();
      builder.append("SMTP_SEND_ACTION 1.1").append("\r\n");
      builder.append("MAILID ").append(action.mailid).append("\r\n");
      builder.append("TIMESTAMP ").append(action.timestamp).append("\r\n");
      builder.append("RCPT ").append(action.rcpt).append("\r\n");
      builder.append("ATTEMPT ").append(action.attempt).append("\r\n");
      builder.append("FROM ").append(mail.getFrom().toSmtpString()).append("\r\n");
      if (mail.hasTo())
         builder.append("TO ").append(SmtpContact.toSmtpString(mail.getToList())).append("\r\n");
      else
         throw new IllegalStateException();
      if (mail.hasCc())
         builder.append("CC ").append(SmtpContact.toSmtpString(mail.getCcList())).append("\r\n");
      if (mail.hasBcc())
         builder.append("BCC ").append(SmtpContact.toSmtpString(mail.getBccList())).append("\r\n");
      builder.append("MIMETYPE ").append(mail.getMimeVersion()).append("\r\n");
      builder.append("CONTENTTYPE ").append(mail.getContentType()).append("\r\n");
      builder.append("SUBJECT ").append(mail.getSubject()).append("\r\n");
      builder.append("\r\n");
      builder.append(mail.getBody());

      return builder.toString();
   }

   private SmtpSendAction unwrap(String wrap)
   {
      String[] lines = Text.splitOnLines(wrap);

      if (lines[0].equals("SMTP_SEND_ACTION 1.1"))
      {
         int mailid = -1;
         long timestamp = -1L;
         String rcpt = null;
         int attempt = -1;

         SmtpMail mail = new SmtpMail();

         int i = 1;
         for (; i < lines.length; i++)
         {
            String[] pair = Text.splitPair(lines[i], ' ');
            if (pair == null)
            {
               i++;
               break; // body!
            }

            if (pair[0].equals("MAILID"))
               mailid = Integer.parseInt(pair[1]);
            else if (pair[0].equals("TIMESTAMP"))
               timestamp = Long.parseLong(pair[1]);
            else if (pair[0].equals("RCPT"))
               rcpt = pair[1];
            else if (pair[0].equals("ATTEMPT"))
               attempt = Integer.parseInt(pair[1]);
            else if (pair[0].equals("FROM"))
               mail.setFrom(SmtpContact.fromSmtpString(pair[1]));
            else if (pair[0].equals("TO"))
               for (SmtpContact contact : SmtpContact.fromSmtpStrings(pair[1]))
                  mail.addTo(contact);
            else if (pair[0].equals("CC"))
               for (SmtpContact contact : SmtpContact.fromSmtpStrings(pair[1]))
                  mail.addCc(contact);
            else if (pair[0].equals("BCC"))
               for (SmtpContact contact : SmtpContact.fromSmtpStrings(pair[1]))
                  mail.addBcc(contact);
            else if (pair[0].equals("MIMETYPE"))
               mail.setMimeVersion(pair[1]);
            else if (pair[0].equals("CONTENTTYPE"))
               mail.setContentType(pair[1]);
            else if (pair[0].equals("SUBJECT"))
               mail.setSubject(pair[1]);
         }

         StringBuilder bodyBuilder = new StringBuilder();
         for (; i < lines.length; i++)
            bodyBuilder.append(lines[i]).append("\r\n");
         if (bodyBuilder.length() > 0)
            bodyBuilder.setLength(bodyBuilder.length() - 2);
         mail.setBody(bodyBuilder.toString());

         mail.verify();

         if (mailid == -1)
            throw new IllegalStateException();
         if (timestamp == -1L)
            throw new IllegalStateException();
         if (rcpt == null)
            throw new IllegalStateException();
         if (attempt == -1)
            throw new IllegalStateException();

         return new SmtpSendAction(mailid, null, mail, rcpt, timestamp, attempt);
      }

      throw new UnsupportedOperationException(lines[0]);
   }
}
