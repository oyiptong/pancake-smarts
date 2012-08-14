package models;

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

    @ManyToOne
    @JoinColumn(name="topic_model_id", nullable=false)
    private TopicModel topicModel;

    @ManyToMany(
            cascade = {CascadeType.ALL},
            mappedBy = "documents"
    )
    private List<Topic> topics;

    public TopicModel getTopicModel() { return topicModel;}

    public List<Topic> getTopics() {return topics;}

    public Document(String url) {
        this.url = url;
    }

    public static Finder<Long,Document> find = new Finder<Long,Document>(Long.class, Document.class);
}