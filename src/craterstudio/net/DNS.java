/*
 * Created on 25 jan 2010
 */

package craterstudio.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import craterstudio.data.tuples.Duo;
import craterstudio.data.tuples.Pair;
import craterstudio.text.Text;
import craterstudio.util.HighLevel;

public class DNS
{
   static final Map<String, List<Pair<String, Integer>>> MX_CACHE;

   static
   {
      MX_CACHE = new HashMap<String, List<Pair<String, Integer>>>();

      final long minutes = 90;

      Runnable task = new Runnable()
      {
         @Override
         public void run()
         {
            while (true)
            {
               HighLevel.sleep(minutes * 60 * 1000);

               synchronized (MX_CACHE)
               {
                  MX_CACHE.clear();
               }
            }
         }
      };

      Thread t = new Thread(task, "DNS cache flusher (interval: " + minutes + "min)");
      t.setDaemon(true);
      t.start();
   }

   public static String getARecord(String domainname)
   {
      try
      {
         return InetAddress.getByName(domainname).getHostAddress();
      }
      catch (UnknownHostException exc)
      {
         return null;
      }
   }

   public static final List<Pair<String, Integer>> getMXRecords(String domainname)
   {
      List<Pair<String, Integer>> result;
      synchronized (MX_CACHE)
      {
         result = MX_CACHE.get(domainname);
      }
      if (result != null)
      {
         // make copy, randomize
         result = new ArrayList<Pair<String, Integer>>(result);
         randomizeRecordsKeepPriority(result);
         return result;
      }

      System.out.println("DNS -->> [" + domainname + "] MX");
      String[] lines = query(domainname, "MX");

      int indexOfAnswer = 0;
      for (; indexOfAnswer < lines.length; indexOfAnswer++)
         if (lines[indexOfAnswer].trim().isEmpty())
            break;

      if (indexOfAnswer >= lines.length - 1)
         return null;

      List<Pair<String, Integer>> domainPriorities;
      domainPriorities = new ArrayList<Pair<String, Integer>>();

      for (indexOfAnswer += 1; indexOfAnswer < lines.length; indexOfAnswer++)
      {
         String line = lines[indexOfAnswer];
         if (line.trim().isEmpty())
            break;
         line = Text.convertWhiteSpaceTo(line, ' ');

         // linux: "kataf.nl        mail exchanger = 10 mail.kataf.nl."
         // winxp: "kataf.nl        MX preference = 10, mail exchanger = mail.kataf.nl"

         int priority;

         if (line.contains("Non-authoritative answer:")) // for linux, for winxp this is in stderr
            continue;
         if (line.contains(" MX preference = ")) // winxp
            priority = Integer.parseInt(Text.between(line, " MX preference = ", ",").trim());
         else if (line.contains(" mail exchanger = ")) // linux
            priority = Integer.parseInt(Text.between(line, " mail exchanger = ", " ").trim());
         else
            break;

         String address = Text.afterLast(line, ' ');
         if (address.endsWith("."))
            address = address.substring(0, address.length() - 1);
         domainPriorities.add(new Pair<String, Integer>(address, Integer.valueOf(priority)));
      }

      synchronized (MX_CACHE)
      {
         randomizeRecordsKeepPriority(domainPriorities);
         MX_CACHE.put(domainname, domainPriorities);
      }

      for (Pair<String, Integer> domainPriority : domainPriorities)
      {
         System.out.println("DNS <<-- [" + domainname + "] MX: " + domainPriority.first() + " pr=" + domainPriority.second());
      }

      return domainPriorities;
   }

   private static void randomizeRecordsKeepPriority(List<Pair<String, Integer>> domainPriorities)
   {
      Collections.shuffle(domainPriorities); // to randomize domains with same priority later on

      Collections.sort(domainPriorities, new Comparator<Pair<String, Integer>>()
      {
         @Override
         public int compare(Pair<String, Integer> a, Pair<String, Integer> b)
         {
            int ia = a.second().intValue();
            int ib = b.second().intValue();
            if (ia < ib)
               return -1;
            if (ia > ib)
               return +1;
            return 0;
         }
      });
   }

   private static final String[] query(String domainname, String queryType)
   {
      try
      {
         Duo<byte[]> std = HighLevel.executeProcess("nslookup", "-q=" + queryType, domainname);
         byte[] stdout = std.first();
         // byte[] stderr = std.second();
         //if (stderr.length != 0)
         // System.err.println(Text.ascii(stderr));
         return Text.splitOnLines(Text.ascii(stdout));
      }
      catch (Exception exc)
      {
         exc.printStackTrace();
         return null;
      }
   }

   public static final String queryHostname()
   {
      try
      {
         Duo<byte[]> std = HighLevel.executeProcess("hostname");
         byte[] stdout = std.first();
         // byte[] stderr = std.second();
         //if (stderr.length != 0)
         // System.err.println(Text.ascii(stderr));
         return Text.splitOnLines(Text.ascii(stdout))[0];
      }
      catch (Exception exc)
      {
         exc.printStackTrace();
         return null;
      }
   }
}