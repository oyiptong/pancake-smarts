package models;

import java.sql.Blob;
import cc.mallet.topics.ParallelTopicModel;
import play.db.ebean.Model;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

@Entity
@Table(name="smarts_topic")
public class Topic extends Model {
}
