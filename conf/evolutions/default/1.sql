# --- !Ups

CREATE TABLE smarts_topic_model (
    id INTEGER UNSIGNED AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    alpha FLOAT UNSIGNED NOT NULL,
    beta FLOAT UNSIGNED NOT NULL,
    num_topics INT UNSIGNED NOT NULL,
    model LONGBLOB NOT NULL,
    inferencer LONGBLOB NOT NULL,
    PRIMARY KEY (id),
    INDEX (name) USING BTREE
) ENGINE InnoDB;

CREATE TABLE smarts_topic (
    id INT UNSIGNED AUTO_INCREMENT,
    topic_model_id INT UNSIGNED,
    number INT UNSIGNED NOT NULL UNIQUE,
    word_sample TEXT,
    name VARCHAR(255),
    PRIMARY KEY (id),
    FOREIGN KEY (topic_model_id) REFERENCES smarts_topic_model(id) ON DELETE CASCADE
) ENGINE InnoDB;

# --- !Downs
DROP TABLE smarts_topic_model;
DROP TABLE smarts_topic;
