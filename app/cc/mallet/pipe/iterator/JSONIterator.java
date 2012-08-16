package cc.mallet.pipe.iterator;

import cc.mallet.types.Instance;
import org.codehaus.jackson.JsonNode;

import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-08-15
 * Time: 10:53 AM
 */
public class JsonIterator implements Iterator<Instance> {

    private JsonNode jsonData;
    private Iterator<JsonNode> jsonIterator;

    public JsonIterator(JsonNode jsonData)
    {
        this.jsonData = jsonData;
        this.jsonIterator = jsonData.iterator();
    }

    public Instance next()
    {
        JsonNode currentNode = jsonIterator.next();
        String name = currentNode.get("name").getTextValue();
        String text = currentNode.get("text").getTextValue();
        String group = currentNode.get("group").getTextValue();

        Instance carrier = new Instance(text, group, name, null);
        return carrier;
    }

    public boolean hasNext()
    {
        return jsonIterator.hasNext();
    }

    public void remove ()
    {
        throw new IllegalStateException ("This Iterator<Instance> does not support remove().");
    }

}
