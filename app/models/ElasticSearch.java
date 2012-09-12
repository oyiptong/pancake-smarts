package models;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;

/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-09-12
 * Time: 10:09 AM
 */
public class ElasticSearch
{
    private static ElasticSearch ref;
    private Node node;

    private ElasticSearch()
    {
        node = nodeBuilder().clusterName("pancake").client(true).node();
    }

    public static ElasticSearch getElasticSearch()
    {
        if (ref == null)
        {
            ref = new ElasticSearch();
        }
        return ref;
    }

    public Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    public Client getClient()
    {
        return node.client();
    }

    public void shutdown()
    {
        node.close();
        ref = null;
    }
}
