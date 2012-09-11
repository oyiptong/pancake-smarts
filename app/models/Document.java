package models;

import org.codehaus.jackson.map.ObjectMapper;
import play.db.ebean.Model;

import javax.persistence.*;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.BitSet;
import java.util.List;


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

    @Column(name="features_text")
    private String featuresText;

    @Column(name="features_bits")
    @Lob
    @Basic(fetch = FetchType.EAGER)
    private byte[] featuresBits;

    @ManyToOne
    @JoinColumn(name="topic_model_id", nullable=false)
    private TopicModel topicModel;

    public TopicModel getTopicModel() { return topicModel;}

    public Document(String url, double[] topicDistribution) throws Exception {
        this.url = url;

        ObjectMapper mapper = new ObjectMapper();
        this.topicDistribution = mapper.writeValueAsString(topicDistribution);
        // 100 dimensions for random projections
        BitSet bs = RandomProjection.projectBinaryBytes(topicDistribution, 100);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(bs);

        this.featuresBits = baos.toByteArray();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++)
        {
            if(bs.get(i) == true)
            {
                sb.append(String.format("one%d ", i));
            } else
            {
                sb.append(String.format("zero%d ", i));
            }
        }
        this.featuresText = sb.toString().trim();
    }

    public static Finder<Long,Document> find = new Finder<Long,Document>(Long.class, Document.class);

    public double getWeight()
    {
        return weight;
    }


    public String getFeaturesText()
    {
        return featuresText;
    }

    public byte[] getFeaturesBits()
    {
        return featuresBits;
    }
}
