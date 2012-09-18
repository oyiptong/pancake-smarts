/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package models;

import play.db.ebean.Model;

import javax.persistence.*;
import java.util.List;

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

    public Topic(int number, String wordSample) {
        this.wordSample = wordSample;
        this.number = number;
    }

    public static Finder<Long,Topic> find = new Finder<Long,Topic>(Long.class, Topic.class);
}
