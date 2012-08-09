package models;

import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.io.ObjectOutputStream;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

import play.db.ebean.Model;

import cc.mallet.topics.TopicAssignment;
import cc.mallet.topics.ParallelTopicModel;
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

    private String name;

    // Getters & Setters

    @Id
    @GeneratedValue
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

    public ParallelTopicModel getModel() {
        return new ParallelTopicModel(50);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
        ParallelTopicModel topicModel = new ParallelTopicModel(this.numTopics, this.alpha, this.beta);
        topicModel.addInstances(instances);
        topicModel.setNumIterations(1000);
        topicModel.setOptimizeInterval(100);
        topicModel.setBurninPeriod(10);
        topicModel.setSymmetricAlpha(false);
        topicModel.setNumThreads(4);

        topicModel.estimate();

        baos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(baos);
        oos.writeObject(topicModel);
        oos.close();
        this.model = baos.toByteArray();

        baos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(baos);
        oos.writeObject(topicModel.getInferencer());
        oos.close();
        this.inferencer = baos.toByteArray();

        this.save();

        ArrayList<TopicAssignment> topics = topicModel.getData();
        for(TopicAssignment topic : topics) {
            System.out.println(topic.instance.getName());
        }
    }

    private TopicModel(){
    }

    public static Finder<Integer,TopicModel> find = new Finder<Integer,TopicModel>(Integer.class, TopicModel.class);
}
