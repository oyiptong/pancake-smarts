package models;

import java.sql.Blob;
import cc.mallet.topics.ParallelTopicModel;
import play.db.ebean.Model;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-08-03
 * Time: 5:59 PM
 */

@Entity
@Table(name="smarts_topic_model")
public class TopicModel extends Model {

    private int id;
    private float alpha;
    private float beta;

    private int numTopics;
    private ParallelTopicModel model;
    private String name;

    // Getters & Setters

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    public float getAlpha() {
        return alpha;
    }

    public float getBeta() {
        return beta;
    }

    public int getNumTopics() {
        return numTopics;
    }

    public ParallelTopicModel getModel() {
        return model;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TopicModel(String name, float alpha, float beta, int numTopics){
        this.name = name;
        this.alpha = alpha;
        this.beta = beta;
        this.numTopics = numTopics;
    }

    private TopicModel(){
    }

    public static TopicModel loadModel(String name){
        return new TopicModel();
    }
}
