/*
 * Created on 18 jun 2009
 */

package craterstudio.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import sun.misc.BASE64Decoder;

import craterstudio.text.Text;

public class ReceivedMail
{
   private final Map<String, String> header;
   private final List<String>        content;

   public ReceivedMail()
   {
      this.header = new HashMap<String, String>();
      this.content = new ArrayList<String>();
   }

   // uidl

   private String uidl;

   public void setUIDL(String uidl)
   {
      this.uidl = uidl;
   }

   public String getUIDL()
   {
      return this.uidl;
   }

   // header

   public void putHeader(String key, String val)
   {
      this.header.put(key, val);
   }

   public String getHeader(String key)
   {
      String val = this.header.get(key);
      if (val == null)
         throw new NoSuchElementException(key);
      return val;
   }

   public Set<String> getHeaderKeys()
   {
      Set<String> set = new HashSet<String>();
      set.addAll(this.header.keySet());
      return set;
   }

   //

   public void addContent(String line)
   {
      this.content.add(line);
   }

   public List<String> getContentLines()
   {
      List<String> copy = new ArrayList<String>();
      copy.addAll(this.content);
      return copy;
   }

   public MailPart getMostLikelyPart()
   {
      if (!this.isMultiPart())
      {
         return this.getContentAsPart();
      }

      List<MailPart> parts = this.parseMultiParts();

      for (MailPart part : parts)
         if (part.isHTML())
            return part;

      for (MailPart part : parts)
         if (part.isPlainText())
            return part;

      throw new NoSuchElementException();
   }

   public MailPart getContentAsPart()
   {
      String nl = System.getProperty("line.separator");

      MailPart part = new MailPart();
      part.headers.putAll(this.header);
      for (String line : this.content)
         part.partcontent.append(line).append(nl);
      return part;
   }

   // date

   public String getDate()
   {
      return this.getHeader("Date");
   }

   // from

   public String getFromAddress()
   {
      String from = this.getHeader("From");
      if (from.endsWith(">"))
         return Text.between(from, '<', '>');
      return from;
   }

   public String getFromName()
   {
      String from = this.getHeader("From");
      if (from.endsWith(">"))
         return Text.before(from, '<').trim();
      return from;
   }

   // to

   public String getToAddress()
   {
      String to = this.getHeader("To");
      if (to.endsWith(">"))
         return Text.between(to, '<', '>');
      return to;
   }

   public String getToName()
   {
      String to = this.getHeader("To");
      if (to.endsWith(">"))
         return Text.before(to, '<').trim();
      return to;
   }

   // subject

   public String getSubject()
   {
      return this.getHeader("Subject");
   }

   public String getContentType()
   {
      return this.getHeader("Content-Type");
   }

   //

   public boolean isMultiPart()
   {
      if (this.content.isEmpty())
         return false;
      return this.content.get(0).equals("This is a multi-part message in MIME format.");
   }

   public final List<MailPart> parseMultiParts()
   {
      Iterator<String> lines = this.content.iterator();

      if (!lines.next().equals("This is a multi-part message in MIME format."))
      {
         throw new IllegalStateException("not a multi-part body (1)");
      }

      if (!lines.next().equals(""))
      {
         throw new IllegalStateException("not a multi-part body (2)");
      }

      List<MailPart> parts = new LinkedList<MailPart>();

      LinkedList<String> boundaryStack = new LinkedList<String>();
      boundaryStack.addLast(lines.next());

      outer: while (true)
      {
         MailPart part = new MailPart();
         parts.add(part);

         // read header
         while (true)
         {
            String line = lines.next();
            if (line.equals(""))
               break;

            String[] pair = Text.split(line, ": ");
            if (pair[1].endsWith(";"))
               pair[1] += " " + lines.next().trim();

            part.headers.put(pair[0], pair[1]);
         }

         if (part.getContentType().startsWith("multipart/alternative;"))
         {
            String b = Text.after(part.getContentType(), "; boundary=");
            b = b.trim();
            b = Text.between(b, '"', '"');
            b = "--" + b;
            boundaryStack.addLast(b);

            if (!lines.next().isEmpty())
               throw new IllegalStateException();
            String line = lines.next();
            if (!line.equals(b))
               throw new IllegalStateException(line);
            continue;
         }

         // read content
         while (true)
         {
            String boundary = boundaryStack.getLast();
            String line = lines.next();
            if (line.equals(boundary))
               break;

            if (line.equals(boundary + "--"))
            {
               boundaryStack.removeLast();
               if (boundaryStack.isEmpty())
                  break outer;

               if (!lines.next().isEmpty())
                  throw new IllegalStateException();
            }

            part.partcontent.append(line);
            part.partcontent.append("\r\n");
         }
      }

      return parts;
   }

   public class MailPart
   {
      public final Map<String, String> headers;
      public final StringBuilder       partcontent;

      public MailPart()
      {
         this.headers = new HashMap<String, String>();
         this.partcontent = new StringBuilder();
      }

      public boolean isPlainText()
      {
         String ct = this.getContentType();
         if (ct == null)
            return false;
         return ct.startsWith("text/plain");
      }

      public boolean isHTML()
      {
         String ct = this.getContentType();
         if (ct == null)
            return false;
         return ct.startsWith("text/html");
      }

      public String getContentType()
      {
         return this.headers.get("Content-Type");
      }

      public String getContentTransferEncoding()
      {
         return this.headers.get("Content-Transfer-Encoding");
      }

      public String getContent()
      {
         String content = this.partcontent.toString();
         String enc = this.getContentTransferEncoding();
         if (enc != null && enc.equals("base64"))
         {
            BASE64Decoder dec = new BASE64Decoder();

            try
            {
               content = Text.utf8(dec.decodeBuffer(content));
            }
            catch (IOException exc)
            {
               throw new IllegalStateException(exc);
            }
         }

         try
         {
            StringBuilder newContent = new StringBuilder();

            for (int i = 0; i < content.length(); i++)
            {
               char c = content.charAt(i);
               if (c == '=')
               {
                  char c1 = content.charAt(++i);
                  char c2 = content.charAt(++i);

                  if (c1 == '\r' && c2 == '\n')
                  {
                     continue;
                  }

                  int h1 = "0123456789ABCDEF".indexOf(c1);
                  int h2 = "0123456789ABCDEF".indexOf(c2);

                  if ((h1 | h2) == -1)
                  {
                     newContent.append(c);
                     newContent.append(c1);
                     newContent.append(c2);
                  }
                  else
                  {
                     int hh = (h1 << 4) | h2;
                     newContent.append((char) hh);
                  }
               }
               else
               {
                  newContent.append(c);
               }
            }

            content = newContent.toString();
         }
         catch (Exception exc)
         {
            throw new IllegalStateException(exc);
         }

         return content;
      }
   }
}