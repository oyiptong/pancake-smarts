package models;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.persistence.*;
import javax.persistence.CascadeType;


import cc.mallet.pipe.iterator.JsonIterator;
import cc.mallet.topics.PancakeTopicInferencer;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.DecimalNode;
import play.db.ebean.Model;

import cc.mallet.topics.TopicAssignment;
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

    /*
    public PersistentParallelTopicModel getModel() {
        //return new PersistentParallelTopicModel(50);
    }
    */

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
        malletTopicModel.setNumThreads(4);

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
            ArrayList<Document> docList = new ArrayList<Document>();
            ArrayList<DocumentTopic> associationList = new ArrayList<DocumentTopic>();

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
            double[][] docWeights = malletTopicModel.getNormalizedDocumentTopicWeights();
            ArrayList<TopicAssignment> docs = malletTopicModel.getData();

            for(int docIndex=0; docIndex < docs.size(); docIndex++) {
                TopicAssignment docAssignment = docs.get(docIndex);
                String docName = (String) docAssignment.instance.getName();
                Document doc = new Document(docName);

                for(int topicIndex=0; topicIndex<this.numTopics; topicIndex++) {
                    Topic topic = topicList.get(topicIndex);
                    double weight = docWeights[docIndex][topicIndex];
                    // save the weight
                    DocumentTopic association = new DocumentTopic(weight, doc, topic);
                    associationList.add(association);

                }
                documents.add(doc);
                docList.add(doc);
                getDocuments().add(doc);

            }

            Ebean.save(this);
            Ebean.save(topics);
            Ebean.save(associationList);

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
        ObjectInputStream ois = new ObjectInputStream (new ByteArrayInputStream(featureSequence));
        InstanceList currentInstanceList = (InstanceList) ois.readObject();
        ois.close();

        Pipe instancePipe = currentInstanceList.getPipe();

        InstanceList newInstances = new InstanceList(instancePipe);
        newInstances.addThruPipe(new JsonIterator(docs));

        return newInstances;

    }

    public List infer(JsonNode jsonData) throws ClassNotFoundException, IOException
    {
        PancakeTopicInferencer inferencer = malletTopicModel.getInferencer();
        InstanceList instances = getInferenceVectors(jsonData);

        List distributions = inferencer.inferDistributions(instances, malletTopicModel.numIterations, 10, malletTopicModel.burninPeriod, 0.0, -1);

        return distributions;
    }

    public List recommend(JsonNode jsonData, double threshold, int max) throws ClassNotFoundException, IOException
    {
        PancakeTopicInferencer inferencer = malletTopicModel.getInferencer();
        InstanceList instances = getInferenceVectors(jsonData);

        List<List> distributions = inferencer.inferDistributions(instances, malletTopicModel.numIterations, 10, malletTopicModel.burninPeriod, threshold, max);

        List<Topic> topics = Topic.find.where().eq("topic_model_id", getId()).orderBy("number ASC").findList();
        String[][] topicDocArray = new String[numTopics][max];

        // should not include the document itself

        for(int topicIndex=0; topicIndex < topics.size(); topicIndex++)
        {

            //List<Document> docList = Document.find.fetch("smarts_document_topic", "weight, topic_id, document_id").where().eq("topic_id", topics.get(topicIndex).getId()).orderBy("weight DESC").setMaxRows(10).findList();
            String sql
                    = "SELECT d.id, d.url FROM smarts_document d, smarts_document_topic dt"
                    + " WHERE dt.topic_id = " + topics.get(topicIndex).getId()
                    + " AND d.topic_model_id = " + id
                    + " AND dt.document_id = d.id"
                    + " ORDER BY weight DESC"
                    + " LIMIT " + max;

            RawSql rawSql = RawSqlBuilder
                    .parse(sql)
                    .columnMapping("d.id", "id")
                    .columnMapping("d.url", "url")
                    .create();

            List<Document> docList = Ebean.find(Document.class).setRawSql(rawSql).findList();
            for(int docIndex=0; docIndex < docList.size() ; docIndex++)
            {
                topicDocArray[topicIndex][docIndex] = docList.get(docIndex).getUrl();
            }
        }

        ArrayList<Set> recommendations = new ArrayList<Set>();

        for(int docIndex=0; docIndex < distributions.size(); docIndex++)
        {
            Set<String> docRecommendations = new HashSet<String>();

            List docData = distributions.get(docIndex);
            List<List> topicDist = (List<List>) docData.get(1);
            double totalWeight = 0.0;
            for (List topicData : topicDist)
            {
                totalWeight += ((Double) topicData.get(1)).doubleValue();
            }

            for (int topicIndex=0 ; topicIndex < topicDist.size(); topicIndex++)
            {
                List topicData = topicDist.get(topicIndex);
                int topicId = ((Integer) topicData.get(0)).intValue();
                double proportion = ((Double) topicData.get(1)).doubleValue()/totalWeight;
                BigDecimal roundedUp = new BigDecimal(proportion * max).setScale(0, BigDecimal.ROUND_HALF_UP);
                int maxNumber = roundedUp.intValue();

                for(int suggestionIndex=0; suggestionIndex < maxNumber+1; suggestionIndex++)
                {
                    if (suggestionIndex < topicDocArray[topicId].length)
                    {
                        docRecommendations.add(topicDocArray[topicId][suggestionIndex]);
                    } else
                    {
                        break;
                    }
                }
            }
            recommendations.add(docRecommendations);
        }

        return recommendations;
    }
}
