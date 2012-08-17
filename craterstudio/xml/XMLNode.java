package craterstudio.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import craterstudio.text.TextEscape;
import craterstudio.util.IteratorUtil;
import craterstudio.util.NonNullHashMap;

/*
 * Created on 2 dec 2007
 */

public class XMLNode implements Iterable<XMLNode>
{
   private static final int  DEBUG_LEVEL   = 3;

   // navigation
   private XMLNode           parent;
   private XMLNode           prevSibling, nextSibling;
   private XMLNode           firstChild, lastChild;

   private int               childrenCount = 0;

   // contents
   private final String      tagName;
   String                    text;
   final Map<String, String> attributes;

   public XMLNode(String tagName)
   {
      this(tagName, null);
   }

   public XMLNode(String tagName, XMLNode parent)
   {
      this.tagName = tagName;
      this.attributes = new NonNullHashMap<String, String>();

      if (parent != null)
      {
         parent.appendChild(this);
      }
   }

   public final String getTagName()
   {
      return tagName;
   }

   // attributes

   public final Map<String, String> attributes()
   {
      return attributes;
   }

   public boolean hasAttribute(String key)
   {
      return this.attributes.containsKey(key);
   }

   public String getAttribute(String key)
   {
      if (this.hasAttribute(key))
         return this.attributes.get(key);
      return null;
   }

   public String getAttribute(String key, String def)
   {
      if (this.hasAttribute(key))
         return this.attributes.get(key);
      return def;
   }

   public int getAttributeAsInt(String key, int def)
   {
      if (this.hasAttribute(key))
         return Integer.parseInt(this.attributes.get(key));
      return def;
   }

   public boolean getAttributeAsBoolean(String key, boolean def)
   {
      if (this.hasAttribute(key))
         return Boolean.parseBoolean(this.attributes.get(key));
      return def;
   }

   //

   public String getChildText(String tagName, String def)
   {
      Iterator<XMLNode> it = this.findAllByTagName(tagName);
      if (!it.hasNext())
         return def;
      XMLNode child = it.next();
      if (it.hasNext())
         throw new IllegalStateException("more than one '" + tagName + "' child found");
      return child.getText().trim();
   }

   public int getChildTextAsInt(String tagName, int def)
   {
      Iterator<XMLNode> it = this.findAllByTagName(tagName);
      if (!it.hasNext())
         return def;
      XMLNode child = it.next();
      if (it.hasNext())
         throw new IllegalStateException("more than one '" + tagName + "' child found");
      return Integer.parseInt(child.getText().trim());
   }

   public boolean getChildTextAsBoolean(String tagName, boolean def)
   {
      Iterator<XMLNode> it = this.findAllByTagName(tagName);
      if (!it.hasNext())
         return def;
      XMLNode child = it.next();
      if (it.hasNext())
         throw new IllegalStateException("more than one '" + tagName + "' child found");
      return Boolean.parseBoolean(child.getText().trim());
   }

   //

   public XMLNode parent()
   {
      return parent;
   }

   public XMLNode prevSibling()
   {
      return prevSibling;
   }

   public XMLNode nextSibling()
   {
      return nextSibling;
   }

   public void detach()
   {
      if (this.parent != null)
         this.parent.removeChild(this);
      this.prevSibling = null;
      this.nextSibling = null;
   }

   //

   public void prependChild(XMLNode node)
   {
      this.mustNotContain(node);
      this.insertImpl(null, node, this.firstChild);
   }

   public void appendChild(XMLNode node)
   {
      this.mustNotContain(node);
      this.insertImpl(this.lastChild, node, null);
   }

   public void insertChildBefore(XMLNode node, XMLNode find)
   {
      this.mustContain(find);
      this.mustNotContain(node);

      node.detach();

      for (XMLNode next : this)
      {
         if (next == find)
         {
            this.insertImpl(next.prevSibling, node, next);
            return;
         }
      }

      throw new IllegalStateException("this should never happen: child-of-parent not in parent");
   }

   public void insertChildAfter(XMLNode node, XMLNode find)
   {
      this.mustContain(find);
      this.mustNotContain(node);

      node.detach();

      for (XMLNode prev : this)
      {
         if (prev == find)
         {
            this.insertImpl(prev, node, prev.nextSibling);
            return;
         }
      }

      throw new IllegalStateException("this should never happen: child-of-parent not in parent");
   }

   public void removeChild(XMLNode child)
   {
      this.removeImpl(child);
   }

   public Iterator<XMLNode> iterator()
   {
      return new Iterator<XMLNode>()
      {
         XMLNode next         = XMLNode.this.firstChild();
         XMLNode lastReturned = null;

         @Override
         public boolean hasNext()
         {
            return (this.next != null);
         }

         @Override
         public XMLNode next()
         {
            if (!this.hasNext())
               throw new NoSuchElementException();

            this.lastReturned = this.next;
            this.next = this.next.nextSibling();
            return this.lastReturned;
         }

         @Override
         public void remove()
         {
            if (this.lastReturned == null)
               throw new NoSuchElementException("nothing to remove");

            XMLNode.this.removeChild(this.lastReturned);

            this.lastReturned = null;
         }
      };
   }

   public boolean containsChild(XMLNode node)
   {
      return node.parent == this;
   }

   private final void mustContain(XMLNode node)
   {
      if (!this.containsChild(node))
         throw new IllegalStateException("node \"" + node + "\" must be child of \"" + this + "\", not \"" + node.parent() + "\"");
   }

   private final void mustNotContain(XMLNode node)
   {
      if (this.containsChild(node))
         throw new IllegalStateException("node \"" + node + "\" already is child of \"" + this + "\"");
   }

   public int indexOfChild(XMLNode node)
   {
      if (node == null)
         return -1;

      if (node.parent != this)
         return -1;

      int counter = 0;
      for (XMLNode child : this)
      {
         if (child == node)
            return counter;
         counter++;
      }

      return -1;
   }

   public XMLNode childAt(int index)
   {
      if (index < 0 || index >= childrenCount)
         throw new IllegalArgumentException("index out of bounds: " + index + " / " + childrenCount);

      if (index <= this.childrenCount / 2)
      {
         XMLNode node = this.firstChild;
         while (node != null)
         {
            if ((index--) == 0)
               return node;
            node = node.nextSibling;
         }

         throw new IllegalStateException("this should never happen");
      }
      // else, search from last-to-first
      {
         index = (this.childrenCount - 1) - index;
         XMLNode node = this.lastChild;
         while (node != null)
         {
            if ((index--) == 0)
               return node;
            node = node.prevSibling;
         }

         throw new IllegalStateException("this should never happen");
      }
   }

   public XMLNode findByTagName(String name)
   {
      for (XMLNode node : this)
         if (node.getTagName().equals(name))
            return node;
      return null;
   }

   public XMLNode findByTagName(String name, int occurence)
   {
      for (XMLNode node : this)
         if (node.getTagName().equals(name) && (occurence-- == 0))
            return node;
      return null;
   }

   public Iterable<XMLNode> foreachTagName(String name)
   {
      return IteratorUtil.foreach(this.findAllByTagName(name));
   }

   public Iterator<XMLNode> findAllByTagName(final String name)
   {
      return new Iterator<XMLNode>()
      {
         Iterator<XMLNode> backing      = XMLNode.this.iterator();
         XMLNode           match        = null;
         XMLNode           lastReturned = null;

         @Override
         public boolean hasNext()
         {
            if (this.match != null)
               return true;

            while (backing.hasNext())
            {
               XMLNode maybe = backing.next();
               if (maybe.getTagName().equals(name))
               {
                  this.match = maybe;
                  break;
               }
            }
            return this.match != null;
         }

         @Override
         public XMLNode next()
         {
            if (!this.hasNext())
               throw new NoSuchElementException();

            if (this.match == null)
               throw new IllegalStateException("this should never happen");

            // consume match
            this.lastReturned = this.match;
            this.match = null;
            return this.lastReturned;
         }

         @Override
         public void remove()
         {
            if (this.lastReturned == null)
               throw new IllegalStateException("nothing to remove");

            XMLNode.this.removeChild(this.lastReturned);

            this.lastReturned = null;
         }
      };
   }

   public XMLNode firstChild()
   {
      return this.firstChild;
   }

   public XMLNode lastChild()
   {
      return this.lastChild;
   }

   public int childrenCount()
   {
      return childrenCount;
   }

   public void setText(String text)
   {
      this.text = text;
   }

   public String getText()
   {
      if (text == null)
         return "";
      return text;
   }

   public final void clear()
   {
      Iterator<XMLNode> it = this.iterator();
      while (it.hasNext())
      {
         it.next();
         it.remove();
      }
      this.attributes().clear();
   }

   public void duplicateFrom(XMLNode that)
   {
      this.clear();

      // copy thatNode
      that = that.deepCopy();

      // transfer thatNode-children into thisNode-children
      Iterator<XMLNode> it = that.iterator();
      while (it.hasNext())
      {
         XMLNode child = it.next();
         child.detach();
         this.appendChild(child);
      }

      // copy properties
      this.attributes().putAll(that.attributes());
   }

   //

   public String print()
   {
      return this.print(false);
   }

   public String print(boolean withHeader)
   {
      StringBuilder out = new StringBuilder();
      this.print(out, 0, 1, '\t');

      String xml = out.toString();
      if (withHeader)
         xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + xml;
      return xml;
   }

   public void print(StringBuilder out, char indentChar)
   {
      this.print(out, 0, 1, indentChar);
   }

   public void print(StringBuilder out, int indent, int indentStep, char indentChar)
   {
      boolean isHeader = tagName.startsWith("?");

      out.append(this.indent(indent, indentChar) + "<" + TextEscape.escapeForXmlAttribute(tagName));
      for (String key : attributes.keySet())
         out.append(" " + TextEscape.escapeForXmlAttribute(key) + "=\"" + TextEscape.escapeForXmlAttribute(attributes.get(key)) + "\"");

      if (this.childrenCount() == 0 && text == null)
      {
         out.append("/>").append("\r\n");
      }
      else if (text != null)
      {
         out.append(">" + TextEscape.escapeForXmlText(text) + "</" + TextEscape.escapeForXmlAttribute(tagName) + ">").append("\r\n");
      }
      else
      {
         if (isHeader)
            out.append('?');
         out.append(">").append("\r\n");
         for (XMLNode node : this)
            node.print(out, indent + (isHeader ? 0 : indentStep), indentStep, indentChar);
         String line = this.indent(indent, indentChar);
         if (!isHeader)
            line += "</" + TextEscape.escapeForXmlAttribute(tagName) + ">";
         out.append(line).append("\r\n");
      }
   }

   private String indent(int indent, char c)
   {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < indent; i++)
         sb.append(c);
      return sb.toString();
   }

   public String toString()
   {
      return "XMLNode[\"" + this.tagName + "\", hash=" + this.hashCode() + "]";
   }

   private final void insertImpl(XMLNode prev, XMLNode toInsert, XMLNode next)
   {
      this.mustNotContain(toInsert);

      if (prev != null)
         this.mustContain(prev);
      if (next != null)
         this.mustContain(next);

      if (DEBUG_LEVEL >= 1)
      {
         if (prev == null && next == null)
            if (this.firstChild != null || this.lastChild != null)
               throw new IllegalStateException("prev:NULL & next:NULL, while first/last not both null");

         if (prev == null && next != null)
            if (next != this.firstChild)
               throw new IllegalStateException("prev:NULL, next must-be first");

         if (prev != null && next == null)
            if (prev != this.lastChild)
               throw new IllegalStateException("next:NULL, prev must-be last");
      }

      if (DEBUG_LEVEL >= 2)
      {
         int iPrev = this.indexOfChild(prev);
         int iCurr = this.indexOfChild(toInsert);
         int iNext = this.indexOfChild(next);

         if (iCurr != -1)
            throw new IllegalStateException("toInsert already in parent");

         if (prev != null && iPrev == -1)
            throw new IllegalStateException("prev not in parent");
         if (next != null && iNext == -1)
            throw new IllegalStateException("next not in parent");

         if ((prev != null || next != null) && (iPrev == iNext))
            throw new IllegalStateException("prev == next, while not both NULL");
         if ((prev != null && next != null) && (iNext - iPrev != 1))
            throw new IllegalStateException("prev is not exactly before next");
      }

      // insert
      toInsert.parent = this;
      toInsert.nextSibling = next;
      toInsert.prevSibling = prev;

      // connect prev/next
      if (prev != null)
         prev.nextSibling = toInsert;
      if (next != null)
         next.prevSibling = toInsert;

      // fix first/last
      if (prev == null)
         this.firstChild = toInsert;
      if (next == null)
         this.lastChild = toInsert;

      this.childrenCount++;

      if (DEBUG_LEVEL >= 3)
         this.checkIntegrity();
   }

   private final void removeImpl(XMLNode toRemove)
   {
      this.mustContain(toRemove);

      XMLNode prev = toRemove.prevSibling;
      XMLNode next = toRemove.nextSibling;

      if (prev != null)
         prev.nextSibling = next;
      else
         this.firstChild = next;

      if (next != null)
         next.prevSibling = prev;
      else
         this.lastChild = prev;

      toRemove.parent = null;
      toRemove.prevSibling = null;
      toRemove.nextSibling = null;

      this.childrenCount--;

      if (DEBUG_LEVEL >= 3)
         this.checkIntegrity();
   }

   public final void checkIntegrity()
   {
      if (this.firstChild == null && this.lastChild != null)
         throw new IllegalStateException();
      if (this.firstChild != null && this.lastChild == null)
         throw new IllegalStateException();

      if (this.firstChild == null)
         return;

      XMLNode node;

      // check prev/next chain

      LinkedList<XMLNode> first2last = new LinkedList<XMLNode>();
      LinkedList<XMLNode> last2first = new LinkedList<XMLNode>();

      node = this.firstChild;
      while (node != null)
      {
         first2last.add(node);
         node = node.nextSibling;
      }

      node = this.lastChild;
      while (node != null)
      {
         last2first.add(node);
         node = node.prevSibling;
      }

      if (first2last.size() != last2first.size())
         throw new IllegalStateException("prev-chain-length != next-chain-length");

      int len = first2last.size();
      if (len != this.childrenCount)
         throw new IllegalStateException("chain-length != children-count");

      for (int i = 0; i < len; i++)
      {
         XMLNode first = first2last.removeFirst();
         XMLNode last = last2first.removeLast();

         if (first.parent != this)
            throw new IllegalStateException("node-parent != self");

         if (first != last)
            throw new IllegalStateException("prev-chain-nodes != next-chain-nodes");
      }
   }

   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;

      if ((obj == null) || !(obj instanceof XMLNode))
         return false;

      XMLNode that = (XMLNode) obj;
      if (!this.tagName.equals(that.tagName))
         return false;

      if (this.childrenCount != that.childrenCount)
         return false;

      try
      {
         if (!this.attributes.equals(that.attributes))
         {
            return false;
         }
      }
      catch (NoSuchElementException exc)
      {
         return false;
      }

      Iterator<XMLNode> itThis = this.iterator();
      Iterator<XMLNode> itThat = that.iterator();

      for (int i = 0; i < this.childrenCount; i++)
         if (!itThis.next().equals(itThat.next()))
            return false;

      return true;
   }

   public XMLNode deepCopy()
   {
      XMLNode copy = new XMLNode(this.tagName);
      copy.attributes.putAll(this.attributes);
      for (XMLNode child : this)
         copy.appendChild(child.deepCopy());
      return copy;
   }

   private class XMLNodeSnapshot
   {
      XMLNode               reference_node;
      List<XMLNodeSnapshot> snapshot_children;
      Map<String, String>   copyof_attributes;
      String                copyof_text;

      XMLNodeSnapshot(XMLNode node)
      {
         if (node.snapshot != null)
            throw new XMLTransaction("a node has a snapshot while snapshot is taken");

         this.reference_node = node;

         this.copyof_text = node.text;
         this.copyof_attributes = new HashMap<String, String>();
         this.copyof_attributes.putAll(node.attributes);

         this.snapshot_children = new ArrayList<XMLNodeSnapshot>();
         for (XMLNode child : node)
            this.snapshot_children.add(new XMLNodeSnapshot(child));
      }

      XMLNode rollback()
      {
         reference_node.clear();

         reference_node.text = this.copyof_text;
         reference_node.attributes.putAll(this.copyof_attributes);

         for (XMLNodeSnapshot child : this.snapshot_children)
            reference_node.appendChild(child.rollback());

         return reference_node;
      }
   }

   XMLNodeSnapshot snapshot;
   private int     toConsume = 0;
   private int     pending   = 0;
   private boolean shouldRollback;

   public void consumeNextTransaction()
   {
      toConsume++;
   }

   public void newTransaction()
   {
      if (toConsume > 0)
      {
         shouldRollback = false;
         toConsume--;
         pending++;
         return;
      }

      if (snapshot != null)
         throw new XMLTransaction("there already is a transaction active");
      snapshot = new XMLNodeSnapshot(this);
   }

   public void rollback(Throwable cause)
   {
      System.out.println("########################");
      System.out.println("########################");
      cause.printStackTrace(System.out);
      System.out.println("########################");
      System.out.println("########################");

      if (snapshot == null)
         throw new XMLTransaction("cannot rollback without transaction", cause);

      if (pending > 0)
      {
         shouldRollback = true;
         pending--;
         return;
      }

      snapshot.rollback();
      snapshot = null;
   }

   public void commit()
   {
      if (snapshot == null)
         throw new XMLTransaction("cannot commit without transaction");

      if (shouldRollback)
         throw new XMLTransaction("cannot commit, a consumed transaction rolled back");

      if (pending > 0)
      {
         pending--;
         return;
      }

      snapshot = null;
   }

   class XMLTransaction extends RuntimeException
   {
      public XMLTransaction(String msg)
      {
         super(msg);
      }

      public XMLTransaction(String msg, Throwable cause)
      {
         super(msg, cause);
      }
   }
}