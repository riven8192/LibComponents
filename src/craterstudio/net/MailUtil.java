/*
 * Created on 7-jun-2005
 */
package craterstudio.net;

import java.io.Writer;
import java.io.IOException;
import java.io.BufferedReader;

class MailUtil
{
   public static final void sendLine(Writer writer, String line) throws IOException
   {
      System.out.println("S: " + line);

      writer.write(line + "\r\n");
   }

   public static final String readLine(BufferedReader reader) throws IOException
   {
      String line = reader.readLine();

      System.out.println("R: " + line);

      return line;
   }
}
