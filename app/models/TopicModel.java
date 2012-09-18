/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package models;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import javax.persistence.*;
import javax.persistence.CascadeType;


import cc.mallet.pipe.iterator.JsonIterator;
import cc.mallet.topics.PancakeTopicInferencer;
import com.avaje.ebean.Ebean;
import org.apache.commons.lang.ArrayUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;

import static org.elasticsearch.index.query.QueryBuilders.fuzzyQuery;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import play.db.ebean.Model;

import cc.mallet.topics.PersistentParallelTopicModel;
import cc.mallet.util.CharSequenceLexer;
import cc.mallet.types.InstanceList;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.TokenSequence2FeatureSequence;

/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-08-03
 * Time: 5:59 PM
 */

@Entity
@Table(name="smarts_topic_model")
public class TopicModel extends Model {

    @Id
    @GeneratedValue
    private long id;

    private double alpha;

    private double beta;

    @Column(name="num_topics", nullable=false)
    private int numTopics;

    @Lob
    @Basic(fetch = FetchType.EAGER)
    private byte[] model;

    @Lob
    @Basic(fetch = FetchType.EAGER)
    private byte[] inferencer;

    @Lob
    @Column(name="feature_sequence", nullable=false)
    private byte[] featureSequence;

    @Column(length=255)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "topicModel")
    private List<Topic> topics;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "topicModel")
    private List<Document> documents;

    @Transient
    private PersistentParallelTopicModel malletTopicModel;

    @Transient
    private InstanceList currentInstanceList;

    // Getters & Setters

    public long getId() {
        return id;
    }

    public double getAlpha() {
        return alpha;
    }

    public double getBeta() {
        return beta;
    }

    public int getNumTopics() {
        return numTopics;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Topic> getTopics() { return topics; }

    public List<Document> getDocuments() { return documents; }

    /*
     * Standard pipe configuration for LDA modelling
     */
    public static SerialPipes getStandardPipes(){
        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

        Pattern tokenPattern = Pattern.compile(CharSequenceLexer.LEX_ALPHA.toString());
        pipeList.add(new CharSequence2TokenSequence(tokenPattern));

        pipeList.add(new TokenSequenceLowercase());

        pipeList.add(new TokenSequenceRemoveStopwords(false, false));

        pipeList.add(new TokenSequence2FeatureSequence());

        return new SerialPipes(pipeList);
    }

    public TopicModel(String name, int numTopics, double alpha, double beta, Reader dataReader) throws Exception {
        this.name = name;
        this.alpha = alpha;
        this.beta = beta;
        this.numTopics = numTopics;

        TopicModel named_model = TopicModel.find.where()
                .eq("name", name)
                .findUnique();

        if(named_model != null) {
            // TODO: a better exception. Also, handle concurrency
            throw new Exception("A model of that name already exists");
        }

        // convert input to vectors
        Pipe instancePipe = getStandardPipes();
        InstanceList instances = new InstanceList(instancePipe);
        instances.addThruPipe(new CsvIterator(dataReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"), 3, 2, 1));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(instances);
        oos.close();
        this.featureSequence = baos.toByteArray();

        // train model
        malletTopicModel = new PersistentParallelTopicModel(this.numTopics, this.alpha, this.beta);
        malletTopicModel.addInstances(instances);
        malletTopicModel.setNumIterations(1000);
        malletTopicModel.setOptimizeInterval(100);
        malletTopicModel.setBurninPeriod(10);
        malletTopicModel.setSymmetricAlpha(false);
        malletTopicModel.setNumThreads(8);

        malletTopicModel.estimate();
    }

    public void saveObjectGraph() throws Exception {
        Ebean.beginTransaction();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(malletTopicModel);
            oos.close();
            model = baos.toByteArray();

            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(malletTopicModel.getInferencer());
            oos.close();
            inferencer = baos.toByteArray();

            ArrayList<Topic> topicList = new ArrayList<Topic>();

            // create topics
            Object[][] topicWords = malletTopicModel.getTopWords(40);
            for(int topicNum = 0; topicNum < this.numTopics; topicNum++) {
                StringBuilder wordList = new StringBuilder();
                Object[] words = topicWords[topicNum];

                for(Object w: words) {
                    wordList.append(w);
                    wordList.append(" ");
                }
                if(wordList.length() > 0) {
                    // remove trailing space
                    wordList.deleteCharAt(wordList.length()-1);
                }
                Topic topic = new Topic(topicNum, wordList.toString());
                topics.add(topic);

                topicList.add(topic);
            }

            // create documents
            PancakeTopicInferencer inferencer = malletTopicModel.getInferencer();
            InstanceList docVectors = getDocumentVectors();

            // Only record the n most significant topics
            List<List> orderedDistributions = inferencer.inferSortedDistributions(docVectors, malletTopicModel.numIterations, 10, malletTopicModel.burninPeriod, 0.0, 5);

            for(int docIndex = 0 ; docIndex < orderedDistributions.size() ; docIndex++)
            {
                List docData = orderedDistributions.get(docIndex);
                String docName = (String) docData.get(0);

                double[] docTopWeights = generateTopTopicWeightVector(docIndex, orderedDistributions);
                Document doc = new Document(docName, docTopWeights);

                documents.add(doc);
                getDocuments().add(doc);
            }

            Ebean.save(this);
            Ebean.save(topics);

            // loop to save so the save hook is called
            for(Document doc : documents) {
                doc.save();
            }

            Ebean.commitTransaction();

        }  finally {
            Ebean.endTransaction();
        }
    }

    public static Finder<Long,TopicModel> find = new Finder<Long,TopicModel>(Long.class, TopicModel.class);

    public static TopicModel fetch(String name) throws Exception {
        TopicModel model = TopicModel.find.where().eq("name", name).findUnique();

        model.malletTopicModel = PersistentParallelTopicModel.read(model.model);
        return model;
    }

    protected InstanceList getInferenceVectors(JsonNode docs) throws IOException, ClassNotFoundException
    {
        InstanceList docVectors = getDocumentVectors();

        Pipe instancePipe = docVectors.getPipe();

        InstanceList newInstances = new InstanceList(instancePipe);
        newInstances.addThruPipe(new JsonIterator(docs));

        return newInstances;

    }

    protected InstanceList getDocumentVectors() throws IOException, ClassNotFoundException
    {
        if (currentInstanceList == null) {
            ObjectInputStream ois = new ObjectInputStream (new ByteArrayInputStream(featureSequence));
            currentInstanceList = (InstanceList) ois.readObject();
            ois.close();
        }
        return currentInstanceList;
    }

    public Map<String, List<String>> inferString(JsonNode jsonData, int maxTopics) throws ClassNotFoundException, IOException
    {
        PancakeTopicInferencer inferencer = malletTopicModel.getInferencer();
        InstanceList instances = getInferenceVectors(jsonData);

        List<Topic> topics = Topic.find.where().eq("topic_model_id", getId()).orderBy("number ASC").findList();

        List<List> distributions = inferencer.inferSortedDistributions(instances, malletTopicModel.numIterations, 10, malletTopicModel.burninPeriod, 0.0, maxTopics);

        Map<String, List<String>> output = new HashMap<String, List<String>>();

        for(int docIndex=0; docIndex < distributions.size(); docIndex++)
        {
            List docData = distributions.get(docIndex);
            List<List> topicDist = (List<List>) docData.get(1);

            List<String> docTopicWords = new ArrayList<String>();
            for(int topicIndex=0; topicIndex < maxTopics; topicIndex++)
            {
                List topicData = topicDist.get(topicIndex);
                int topicId = ((Integer) topicData.get(0)).intValue();
                Topic topic = topics.get(topicId);
                docTopicWords.add(topic.getWordSample());
            }
            output.put((String )docData.get(0), docTopicWords);
        }

        return output;
    }

    public List recommend(JsonNode jsonData, int maxRecommendations, int maxTopics) throws ClassNotFoundException, IOException, InterruptedException
    {
        PancakeTopicInferencer inferencer = malletTopicModel.getInferencer();
        InstanceList inferenceVectors = getInferenceVectors(jsonData);

        List<List> distributions = inferencer.inferDistributions(inferenceVectors, malletTopicModel.numIterations, 10, malletTopicModel.burninPeriod, 0.0);

        List<List> inferenceOrderedDistribution = inferencer.inferSortedDistributions(inferenceVectors, malletTopicModel.numIterations, 10, malletTopicModel.burninPeriod, 0.0, Math.max(maxTopics, 5));

        // output containers
        List output = new ArrayList(2);
        List<String> inferredWords = new ArrayList<String>();
        List<String> distributionDesc = new ArrayList<String>();
        List<String> recommendations = new ArrayList<String>();
        Set<String> allRecommendations = new HashSet<String>();

        // for each document
        for(int distIndex=0; distIndex < inferenceOrderedDistribution.size(); distIndex++)
        {
            List docTopTopics = inferenceOrderedDistribution.get(distIndex);
            List<List> topicDist = (List<List>) docTopTopics.get(1);

            // obtain textual topic distribution info
            List<String> docTopicWords = new ArrayList<String>();
            List<String> docTopicWeightDesc = new ArrayList<String>();
            for(int topicIndex=0; topicIndex < maxTopics; topicIndex++)
            {
                // obtain textual topic distribution info
                List topicData = topicDist.get(topicIndex);
                int topicNumber = ((Integer) topicData.get(0)).intValue();
                double topicWeight = ((Double) topicData.get(1)).doubleValue();

                Topic topic = topics.get(topicNumber);
                docTopicWords.add(topic.getWordSample());
                docTopicWeightDesc.add(String.format("topic #%d match: %.2f%%", topicNumber, topicWeight));
            }
            inferredWords = docTopicWords;
            distributionDesc = docTopicWeightDesc;

            double[] docTopWeights = generateTopTopicWeightVector(distIndex, inferenceOrderedDistribution);

            ObjectMapper mapper = new ObjectMapper();

            // 100 dimensions to match projection indexing
            String signature = RandomProjection.projectString(docTopWeights, 50);

            ElasticSearch es = ElasticSearch.getElasticSearch();
            Client esClient = es.getClient();

            SearchResponse response = esClient.prepareSearch("pancake-smarts")
                .setTypes("document")
                .setQuery(
                        fuzzyQuery("features_bits", signature).minSimilarity((float) 0.6)
                )
                .setFrom(0).setSize(maxRecommendations)
                .execute()
                .actionGet();

            SearchHits hits = response.getHits();
            SearchHit[] hitArray = hits.getHits();

            long[] hitIds = new long[maxRecommendations];

            for(int hitIndex = 0; hitIndex < hitArray.length; hitIndex++)
            {
                SearchHit hit = hitArray[hitIndex];
                hitIds[hitIndex] = Long.parseLong(hit.getId());
            }

            List<Document> recommendedDocs = Ebean.find(Document.class).where().in("id", ArrayUtils.toObject(hitIds)).findList();
            for(Document doc : recommendedDocs) {
                if(!allRecommendations.contains(doc.getUrl())) {
                    recommendations.add(doc.getUrl());
                    allRecommendations.add(doc.getUrl());
                }
            }
        }
        output.add(inferredWords);
        output.add(recommendations);
        output.add(distributionDesc);
        return output;
    }

    public double[] generateTopTopicWeightVector(int docIndex, List<List> sortedDistribution)
    {
        double[] docWeights = new double[numTopics];

        List<List> topTopics = (List<List>) sortedDistribution.get(docIndex).get(1);
        for(List topicData : topTopics)
        {
            int topicNum = ((Integer) topicData.get(0)).intValue();
            double weight = ((Double) topicData.get(1)).doubleValue();
            docWeights[topicNum] = weight;
        }

        return docWeights;
    }
}
