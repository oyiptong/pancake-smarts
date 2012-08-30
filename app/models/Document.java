package models;

import org.codehaus.jackson.map.ObjectMapper;
import play.db.ebean.Model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-08-10
 * Time: 4:13 PM
 */
@Entity
@Table(name="smarts_document")
public class Document extends Model {

    public long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    @Id
    @GeneratedValue
    private long id;

    private String url;

    @Transient
    private double weight;

    @Column(name="topic_distribution")
    private String topicDistribution;

    @ManyToOne
    @JoinColumn(name="topic_model_id", nullable=false)
    private TopicModel topicModel;

    /*
    @ManyToMany(
            cascade = {CascadeType.ALL},
            mappedBy = "documents"
    )
    private List<Topic> topics;
    */
    @OneToMany
    @JoinColumn(name="topic_id")
    private List<DocumentTopic> associations;

    public TopicModel getTopicModel() { return topicModel;}

    //public List<Topic> getTopics() {return topics;}

    public List<DocumentTopic> getAssociations() { return associations; }

    public Document(String url, double[] topicDistribution) throws Exception {
        this.url = url;

        ObjectMapper mapper = new ObjectMapper();
        this.topicDistribution = mapper.writeValueAsString(topicDistribution);
    }

    public static Finder<Long,Document> find = new Finder<Long,Document>(Long.class, Document.class);

    public double getWeight() {
        return weight;
    }
}
