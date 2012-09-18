/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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
