# --- !Ups

CREATE TABLE smarts_document (
    id BIGINT UNSIGNED AUTO_INCREMENT,
    topic_model_id BIGINT UNSIGNED,
    url TEXT,
    PRIMARY KEY (id),
    FOREIGN KEY (topic_model_id) REFERENCES smarts_topic_model(id) ON DELETE CASCADE
) ENGINE InnoDB;

CREATE TABLE smarts_document_topic (
    id BIGINT UNSIGNED AUTO_INCREMENT,
    topic_id BIGINT UNSIGNED,
    document_id BIGINT UNSIGNED,
    weight DOUBLE UNSIGNED,
    PRIMARY KEY (id),
    FOREIGN KEY (topic_id) REFERENCES smarts_topic(id) ON DELETE CASCADE,
    FOREIGN KEY (document_id) REFERENCES smarts_document(id) ON DELETE CASCADE,
    INDEX (weight) USING BTREE
) ENGINE InnoDB;

# --- !Downs

DROP TABLE smarts_document;
DROP TABLE smarts_document_topic;

