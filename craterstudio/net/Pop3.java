/*
 * Created on 7-jun-2005
 */
package craterstudio.net;

import java.io.Writer;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;

import craterstudio.text.Text;
import craterstudio.text.TextValues;

public class Pop3
{
   private final BufferedReader reader;
   private final Writer         writer;

   public Pop3(String host) throws IOException
   {
      this(host, 110);
   }

   public Pop3(String host, int port) throws IOException
   {
      this(new Socket(host, port));
   }

   public Pop3(Socket socket) throws IOException
   {
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      writer = new OutputStreamWriter(socket.getOutputStream());

      reader.readLine();
   }

   public final void login(String user, String pass) throws IOException
   {
      writer.write("USER " + user + "\r\n");
      writer.flush();
      String line1 = reader.readLine();
      if (!line1.startsWith("+OK"))
         throw new IllegalStateException("error response: " + line1);

      writer.write("PASS " + pass + "\r\n");
      writer.flush();
      String line2 = reader.readLine();
      if (!line2.startsWith("+OK"))
         throw new IllegalStateException("error response: " + line2);
   }

   public final Pop3Stat stat() throws IOException
   {
      writer.write("STAT" + "\r\n");
      writer.flush();
      String response = reader.readLine();
      if (!response.startsWith("+OK"))
         throw new IllegalStateException("error response: " + response);

      String text = Text.splitPair(response, ' ')[1];

      int[] values = TextValues.parseInts(Text.splitPair(text, ' '));
      return new Pop3Stat(values[0], values[1]);
   }

   public final Pop3ListEntry list(int msg) throws IOException
   {
      writer.write("LIST " + msg + "\r\n");
      writer.flush();
      String response = reader.readLine();
      if (!response.startsWith("+OK"))
         throw new IllegalStateException("error response: " + response);

      String line = response.substring(4);
      int[] values = TextValues.parseInts(Text.splitPair(line, ' '));
      return new Pop3ListEntry(values[0], values[1]);
   }

   public final List<Pop3ListEntry> list() throws IOException
   {
      writer.write("LIST " + "\r\n");
      writer.flush();
      String response = reader.readLine();
      if (!response.startsWith("+OK"))
      {
         throw new IllegalStateException("error response: " + response);
      }

      List<Pop3ListEntry> entries = new ArrayList<Pop3ListEntry>();

      while (true)
      {
         String line = reader.readLine(); // n len

         if (line.equals("."))
            break;

         int[] values = TextValues.parseInts(Text.splitPair(line, ' '));
         entries.add(new Pop3ListEntry(values[0], values[1]));
      }

      return entries;
   }

   public final String uidl(int msg) throws IOException
   {
      writer.write("UIDL " + msg + "\r\n");
      writer.flush();
      String response = reader.readLine();
      if (!response.startsWith("+OK"))
         throw new IllegalStateException("error response: " + response);
      return Text.split(response, ' ')[2];
   }

   public final ReceivedMail top(int msg, int lines) throws IOException
   {
      writer.write("TOP " + msg + " " + lines + "\r\n");
      writer.flush();
      String response = reader.readLine();
      if (!response.startsWith("+OK"))
         throw new IllegalStateException("error response: " + response);

      return this.processLines(reader);
   }

   public final ReceivedMail retr(int msg) throws IOException
   {
      writer.write("RETR " + msg + "\r\n");
      writer.flush();
      String response = reader.readLine();
      if (!response.startsWith("+OK"))
         throw new IllegalStateException("error response: " + response);

      return this.processLines(reader);
   }

   private final ReceivedMail processLines(BufferedReader reader) throws IOException
   {
      ReceivedMail mail = new ReceivedMail();

      // read header
      String lastHeaderKey = null;
      while (true)
      {
         String line = reader.readLine();
         if (line == null)
            throw new IllegalStateException();
         if (line.equals(""))
            break;

         if (!line.contains(": "))
         {
            String lastValue = mail.getHeader(lastHeaderKey);
            lastValue += line;
            mail.putHeader(lastHeaderKey, lastValue);
         }
         else
         {
            String[] pair = Text.splitPair(line, ": ");
            String key = pair[0];
            String val = pair[1];
            mail.putHeader(key, val);
            lastHeaderKey = key;
         }
      }

      // read content
      while (true)
      {
         String line = reader.readLine();
         if (line == null)
            throw new IllegalStateException();
         if (line.equals("."))
            break;

         mail.addContent(line);
      }

      return mail;
   }

   /**
    * DELE
    */

   public final boolean dele(int msg) throws IOException
   {
      writer.write("DELE " + msg + "\r\n");
      writer.flush();
      String response = reader.readLine();
      return response.startsWith("+OK");
   }

   /**
    * NOOP
    */

   public final boolean noop() throws IOException
   {
      writer.write("NOOP" + "\r\n");
      writer.flush();
      String response = reader.readLine();
      return response.startsWith("+OK");
   }

   /**
    * QUIT
    */

   public final boolean quit() throws IOException
   {
      writer.write("QUIT" + "\r\n");
      writer.flush();
      String response = reader.readLine();
      return response.startsWith("+OK");
   }

   /**
    * 
    */

   public final void disconnect() throws IOException
   {
      reader.close();
      writer.close();
   }

   public class Pop3ListEntry
   {
      public Pop3ListEntry(int index, int size)
      {
         this.index = index;
         this.size = size;
      }

      public final int index;
      public final int size;

      public String toString()
      {
         return "Entry[index=" + index + ", size=" + size + "]";
      }
   }

   public class Pop3Stat
   {
      public Pop3Stat(int count, int size)
      {
         this.count = count;
         this.size = size;
      }

      public final int count;
      public final int size;

      public String toString()
      {
         return "Stat[count=" + count + ", size=" + size + "]";
      }
   }
}
