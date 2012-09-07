# --- !Ups
DROP TABLE smarts_document_topic;

# --- !Downs
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