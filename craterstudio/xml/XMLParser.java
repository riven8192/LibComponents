package craterstudio.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Created on 2 dec 2007
 */

public class XMLParser
{
   public final void process(XMLCallback callback, String txt)
   {
      for (int i = 0; i < txt.length(); i++)
      {
         char c = txt.charAt(i);
         if (c != '<')
            continue;

         String text = txt.substring(0, i);//.trim();
         if (text.trim().length() != 0)
            callback.handleTextElement(text);

         txt = this.processTag(callback, txt.substring(i + 1));
         i = -1; // becomes 0
      }
   }

   private final String processTag(XMLCallback callback, String txt)
   {
      int delim1 = indexOfWhiteSpace(txt);
      int delim2 = txt.indexOf('>');

      if (delim1 == delim2) // both -1
         throw new IllegalStateException("");

      int tagNameEnd = (delim1 == -1) ? delim2 : ((delim2 == -1) ? delim1 : Math.min(delim1, delim2));
      if (tagNameEnd == -1)
         throw new IllegalStateException();

      if (txt.charAt(0) == '/')
      {
         callback.handleCloseTag(txt.substring(1, tagNameEnd));
      }
      else if (txt.charAt(delim2 - 1) == '/')
      {
         String attr = txt.substring(tagNameEnd, delim2);
         Map<String, String> map = this.processAttr(attr);
         String tagName = txt.substring(0, tagNameEnd - (attr.length() == 0 ? 1 : 0)); // ??
         callback.handleOpenTag(tagName, map);
         callback.handleCloseTag(tagName);
      }
      else
      {
         Map<String, String> map = this.processAttr(txt.substring(tagNameEnd, delim2));
         callback.handleOpenTag(txt.substring(0, tagNameEnd), map);
      }

      return txt.substring(delim2 + 1);
   }

   private final Map<String, String> processAttr(String txt)
   {
      Map<String, String> map = new HashMap<String, String>();

      String[] parts = split(txt, '\"');

      for (int i = 0; i < parts.length - 1; i++)
      {
         String key = parts[i].trim();

         if (key.indexOf('=') != key.lastIndexOf('='))
            throw new IllegalStateException();
         if (key.indexOf('=') != key.length() - 1)
            throw new IllegalStateException();

         map.put(key.substring(0, key.length() - 1).trim(), parts[++i]);
      }

      return map;
   }

   static final int indexOfWhiteSpace(String txt)
   {
      for (int i = 0; i < txt.length(); i++)
         if (Character.isWhitespace(txt.charAt(i)))
            return i;
      return -1;
   }

   static final String[] split(String txt, char c)
   {
      List<String> parts = new ArrayList<String>();

      int last = -1;
      for (int i = 0; i < txt.length(); i++)
      {
         if (txt.charAt(i) != c)
            continue;
         parts.add(txt.substring(last + 1, i));
         last = i;
      }

      if (last != txt.length() - 1)
         parts.add(txt.substring(last + 1));
      return parts.toArray(new String[parts.size()]);
   }
}