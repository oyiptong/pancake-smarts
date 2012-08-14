package models;

import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.security.AllPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.io.ObjectOutputStream;
import javax.persistence.*;
import javax.persistence.CascadeType;


import cc.mallet.topics.ParallelTopicModel;
import com.avaje.ebean.Ebean;
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
import cc.mallet.pipe.FeatureSequence2AugmentableFeatureVector;

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
    private byte[] model;

    @Lob
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
        //malletTopicModel.setNumIterations(1000);
        malletTopicModel.setNumIterations(10);
        malletTopicModel.setOptimizeInterval(1000);
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

                for(int topicIndex=0; topicIndex<this.numTopics; topicIndex++) {
                    double weight = docWeights[docIndex][topicIndex];
                    Topic topic = topicList.get(topicIndex);

                    Document doc = new Document(docName);
                    documents.add(doc);
                    getDocuments().add(doc);
                    topic.getDocuments().add(doc);
                }
            }

            Ebean.save(this);
            Ebean.save(topics);
            Ebean.commitTransaction();

        }  finally {
            Ebean.endTransaction();
        }
    }

    public static Finder<Long,TopicModel> find = new Finder<Long,TopicModel>(Long.class, TopicModel.class);
}
