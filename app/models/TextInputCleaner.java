package models;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-08-17
 * Time: 1:08 PM
 */
public class TextInputCleaner {

    static final String[] stopwords =
    {"i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now"
    };

    public static String clean(String input)
    {
        HashSet<String> stoplist = new HashSet<String>();
        for (int i = 0; i < stopwords.length; i++)
        {
            stoplist.add (stopwords[i]);
        }
        String[] tokenSeq = input.toLowerCase().split("\\s");
        ArrayList<String> outputList = new ArrayList<String>();

        for(String str : tokenSeq)
        {
            if(!stoplist.contains(str))
            {
                outputList.add(str);
            }
        }

        String outStr = StringUtils.join(outputList, " ").replaceAll("[\\W_]+", "");

        return outStr;
    }
}
