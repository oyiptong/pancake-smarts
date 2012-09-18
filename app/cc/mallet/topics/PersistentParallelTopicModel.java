package cc.mallet.topics;
import cc.mallet.types.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;

/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-08-10
 * Time: 11:35 AM
 */
public class PersistentParallelTopicModel extends ParallelTopicModel {

    // The number of times each type appears in the corpus
    int[] typeTotals;
    // The max over typeTotals, used for beta optimization
    int maxTypeCount;

    int numThreads = 1;

    public PersistentParallelTopicModel(int numberOfTopics) {
        super(numberOfTopics, numberOfTopics, DEFAULT_BETA);
    }

    public PersistentParallelTopicModel(int numberOfTopics, double alphaSum, double beta) {
        super(newLabelAlphabet(numberOfTopics), alphaSum, beta);
        try {
                Handler handler = new FileHandler("training.log", 10485760, 10);
                handler.setFormatter(new SimpleFormatter());
                logger.setUseParentHandlers(false);
                logger.addHandler(handler);
        } catch (IOException e) {
            // default stdout logging
        }
    }

    private static LabelAlphabet newLabelAlphabet (int numTopics) {
        LabelAlphabet ret = new LabelAlphabet();
        for (int i = 0; i < numTopics; i++)
            ret.lookupIndex("topic"+i);
        return ret;
    }

    /**
     *  Gather statistics on the size of documents
     *  and create histograms for use in Dirichlet hyperparameter
     *  optimization.
     */
    private void initializeHistograms() {

        int maxTokens = 0;
        totalTokens = 0;
        int seqLen;

        for (int doc = 0; doc < data.size(); doc++) {
            FeatureSequence fs = (FeatureSequence) data.get(doc).instance.getData();
            seqLen = fs.getLength();
            if (seqLen > maxTokens)
                maxTokens = seqLen;
            totalTokens += seqLen;
        }

        logger.info("max tokens: " + maxTokens);
        logger.info("total tokens: " + totalTokens);

        docLengthCounts = new int[maxTokens + 1];
        topicDocCounts = new int[numTopics][maxTokens + 1];
    }

    // Serialization

    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;
    private static final int NULL_INTEGER = -1;

    private void writeObject (ObjectOutputStream out) throws IOException {
        out.writeInt (CURRENT_SERIAL_VERSION);

        out.writeObject(data);
        out.writeObject(alphabet);
        out.writeObject(topicAlphabet);

        out.writeInt(numTopics);

        out.writeInt(topicMask);
        out.writeInt(topicBits);

        out.writeInt(numTypes);

        out.writeObject(alpha);
        out.writeDouble(alphaSum);
        out.writeDouble(beta);
        out.writeDouble(betaSum);

        out.writeObject(typeTopicCounts);
        out.writeObject(tokensPerTopic);

        out.writeObject(docLengthCounts);
        out.writeObject(topicDocCounts);

        out.writeInt(numIterations);
        out.writeInt(burninPeriod);
        out.writeInt(saveSampleInterval);
        out.writeInt(optimizeInterval);
        out.writeInt(showTopicsInterval);
        out.writeInt(wordsPerTopic);

        out.writeInt(saveStateInterval);
        out.writeObject(stateFilename);

        out.writeInt(saveModelInterval);
        out.writeObject(modelFilename);

        out.writeInt(randomSeed);
        out.writeObject(formatter);
        out.writeBoolean(printLogLikelihood);

        out.writeInt(numThreads);
    }

    private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {

        int version = in.readInt ();

        data = (ArrayList<TopicAssignment>) in.readObject ();
        alphabet = (Alphabet) in.readObject();
        topicAlphabet = (LabelAlphabet) in.readObject();

        numTopics = in.readInt();

        topicMask = in.readInt();
        topicBits = in.readInt();

        numTypes = in.readInt();

        alpha = (double[]) in.readObject();
        alphaSum = in.readDouble();
        beta = in.readDouble();
        betaSum = in.readDouble();

        typeTopicCounts = (int[][]) in.readObject();
        tokensPerTopic = (int[]) in.readObject();

        docLengthCounts = (int[]) in.readObject();
        topicDocCounts = (int[][]) in.readObject();

        numIterations = in.readInt();
        burninPeriod = in.readInt();
        saveSampleInterval = in.readInt();
        optimizeInterval = in.readInt();
        showTopicsInterval = in.readInt();
        wordsPerTopic = in.readInt();

        saveStateInterval = in.readInt();
        stateFilename = (String) in.readObject();

        saveModelInterval = in.readInt();
        modelFilename = (String) in.readObject();

        randomSeed = in.readInt();
        formatter = (NumberFormat) in.readObject();
        printLogLikelihood = in.readBoolean();

        numThreads = in.readInt();
    }

    public double[][] getNormalizedDocumentTopicWeights() {
        int docLen;
        int[] topicCounts = new int[ numTopics ];
        double[][] documentWeights = new double[data.size()][numTopics];

        for (int docIndex=0; docIndex < data.size(); docIndex++) {
            TopicAssignment doc = data.get(docIndex);

            LabelSequence topicSequence = (LabelSequence) doc.topicSequence;
            int[] currentDocTopics = topicSequence.getFeatures();
            docLen = currentDocTopics.length;

            // Count up the tokens
            for (int token=0; token < docLen; token++) {
                topicCounts[ currentDocTopics[token] ]++;
            }

            // And normalize

            double weightSum = 0;
            for (int topicId = 0; topicId < numTopics; topicId++) {
                weightSum += (alpha[topicId] + topicCounts[topicId]) / (docLen + alphaSum);
            }

            // Save proportional topic weight
            for (int topicId = 0; topicId < numTopics; topicId++) {
                documentWeights[docIndex][topicId] = ((alpha[topicId] + topicCounts[topicId]) / (docLen + alphaSum))/weightSum;
            }
        }

        return documentWeights;
    }

    public static PersistentParallelTopicModel read (byte[] data) throws Exception {

        PersistentParallelTopicModel topicModel = null;

        ObjectInputStream ois = new ObjectInputStream (new ByteArrayInputStream(data));
        topicModel = (PersistentParallelTopicModel) ois.readObject();
        ois.close();

        topicModel.initializeHistograms();

        return topicModel;
    }

    public PancakeTopicInferencer getInferencer(){
        return new PancakeTopicInferencer(typeTopicCounts, tokensPerTopic, data.get(0).instance.getDataAlphabet(), alpha, beta, betaSum);
    }
}
