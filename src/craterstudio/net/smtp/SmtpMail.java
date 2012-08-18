/*
 * Created on 7-jun-2005
 */
package craterstudio.net.smtp;

import java.util.ArrayList;
import java.util.List;

public class SmtpMail
{
   /**
    * FROM
    */

   private SmtpContact from;

   public final void setFrom(SmtpContact from)
   {
      this.from = from;
   }

   public final SmtpContact getFrom()
   {
      return this.from;
   }

   /**
    * TO
    */

   private final List<SmtpContact> toList = new ArrayList<SmtpContact>();

   public final void addTo(SmtpContact smtpContact)
   {
      this.toList.add(smtpContact);
   }

   public final boolean hasTo()
   {
      return !this.toList.isEmpty();
   }

   public final List<SmtpContact> getToList()
   {
      return new ArrayList<SmtpContact>(this.toList);
   }

   /**
    * CC
    */

   private final List<SmtpContact> ccList = new ArrayList<SmtpContact>();

   public final void addCc(SmtpContact smtpContact)
   {
      this.ccList.add(smtpContact);
   }

   public final boolean hasCc()
   {
      return !this.ccList.isEmpty();
   }

   public final List<SmtpContact> getCcList()
   {
      return new ArrayList<SmtpContact>(this.ccList);
   }

   /**
    * BCC
    */

   private final List<SmtpContact> bccList = new ArrayList<SmtpContact>();

   public final void addBcc(SmtpContact smtpContact)
   {
      this.bccList.add(smtpContact);
   }

   public final boolean hasBcc()
   {
      return !this.bccList.isEmpty();
   }

   public final List<SmtpContact> getBccList()
   {
      return new ArrayList<SmtpContact>(this.bccList);
   }

   /**
    * SUBJECT
    */

   private String subject;

   public final void setSubject(String subject)
   {
      if (subject.indexOf('\n') != -1)
         throw new IllegalArgumentException("newline");
      if (subject.indexOf('\r') != -1)
         throw new IllegalArgumentException("newline");
      if (subject.indexOf('\t') != -1)
         throw new IllegalArgumentException("newline");
      this.subject = subject;
   }

   public final String getSubject()
   {
      return subject;
   }

   /**
    * MIME VERSION
    */

   private String mimeVersion = "1.0";

   public final void setMimeVersion(String mimeVersion)
   {
      if (mimeVersion.indexOf('\n') != -1)
         throw new IllegalArgumentException("newline");
      if (mimeVersion.indexOf('\r') != -1)
         throw new IllegalArgumentException("newline");
      if (mimeVersion.indexOf('\t') != -1)
         throw new IllegalArgumentException("newline");
      this.mimeVersion = mimeVersion;
   }

   public final String getMimeVersion()
   {
      return mimeVersion;
   }

   /**
    * CONTENT TYPE
    */

   public static final String PLAIN_TEXT  = "text/plain;charset=\"iso-8859-1\"";
   public static final String HTML_TEXT   = "text/html;charset=\"iso-8859-1\"";

   private String             contentType = PLAIN_TEXT;

   public final void setContentType(String contentType)
   {
      if (contentType.indexOf('\n') != -1)
         throw new IllegalArgumentException("newline");
      if (contentType.indexOf('\r') != -1)
         throw new IllegalArgumentException("newline");
      if (contentType.indexOf('\t') != -1)
         throw new IllegalArgumentException("newline");
      this.contentType = contentType;
   }

   public final String getContentType()
   {
      return contentType;
   }

   /**
    * BODY
    */

   private String body;

   public final void setBody(String body)
   {
      this.body = body;
   }

   public final String getBody()
   {
      return body;
   }

   //

   public void verify()
   {
      if (this.from == null)
         throw new IllegalStateException("no FROM contact");
      if (this.toList.isEmpty())
         throw new IllegalStateException("no TO contacts");
      if (this.subject == null)
         throw new IllegalStateException("no subject");
      if (this.body == null)
         throw new IllegalStateException("no body");
   }

   //

   public final String toString()
   {
      return "Email[to=" + toList + ", from=" + from + ", content-type=" + contentType + ", subject=" + subject + "]";
   }

}
