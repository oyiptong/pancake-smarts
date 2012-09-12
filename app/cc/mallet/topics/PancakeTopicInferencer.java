package cc.mallet.topics;

import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.Randoms;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-08-15
 * Time: 9:37 AM
 */
public class PancakeTopicInferencer extends TopicInferencer {

    Alphabet alphabet;
    double smoothingOnlyMass = 0.0;
    double[] cachedCoefficients;

    public PancakeTopicInferencer(int[][] typeTopicCounts, int[] tokensPerTopic, Alphabet alphabet, double[] alpha, double beta, double betaSum) {
        super(typeTopicCounts, tokensPerTopic, alphabet, alpha, beta, betaSum);
    }

    public List<List> inferSortedDistributions(InstanceList instances, int numIterations, int thinning, int burnIn, double threshold, int max){

        IDSorter[] sortedTopics = new IDSorter[ numTopics ];
        for (int topic = 0; topic < numTopics; topic++) {
            // Initialize the sorters with dummy values
            sortedTopics[topic] = new IDSorter(topic, topic);
        }

        if (max < 0 || max > numTopics) {
            max = numTopics;
        }

        ArrayList<List> output = new ArrayList<List>();

        for (Instance instance: instances) {
            ArrayList document = new ArrayList();
            ArrayList<List> topTopics = new ArrayList<List>();

            double[] topicDistribution =
                    getSampledDistribution(instance, numIterations,
                            thinning, burnIn);

            for (int topic = 0; topic < numTopics; topic++) {
                sortedTopics[topic].set(topic, topicDistribution[topic]);
            }
            Arrays.sort(sortedTopics);

            for (int i = 0; i < max; i++) {
                if (sortedTopics[i].getWeight() < threshold) { break; }
                ArrayList topicDist = new ArrayList();
                topicDist.add(sortedTopics[i].getID());
                topicDist.add(sortedTopics[i].getWeight());

                topTopics.add(topicDist);

            }
            document.add(instance.getName());
            document.add(topTopics);
            output.add(document);
        }
        return output;
    }

    public List<List> inferDistributions(InstanceList instances, int numIterations, int thinning, int burnIn, double threshold){

        ArrayList<List> output = new ArrayList<List>();

        for (Instance instance: instances) {
            ArrayList document = new ArrayList();

            double[] topicDistribution =
                    getSampledDistribution(instance, numIterations, thinning, burnIn);


            document.add(instance.getName());
            document.add(topicDistribution);
            output.add(document);
        }
        return output;
    }

    // Serialization

    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;
    private static final int NULL_INTEGER = -1;

    private void writeObject (ObjectOutputStream out) throws IOException {
        out.writeInt (CURRENT_SERIAL_VERSION);

        out.writeObject(alphabet);

        out.writeInt(numTopics);

        out.writeInt(topicMask);
        out.writeInt(topicBits);

        out.writeInt(numTypes);

        out.writeObject(alpha);
        out.writeDouble(beta);
        out.writeDouble(betaSum);

        out.writeObject(typeTopicCounts);
        out.writeObject(tokensPerTopic);

        out.writeObject(random);

        out.writeDouble(smoothingOnlyMass);
        out.writeObject(cachedCoefficients);
    }

    private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {

        int version = in.readInt ();

        alphabet = (Alphabet) in.readObject();

        numTopics = in.readInt();

        topicMask = in.readInt();
        topicBits = in.readInt();

        numTypes = in.readInt();

        alpha = (double[]) in.readObject();
        beta = in.readDouble();
        betaSum = in.readDouble();

        typeTopicCounts = (int[][]) in.readObject();
        tokensPerTopic = (int[]) in.readObject();

        random = (Randoms) in.readObject();

        smoothingOnlyMass = in.readDouble();
        cachedCoefficients = (double[]) in.readObject();
    }


}
