package craterstudio.xml;
import java.util.Map;

/*
 * Created on 2 dec 2007
 */

public interface XMLCallback
{
   public void handleOpenTag(String tagName, Map<String, String> attributes);

   public void handleCloseTag(String tagName);
   
   public void handleTextElement(String text);
}
