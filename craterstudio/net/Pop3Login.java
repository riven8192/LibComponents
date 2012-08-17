/*
 * Created on 29 jun 2009
 */

package craterstudio.net;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Pop3Login
{
   public static void main(String[] args) throws Exception
   {
      int[] input = new int[16];
      for (int i = 0; i < input.length; i++)
         input[i] = i;

      Random r = new Random(12344);
      for (int i = input.length - 1; i >= 0; i--)
      {
         int index = r.nextInt(i + 1);
         int temp = input[i];
         input[i] = input[index];
         input[index] = temp;
      }

      int[] aux = new int[input.length];

      for (int p = 0; p < 256; p++)
      {
         long t0 = System.nanoTime();
         for (int m = 0; m < 16; m++)
         {
            int[] work = input.clone();
            for (int k = 2; k <= input.length; k *= 2)
               for (int i = 0; i < work.length - (k - 1); i += k)
                  merge(k, input, i, aux);
         }
         long t1 = System.nanoTime();
         for (int m = 0; m < 16; m++)
         {
            int[] work = input.clone();
            Arrays.sort(work);
         }
         long t2 = System.nanoTime();

         System.out.println("merge: " + (t1 - t0) + "ns");
         System.out.println("isort: " + (t2 - t1) + "ns");
         System.out.println();
      }
   }

   private static void merge(int size, int[] input, int offset, int[] aux)
   {
      int half = size >> 1;
      int a = offset + 0;
      int b = offset + half;

      for (int i = 0; i < size; i++)
      {
         if (a == (offset + half))
            aux[i] = input[b++];
         else if (b == (offset + size) || input[a] <= input[b])
            aux[i] = input[a++];
         else
            aux[i] = input[b++];
      }

      for (int i = 0; i < size; i++)
      {
         input[offset + i] = aux[i];
      }
   }

   private static void uh() throws Exception
   {
      Pop3 pop = new Pop3("www.kataf.nl");
      pop.login("s.balk@kataf.nl", "RDwW6Ww+");

      List<Pop3.Pop3ListEntry> entries = pop.list();

      System.out.println("emails: " + entries.size());

      for (Pop3.Pop3ListEntry entry : entries)
      {
         System.out.println("entry=" + entry);
         ReceivedMail mail = pop.retr(entry.index);

         System.out.println("from:    " + mail.getFromAddress());
         System.out.println("to:      " + mail.getToAddress());
         System.out.println("subject: " + mail.getSubject());
         System.out.println();
         for (String line : mail.getContentLines())
            System.out.println(line);
         System.out.println();
      }
   }
}
