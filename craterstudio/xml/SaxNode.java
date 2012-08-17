package craterstudio.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Created on Nov 9, 2009
 */

public class SaxNode
{
   public SaxNode(SaxNode parent, String tagName)
   {
      this.tagName = tagName;
      this.children = new ArrayList<Object>();
      this.attributes = new HashMap<String, String>();

      this.parent = parent;

      if (this.parent != null)
      {
         this.parent.children.add(this);
      }
   }

   //

   void cleanup()
   {
      // concatenate and trim text elements

      List<Object> replace = new ArrayList<Object>();

      StringBuilder conseq = null;

      for (Object child : this.children)
      {
         if (child instanceof SaxNode)
         {
            if (conseq != null)
            {
               String text = conseq.toString().trim();
               if (!text.isEmpty())
                  replace.add(text);
               conseq = null;
            }

            replace.add(child);
            ((SaxNode) child).cleanup();
         }
         else if (child instanceof String)
         {
            if (conseq == null)
               conseq = new StringBuilder();
            conseq.append((String) child);
         }
         else
         {
            throw new IllegalStateException();
         }
      }

      if (conseq != null)
      {
         String text = conseq.toString().trim();
         if (!text.isEmpty())
            replace.add(text);
         conseq = null;
      }

      this.children.clear();
      this.children.addAll(replace);
   }

   //

   private final String tagName;

   public final String getTagName()
   {
      return this.tagName;
   }

   //

   private SaxNode parent;

   public final SaxNode getParent()
   {
      return this.parent;
   }

   public final SaxNode getRoot()
   {
      SaxNode node = this;
      for (SaxNode parent; (parent = node.getParent()) != null; node = parent)
         continue;
      return node;
   }

   private final List<Object>  children;

   //

   private Map<String, String> attributes;

   public Map<String, String> attributes()
   {
      return this.attributes;
   }

   //

   void appendTextNode(String text)
   {
      this.children.add(text);
   }

   //

   public List<SaxNode> nodes()
   {
      List<SaxNode> children = new ArrayList<SaxNode>();
      for (Object obj : this.children)
         if (obj instanceof SaxNode)
            children.add((SaxNode) obj);
      return children;
   }

   public List<Object> children()
   {
      List<Object> children = new ArrayList<Object>();
      children.addAll(this.children);
      return children;
   }

   public String toString()
   {
      return "SaxNode[" + this.tagName + "]";
   }

   public String toTreeString()
   {
      StringBuilder builder = new StringBuilder();
      this.toTreeStringImpl(builder, 0);
      return builder.toString();
   }

   private void toTreeStringImpl(StringBuilder builder, int level)
   {
      for (int i = 0; i < level; i++)
         builder.append('\t');
      builder.append("<").append(this.tagName).append(">\r\n");

      for (Object child : this.children)
      {
         if (child instanceof SaxNode)
         {
            ((SaxNode) child).toTreeStringImpl(builder, level + 1);
         }
         else if (child instanceof String)
         {
            String text = (String) child;
            for (int i = 0; i <= level; i++)
               builder.append('\t');
            builder.append("text:len=" + text.length());
            builder.append("\r\n");
         }
      }

      for (int i = 0; i < level; i++)
         builder.append('\t');
      builder.append("</").append(this.tagName).append(">\r\n");
   }

   // utility stuff

   public SaxNode traverseFirst(String... tagNames)
   {
      SaxNode node = this;
      for (String tagName : tagNames)
         if ((node = node.findFirstNode(tagName)) == null)
            return null;
      return node;
   }

   public List<SaxNode> traverseGraph(String... tagNames)
   {
      List<SaxNode> matches = new ArrayList<SaxNode>();

      matches.add(this);

      for (String tagName : tagNames)
      {
         List<SaxNode> next = new ArrayList<SaxNode>();
         for (SaxNode curr : matches)
            next.addAll(curr.findNodes(tagName));
         matches = next;
      }

      return matches;
   }

   //

   public List<String> findText()
   {
      List<String> texts = new ArrayList<String>();
      for (Object obj : this.children)
         if (obj instanceof String)
            texts.add((String) obj);
      return texts;
   }

   public String findFirstText()
   {
      for (Object obj : this.children)
         if (obj instanceof String)
            return (String) obj;
      return null;
   }

   public String findLastText()
   {
      // TODO: traverse from end to begin
      String last = null;
      for (Object obj : this.children)
         if (obj instanceof String)
            last = (String) obj;
      return last;
   }

   //

   public List<SaxNode> findNodes(String tagName)
   {
      List<SaxNode> children = new ArrayList<SaxNode>();
      for (Object obj : this.children)
         if (obj instanceof SaxNode)
            if (match((SaxNode) obj, tagName))
               children.add((SaxNode) obj);
      return children;
   }

   public SaxNode findFirstNode(String tagName)
   {
      for (Object obj : this.children)
         if (obj instanceof SaxNode)
            if (match((SaxNode) obj, tagName))
               return (SaxNode) obj;
      return null;
   }

   public SaxNode findLastNode(String tagName)
   {
      // TODO: traverse from end to begin
      SaxNode last = null;
      for (Object obj : this.children)
         if (obj instanceof SaxNode)
            if (match((SaxNode) obj, tagName))
               last = (SaxNode) obj;
      return last;
   }

   static boolean match(SaxNode node, String tagName)
   {
      if (tagName.equals("*"))
         return true;
      if (tagName.startsWith("*"))
         return node.getTagName().endsWith(tagName.substring(1));
      if (tagName.endsWith("*"))
         return node.getTagName().startsWith(tagName.substring(tagName.length() - 1));
      return node.getTagName().equals(tagName);
   }
}
