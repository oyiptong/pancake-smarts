package models;

import play.db.ebean.Model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name="smarts_topic")
public class Topic extends Model {

    @Id
    @GeneratedValue
    private long id;

    private int number;

    @Column(name="word_sample")
    private String wordSample;

    @ManyToOne(optional = false)
    @JoinColumn(name="topic_model_id", nullable=false)
    private TopicModel topicModel;

    @ManyToMany
    @JoinTable(
            name = "smarts_document_topic",
            inverseJoinColumns = @JoinColumn(name="document_id"),
            joinColumns = @JoinColumn(name="topic_id")
    )
    private List<Document> documents;

    @Column(length=255)
    private String name;

    public long getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public String getWordSample() {
        return wordSample;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TopicModel getTopicModel() { return topicModel; }

    public List<Document> getDocuments() {return documents; }

    public Topic(int number, String wordSample) {
        this.wordSample = wordSample;
        this.number = number;
    }

    public static Finder<Long,Topic> find = new Finder<Long,Topic>(Long.class, Topic.class);
}
