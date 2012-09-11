-- MySQL dump 10.13  Distrib 5.5.25a, for osx10.8 (i386)
--
-- Host: localhost    Database: pancake_smarts
-- ------------------------------------------------------
-- Server version	5.5.25a-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `play_evolutions`
--

DROP TABLE IF EXISTS `play_evolutions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `play_evolutions` (
  `id` int(11) NOT NULL,
  `hash` varchar(255) NOT NULL,
  `applied_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `apply_script` text,
  `revert_script` text,
  `state` varchar(255) DEFAULT NULL,
  `last_problem` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `play_evolutions`
--

LOCK TABLES `play_evolutions` WRITE;
/*!40000 ALTER TABLE `play_evolutions` DISABLE KEYS */;
INSERT INTO `play_evolutions` VALUES (1,'f0064bc101bde9dec17ed5e5a7bc33cf43dd4d9f','2012-09-11 15:14:37','CREATE TABLE smarts_topic_model (\nid BIGINT UNSIGNED AUTO_INCREMENT,\nname VARCHAR(255) NOT NULL UNIQUE,\nalpha DOUBLE UNSIGNED NOT NULL,\nbeta DOUBLE UNSIGNED NOT NULL,\nnum_topics INT UNSIGNED NOT NULL,\nmodel LONGBLOB NOT NULL,\ninferencer LONGBLOB NOT NULL,\nfeature_sequence LONGBLOB NOT NULL,\nPRIMARY KEY (id),\nINDEX (name) USING BTREE\n) ENGINE InnoDB;\n\nCREATE TABLE smarts_topic (\nid BIGINT UNSIGNED AUTO_INCREMENT,\ntopic_model_id BIGINT UNSIGNED,\nnumber INT UNSIGNED NOT NULL,\nword_sample TEXT,\nname VARCHAR(255),\nPRIMARY KEY (id),\nFOREIGN KEY (topic_model_id) REFERENCES smarts_topic_model(id) ON DELETE CASCADE\n) ENGINE InnoDB;','DROP TABLE smarts_topic_model;\nDROP TABLE smarts_topic;','applied',''),(2,'7463abfe73fc5b9b79d5cee06838a1df04479690','2012-09-11 15:14:37','CREATE TABLE smarts_document (\nid BIGINT UNSIGNED AUTO_INCREMENT,\ntopic_model_id BIGINT UNSIGNED,\nurl TEXT,\ntopic_distribution TEXT,\nPRIMARY KEY (id),\nFOREIGN KEY (topic_model_id) REFERENCES smarts_topic_model(id) ON DELETE CASCADE\n) ENGINE InnoDB;\n\nCREATE TABLE smarts_document_topic (\nid BIGINT UNSIGNED AUTO_INCREMENT,\ntopic_id BIGINT UNSIGNED,\ndocument_id BIGINT UNSIGNED,\nweight DOUBLE UNSIGNED,\nPRIMARY KEY (id),\nFOREIGN KEY (topic_id) REFERENCES smarts_topic(id) ON DELETE CASCADE,\nFOREIGN KEY (document_id) REFERENCES smarts_document(id) ON DELETE CASCADE,\nINDEX (weight) USING BTREE\n) ENGINE InnoDB;','DROP TABLE smarts_document;\nDROP TABLE smarts_document_topic;','applied',''),(3,'7f1c9069b51dea6fbc1e69754e62de9bcd58c714','2012-09-11 15:14:37','DROP TABLE smarts_document_topic;','CREATE TABLE smarts_document_topic (\nid BIGINT UNSIGNED AUTO_INCREMENT,\ntopic_id BIGINT UNSIGNED,\ndocument_id BIGINT UNSIGNED,\nweight DOUBLE UNSIGNED,\nPRIMARY KEY (id),\nFOREIGN KEY (topic_id) REFERENCES smarts_topic(id) ON DELETE CASCADE,\nFOREIGN KEY (document_id) REFERENCES smarts_document(id) ON DELETE CASCADE,\nINDEX (weight) USING BTREE\n) ENGINE InnoDB;','applied',''),(4,'748c82c674445f08db49c3c0b4ad791c8e4262f9','2012-09-11 15:14:37','ALTER TABLE smarts_document ADD COLUMN features_text TEXT, ADD COLUMN features_bits BLOB;','ALTER TABLE smarts_document DROP COLUMN features_text, DROP COLUMN features_bits;','applied','');
/*!40000 ALTER TABLE `play_evolutions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smarts_document`
--

DROP TABLE IF EXISTS `smarts_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smarts_document` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `topic_model_id` bigint(20) unsigned DEFAULT NULL,
  `url` text,
  `topic_distribution` text,
  `features_text` text,
  `features_bits` blob,
  PRIMARY KEY (`id`),
  KEY `topic_model_id` (`topic_model_id`),
  CONSTRAINT `smarts_document_ibfk_1` FOREIGN KEY (`topic_model_id`) REFERENCES `smarts_topic_model` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smarts_document`
--

LOCK TABLES `smarts_document` WRITE;
/*!40000 ALTER TABLE `smarts_document` DISABLE KEYS */;
/*!40000 ALTER TABLE `smarts_document` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smarts_topic`
--

DROP TABLE IF EXISTS `smarts_topic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smarts_topic` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `topic_model_id` bigint(20) unsigned DEFAULT NULL,
  `number` int(10) unsigned NOT NULL,
  `word_sample` text,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `topic_model_id` (`topic_model_id`),
  CONSTRAINT `smarts_topic_ibfk_1` FOREIGN KEY (`topic_model_id`) REFERENCES `smarts_topic_model` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smarts_topic`
--

LOCK TABLES `smarts_topic` WRITE;
/*!40000 ALTER TABLE `smarts_topic` DISABLE KEYS */;
/*!40000 ALTER TABLE `smarts_topic` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smarts_topic_model`
--

DROP TABLE IF EXISTS `smarts_topic_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smarts_topic_model` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `alpha` double unsigned NOT NULL,
  `beta` double unsigned NOT NULL,
  `num_topics` int(10) unsigned NOT NULL,
  `model` longblob NOT NULL,
  `inferencer` longblob NOT NULL,
  `feature_sequence` longblob NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `name_2` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smarts_topic_model`
--

LOCK TABLES `smarts_topic_model` WRITE;
/*!40000 ALTER TABLE `smarts_topic_model` DISABLE KEYS */;
/*!40000 ALTER TABLE `smarts_topic_model` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2012-09-11 11:25:25
