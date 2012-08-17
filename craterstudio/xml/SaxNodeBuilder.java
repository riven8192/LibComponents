package craterstudio.xml;

/*
 * Created on Nov 9, 2009
 */

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

public class SaxNodeBuilder extends DefaultHandler
{
   public static SaxNode build(InputStream in) throws IOException, SAXException
   {
      XMLReader xr = XMLReaderFactory.createXMLReader();

      SaxNodeBuilder handler = new SaxNodeBuilder();
      xr.setContentHandler(handler);
      xr.setErrorHandler(handler);

      xr.parse(new InputSource(in));

      return handler.root;
   }

   //

   @Override
   public void warning(SAXParseException e) throws SAXException
   {
      e.printStackTrace();
   }

   @Override
   public void error(SAXParseException e) throws SAXException
   {
      e.printStackTrace();
   }

   @Override
   public void fatalError(SAXParseException e) throws SAXException
   {
      e.printStackTrace();
   }

   @Override
   public void skippedEntity(String name) throws SAXException
   {
      System.err.println(name);
   }

   @Override
   public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) throws SAXException
   {
      System.err.println(name);
   }

   //

   public void startDocument()
   {
      this.root = null;
   }

   public void endDocument()
   {
      if (this.current != null)
      {
         throw new IllegalStateException();
      }

      // this.root.cleanup();
   }

   private SaxNode root;
   private SaxNode current;

   public void startElement(String uri, String name, String qName, Attributes atts)
   {
      System.out.println("startElement:" + name);

      this.current = new SaxNode(this.current, qName);

      for (int i = 0; i < atts.getLength(); i++)
         if (this.current.attributes().put(atts.getQName(i), atts.getValue(i)) != null)
            throw new IllegalStateException("duplicate attribute: " + atts.getQName(i));

      if (this.root == null)
         this.root = this.current;
   }

   public void endElement(String uri, String name, String qName)
   {
      this.current = this.current.getParent();
   }

   public void characters(char ch[], int start, int length)
   {
      this.current.appendTextNode(new String(ch, start, length));
   }
}