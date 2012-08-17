package craterstudio.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import craterstudio.io.FileUtil;
import craterstudio.text.Text;
import craterstudio.text.TextEscape;

/*
 * Created on 2 dec 2007
 */

public class XMLBuilder implements XMLCallback
{
   public static XMLNode load(String xml)
   {
      return new XMLBuilder(xml).getRoot();
   }

   public static XMLNode load(File file) throws FileNotFoundException
   {
      String xml = Text.convert(FileUtil.readFile(file));
      if (xml == null)
         throw new FileNotFoundException(file.getAbsolutePath());
      return load(xml);
   }

   private final XMLNode holder      = new XMLNode(null);
   private XMLNode       currentNode = holder;

   public XMLBuilder()
   {
      //
   }

   public XMLBuilder(String xml)
   {
      if (xml == null)
         throw new NullPointerException();

      // strip XML comments
      while (true)
      {
         String beforeOpen = Text.before(xml, "<!--");
         if (beforeOpen == null)
            break;
         String afterOpen = Text.after(xml, "<!--");
         String afterClose = Text.after(afterOpen, "-->");
         if (afterClose == null)
            throw new IllegalStateException("comment not closed");
         xml = beforeOpen + afterClose;
      }

      new XMLParser().process(this, xml);
   }

   public final XMLNode getRoot()
   {
      return holder.firstChild();
   }

   public final void handleOpenTag(String tagName, Map<String, String> map)
   {
      tagName = TextEscape.unescapeForXmlAttribute(tagName);

      Map<String, String> unescapedMap = new HashMap<String, String>();
      for (Entry<String, String> entry : map.entrySet())
      {
         String key = TextEscape.unescapeForXmlAttribute(entry.getKey());
         String val = TextEscape.unescapeForXmlAttribute(entry.getValue());
         unescapedMap.put(key, val);
      }

      XMLNode node = new XMLNode(tagName);

      if (currentNode != null)
         currentNode.appendChild(node);

      currentNode = node;
      currentNode.attributes().putAll(unescapedMap);
   }

   public final void handleCloseTag(String tagName)
   {
      tagName = TextEscape.unescapeForXmlAttribute(tagName);

      if (currentNode == holder)
         throw new IllegalStateException("stack underflow");

      if (!currentNode.getTagName().equals(tagName))
         throw new IllegalStateException("closing " + tagName + " instead of " + currentNode.getTagName());

      currentNode = currentNode.parent();
   }

   public final void handleTextElement(String txt)
   {
      currentNode.setText(TextEscape.unescapeForXmlText(txt));
   }
}