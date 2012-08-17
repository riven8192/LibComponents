/*
 * Created on 25 jan 2010
 */

package craterstudio.net.smtp;

import java.util.ArrayList;
import java.util.List;

import craterstudio.text.Text;

public class SmtpContact
{
   public static String toSmtpString(List<SmtpContact> contacts)
   {
      if (contacts.isEmpty())
         return null;

      String concat = "";
      for (SmtpContact contact : contacts)
         concat += contact.toSmtpString() + ", ";
      return Text.chopLast(concat, 2);
   }

   public static SmtpContact fromSmtpString(String input)
   {
      if (input.startsWith("\""))
         return new SmtpContact(Text.between(input, '"', '"'), Text.between(input, '<', '>'));
      return new SmtpContact(Text.between(input, '<', '>'));
   }

   public static List<SmtpContact> fromSmtpStrings(String input)
   {
      List<SmtpContact> contacts = new ArrayList<SmtpContact>();
      if (!input.isEmpty())
         for (String i : Text.split(input, ", "))
            contacts.add(fromSmtpString(i));
      return contacts;
   }

   public final String name, address;

   public SmtpContact(String address)
   {
      this(null, address);
   }

   public SmtpContact(String name, String address)
   {
      if (name != null)
      {
         if (name.trim().isEmpty())
            throw new IllegalArgumentException("empty name");
         if (name.indexOf('"') != -1)
            throw new IllegalArgumentException("quote in name");
         if (name.indexOf('\n') != -1)
            throw new IllegalArgumentException("newline");
         if (name.indexOf('\r') != -1)
            throw new IllegalArgumentException("newline");
         if (name.indexOf('\t') != -1)
            throw new IllegalArgumentException("newline");
         if (name.indexOf('<') != -1)
            throw new IllegalArgumentException("newline");
         if (name.indexOf('>') != -1)
            throw new IllegalArgumentException("newline");
      }

      if (address == null)
         throw new IllegalArgumentException("address is null");
      if (address.indexOf('@') == -1)
         throw new IllegalArgumentException("missing @ in address");

      if (address.indexOf('"') != -1)
         throw new IllegalArgumentException("quote in name");
      if (address.indexOf('\n') != -1)
         throw new IllegalArgumentException("newline");
      if (address.indexOf('\r') != -1)
         throw new IllegalArgumentException("newline");
      if (address.indexOf('\t') != -1)
         throw new IllegalArgumentException("newline");
      if (address.indexOf('<') != -1)
         throw new IllegalArgumentException("newline");
      if (address.indexOf('>') != -1)
         throw new IllegalArgumentException("newline");

      this.name = name;
      this.address = address;
   }

   public String toSmtpString()
   {
      if (name == null)
         return "<" + address + ">";
      return "\"" + name + "\" <" + address + ">";
   }

   @Override
   public String toString()
   {
      return "SmtpContact[" + this.toSmtpString() + "]";
   }
}