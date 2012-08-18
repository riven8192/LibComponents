/*
 * Created on 25 jan 2010
 */

package craterstudio.net.smtp;

public interface SmtpStorage
{
   public long delay();

   //

   public boolean isEmpty();

   public void push(SmtpSendAction action);

   public SmtpSendAction pop();

   //

   public void sent(SmtpSendAction action);

   public void failed(SmtpSendAction action);
}