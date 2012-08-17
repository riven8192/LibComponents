/*
 * Created on 25 jan 2010
 */

package craterstudio.net.smtp;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

public class RamSmtpStorage implements SmtpStorage
{
   private final TreeSet<SmtpSendAction> actions;

   public RamSmtpStorage()
   {
      this.actions = new TreeSet<SmtpSendAction>(comparator);
   }

   @Override
   public long delay()
   {
      return 1000;
   }
   
   @Override
   public boolean isEmpty()
   {
      return this.actions.isEmpty();
   }

   @Override
   public synchronized void push(SmtpSendAction action)
   {
      this.actions.add(action);
   }

   @Override
   public synchronized SmtpSendAction pop()
   {
      Iterator<SmtpSendAction> it = this.actions.iterator();

      if (!it.hasNext())
         return null;

      SmtpSendAction action = it.next();
      if (action.timestamp > System.currentTimeMillis())
         return null;

      it.remove();
      return action;
   }

   @Override
   public void sent(SmtpSendAction action)
   {
      //
   }

   @Override
   public void failed(SmtpSendAction action)
   {
      //
   }

   // -----

   static Comparator<SmtpSendAction> comparator;

   static
   {
      comparator = new Comparator<SmtpSendAction>()
      {
         @Override
         public int compare(SmtpSendAction a, SmtpSendAction b)
         {
            if (a.timestamp < b.timestamp)
               return -1;
            if (a.timestamp > b.timestamp)
               return +1;
            return a.mailid - b.mailid;
         }
      };
   }
}
