# --- !Ups

CREATE TABLE smarts_topic_model (
    id BIGINT UNSIGNED AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    alpha DOUBLE UNSIGNED NOT NULL,
    beta DOUBLE UNSIGNED NOT NULL,
    num_topics INT UNSIGNED NOT NULL,
    model LONGBLOB NOT NULL,
    inferencer LONGBLOB NOT NULL,
    feature_sequence LONGBLOB NOT NULL,
    PRIMARY KEY (id),
    INDEX (name) USING BTREE
) ENGINE InnoDB;

CREATE TABLE smarts_topic (
    id BIGINT UNSIGNED AUTO_INCREMENT,
    topic_model_id BIGINT UNSIGNED,
    number INT UNSIGNED NOT NULL,
    word_sample TEXT,
    name VARCHAR(255),
    PRIMARY KEY (id),
    FOREIGN KEY (topic_model_id) REFERENCES smarts_topic_model(id) ON DELETE CASCADE
) ENGINE InnoDB;

# --- !Downs
DROP TABLE smarts_topic_model;
DROP TABLE smarts_topic;
