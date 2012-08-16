package models;

import play.db.ebean.Model;

import javax.persistence.*;

/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-08-16
 * Time: 10:39 AM
 */
@Entity
@Table(name="smarts_document_topic")
public class DocumentTopic extends Model {

    @Id
    @GeneratedValue
    private long id;

    private double weight;

    @ManyToOne
    @JoinColumn(name="topic_id", nullable=false)
    private Topic topic;

    @ManyToOne
    @JoinColumn(name="document_id", nullable=false)
    private Document document;

    public long getId()
    {
        return id;
    }

    public Topic getTopic() {
        return topic;
    }

    public Document getDocument() {
        return document;
    }

    public DocumentTopic(double weight, Document document, Topic topic) {
        this.weight = weight;
        this.topic = topic;
        this.document = document;
    }
}
