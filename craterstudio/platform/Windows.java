/*
 * Created on 2 okt 2008
 */

package craterstudio.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.NoPermissionException;

import craterstudio.io.Streams;
import craterstudio.streams.NullOutputStream;
import craterstudio.text.Text;

public class Windows
{
   private static final String USER_REG_KEY;

   static
   {
      String a = "";
      a += "HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\";
      a += "CurrentVersion\\Explorer\\User Shell Folders";
      USER_REG_KEY = a;
   }

   public static void executeFile(File file)
   {
      if (file == null)
         throw new NullPointerException();
      if (!file.exists())
         throw new IllegalArgumentException("file does not exist: '" + file.getAbsolutePath() + "'");
      if (file.isDirectory())
         throw new IllegalArgumentException("cannot execute a directory: '" + file.getAbsolutePath() + "'");

      try
      {
         Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler \"" + file.getAbsoluteFile() + "\"");
      }
      catch (Exception exc)
      {
         exc.printStackTrace();
      }
   }

   public static void addToRegistry(String path, String type, String key, String value)
   {
      List<String> cmds = new ArrayList<String>();
      cmds.add("reg");
      cmds.add("ADD");
      cmds.add("\"" + Text.replace(path, "\"", "\\\"") + "\"");

      cmds.add("/f");

      if (key == null)
      {
         cmds.add("/ve");
      }
      else
      {
         cmds.add("/v");
         cmds.add("\"" + Text.replace(key, "\"", "\\\"") + "\"");
      }

      cmds.add("/t");
      cmds.add(type);

      cmds.add("/d");
      cmds.add("\"" + Text.replace(value, "\"", "\\\"") + "\"");

      try
      {
         String[] cmdArray = cmds.toArray(new String[cmds.size()]);
         Process p = Runtime.getRuntime().exec(cmdArray);

         Streams.asynchronousTransfer(p.getInputStream(), new NullOutputStream(), true, false);
         Streams.asynchronousTransfer(p.getErrorStream(), System.err, true, false);

         p.waitFor();
      }
      catch (Exception exc)
      {
         exc.printStackTrace();
      }
   }

   public static void createShortcut(File XXMKLINK_EXE, File out, File target, String[] args, File startin, String desc, int mode, File icon)
   {
      try
      {
         List<String> cmds = new ArrayList<String>();
         cmds.add(XXMKLINK_EXE.getAbsolutePath());
         cmds.add(out.getAbsolutePath());
         cmds.add(target.getAbsolutePath());

         StringBuilder sb = new StringBuilder();
         sb.append("\"");
         if (args != null)
         {
            for (String arg : args)
               sb.append(Text.replace(arg, "\"", "\\\"")).append(" ");
            if (sb.length() > 2)
               sb.setLength(sb.length() - 1);
         }
         sb.append("\"");
         cmds.add(sb.toString());

         if (startin == null)
            startin = target.getParentFile();
         cmds.add(startin.getAbsolutePath());
         if (desc != null)
            cmds.add(desc);

         if (mode == -1)
            mode = 1;
         cmds.add(String.valueOf(mode));
         if (icon != null)
            cmds.add(icon.getAbsolutePath());
         cmds.add("/q");

         Runtime.getRuntime().exec(cmds.toArray(new String[cmds.size()])).waitFor();
      }
      catch (Exception exc)
      {
         throw new IllegalStateException(exc);
      }
   }

   public static final Map<String, String> getUserFolders()
   {
      Map<String, String> map = Windows.queryRegister(USER_REG_KEY);

      for (Entry<String, String> entry : map.entrySet())
      {
         // resolve environment variables
         String[] parts = Text.split(entry.getValue(), '%');
         for (int i = 1; i < parts.length; i += 2)
            parts[i] = System.getenv(parts[i]);
         entry.setValue(Text.join(parts));
      }

      return map;
   }

   public static final String getUserDesktopFolder()
   {
      return Windows.getUserFolders().get("Desktop");
   }

   public static final String getUserDocumentsFolder()
   {
      return Windows.getUserFolders().get("Personal");
   }

   public static final String getUserApplicationDataFolder()
   {
      return Windows.getUserFolders().get("AppData");
   }

   public static final String getUserCookiesFolder()
   {
      return Windows.getUserFolders().get("Cookies");
   }

   public static final Map<String, String> queryRegister(String key)
   {
      HashMap<String, String> map = new HashMap<String, String>();

      Process p = null;
      BufferedReader br = null;
      try
      {
         p = Runtime.getRuntime().exec(new String[] { "reg", "query", key });
         br = new BufferedReader(new InputStreamReader(p.getInputStream()));

         // wait for Key
         while (true)
         {
            String line = br.readLine();
            System.out.println("LINE: [" + line + "]");
            if (line == null)
               throw new IllegalStateException("Key line not found");
            if (line.equalsIgnoreCase(key))
               break;
         }

         // read results
         while (true)
         {
            String line = br.readLine();
            System.out.println("LINE: [" + line + "]");
            if (line == null)
               break;
            if (line.length() == 0)
               break;

            // {white}KEY{white}REG_[EXPAND_]SZ{white}VAL

            String[] pair;

            if (line.contains("REG_EXPAND_SZ"))
               pair = Text.splitPair(line, "REG_EXPAND_SZ");
            else if (line.contains("REG_SZ"))
               pair = Text.splitPair(line, "REG_SZ");
            else
               break;

            map.put(pair[0].trim(), pair[1].trim());
         }
      }
      catch (Exception exc)
      {
         exc.printStackTrace();
      }
      finally
      {
         p.destroy();
         Streams.safeClose(br);
      }

      return map;
   }
}
