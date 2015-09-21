-- MySQL dump 10.13  Distrib 5.6.23-72.1, for Linux (x86_64)
--
-- Host: localhost    Database: gotrack
-- ------------------------------------------------------
-- Server version	5.6.23-72.1-log

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
-- Table structure for table `acindex`
--

DROP TABLE IF EXISTS `acindex`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `acindex` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `accession` varchar(10) NOT NULL,
  `symbol` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `accession` (`accession`,`symbol`)
) ENGINE=InnoDB AUTO_INCREMENT=774567 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `edition`
--

DROP TABLE IF EXISTS `edition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `edition` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `edition` int(11) NOT NULL,
  `species_id` int(11) NOT NULL,
  `date` date NOT NULL,
  `go_edition_id_fk` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `spec_ed` (`species_id`,`edition`)
) ENGINE=InnoDB AUTO_INCREMENT=2048 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `edition_aggregates`
--

DROP TABLE IF EXISTS `edition_aggregates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `edition_aggregates` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `species_id` int(11) NOT NULL,
  `edition` int(11) NOT NULL,
  `gene_count` int(11) NOT NULL,
  `avg_direct_terms_for_gene` double NOT NULL,
  `avg_inferred_terms_for_gene` double NOT NULL,
  `avg_inferred_genes_for_term` double NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1079 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `evidence_categories`
--

DROP TABLE IF EXISTS `evidence_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `evidence_categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `evidence` varchar(3) NOT NULL,
  `description` varchar(255) NOT NULL,
  `category` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `evidence` (`evidence`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `evidence_categories`
--

LOCK TABLES `evidence_categories` WRITE;
/*!40000 ALTER TABLE `evidence_categories` DISABLE KEYS */;
INSERT INTO `evidence_categories` VALUES (1,'EXP','Inferred from Experiment ','Experimental'),(2,'IDA','Inferred from Direct Assay ','Experimental'),(3,'IPI','Inferred from Physical Interaction ','Experimental'),(4,'IMP','Inferred from Mutant Phenotype ','Experimental'),(5,'IGI','Inferred from Genetic Interaction ','Experimental'),(6,'IEP','Inferred from Expression Pattern ','Experimental'),(7,'ISS','Inferred from Sequence or structural Similarity ','Computational'),(8,'ISO','Inferred from Sequence Orthology ','Computational'),(9,'ISA','Inferred from Sequence Alignment ','Computational'),(10,'ISM','Inferred from Sequence Model ','Computational'),(11,'IGC','Inferred from Genomic Context ','Computational'),(12,'IBA','Inferred from Biological aspect of Ancestor ','Computational'),(13,'IBD','Inferred from Biological aspect of Descendant ','Computational'),(14,'IKR','Inferred from Key Residues ','Computational'),(15,'IRD','Inferred from Rapid Divergence','Computational'),(16,'RCA','Inferred from Reviewed Computational Analysis ','Computational'),(17,'TAS','Traceable Author Statement ','Author'),(18,'NAS','Non-traceable Author Statement ','Author'),(19,'IC','Inferred by Curator','Curatorial'),(20,'ND','No biological Data available','Curatorial'),(21,'NR','Not recorded','Curatorial'),(22,'IEA','Inferred from Electronic Annotation ','Automatic');
/*!40000 ALTER TABLE `evidence_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gene_annotation`
--

DROP TABLE IF EXISTS `gene_annotation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gene_annotation` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `edition` int(11) NOT NULL,
  `species_id` int(11) NOT NULL,
  `accession` varchar(10) DEFAULT NULL,
  `db` varchar(255) NOT NULL,
  `db_object_id` varchar(255) NOT NULL,
  `symbol` varchar(255) NOT NULL,
  `qualifier` varchar(255) NOT NULL,
  `go_id` varchar(10) NOT NULL,
  `reference` varchar(255) NOT NULL,
  `evidence` varchar(255) NOT NULL,
  `db_object_name` text NOT NULL,
  `synonyms` text NOT NULL,
  `db_object_type` varchar(255) NOT NULL,
  `taxon` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `spec_ed_acc_go` (`edition`,`go_id`,`accession`,`species_id`),
  KEY `go_symbol` (`go_id`,`symbol`),
  KEY `spec_ac_ed` (`species_id`,`accession`,`edition`)
) ENGINE=InnoDB AUTO_INCREMENT=142292135 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `go_adjacency`
--

DROP TABLE IF EXISTS `go_adjacency`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `go_adjacency` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `go_edition_id_fk` int(10) unsigned NOT NULL,
  `child` varchar(10) NOT NULL,
  `parent` varchar(10) NOT NULL,
  `relationship` enum('IS_A','PART_OF') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `go_edition_id_fk` (`go_edition_id_fk`,`child`,`parent`,`relationship`)
) ENGINE=InnoDB AUTO_INCREMENT=7287970 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `go_annotation_counts`
--

DROP TABLE IF EXISTS `go_annotation_counts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `go_annotation_counts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `species_id` int(11) NOT NULL,
  `edition` int(11) NOT NULL,
  `go_id` varchar(10) NOT NULL,
  `direct_annotation_count` int(11) DEFAULT NULL,
  `inferred_annotation_count` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9664759 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `go_edition`
--

DROP TABLE IF EXISTS `go_edition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `go_edition` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `date` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=170 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `go_term`
--

DROP TABLE IF EXISTS `go_term`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `go_term` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `go_edition_id_fk` bigint(20) unsigned NOT NULL,
  `go_id` varchar(10) NOT NULL,
  `name` text NOT NULL,
  `aspect` enum('CC','BP','MF') DEFAULT NULL,
  `is_obsolete` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `go_ed_id_go_id` (`go_edition_id_fk`,`go_id`),
  KEY `go_id` (`go_id`),
  CONSTRAINT `fk_go_term_go_edition` FOREIGN KEY (`go_edition_id_fk`) REFERENCES `go_edition` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6597061 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pp_current_genes`
--

DROP TABLE IF EXISTS `pp_current_genes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pp_current_genes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `species_id` int(11) NOT NULL,
  `symbol` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `spec_symb` (`species_id`,`symbol`)
) ENGINE=InnoDB AUTO_INCREMENT=205539 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pp_goa`
--

DROP TABLE IF EXISTS `pp_goa`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pp_goa` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `pp_current_genes_id` int(10) unsigned NOT NULL,
  `edition` int(11) NOT NULL,
  `go_id` varchar(10) NOT NULL,
  `qualifier` varchar(255) DEFAULT NULL,
  `evidence` varchar(255) NOT NULL,
  `reference` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `cgi_ed_go_q_e_r` (`pp_current_genes_id`,`edition`,`go_id`,`qualifier`,`evidence`,`reference`),
  KEY `category_counts` (`go_id`,`evidence`,`edition`,`pp_current_genes_id`)
) ENGINE=InnoDB AUTO_INCREMENT=96795196 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pp_primary_accessions`
--

DROP TABLE IF EXISTS `pp_primary_accessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pp_primary_accessions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `pp_current_genes_id` int(11) NOT NULL,
  `accession` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `cgi_acc` (`pp_current_genes_id`,`accession`),
  KEY `acc_cgi` (`accession`,`pp_current_genes_id`)
) ENGINE=InnoDB AUTO_INCREMENT=289911 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pp_synonyms`
--

DROP TABLE IF EXISTS `pp_synonyms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pp_synonyms` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `pp_current_genes_id` int(11) NOT NULL,
  `synonym` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `cgi_syn` (`pp_current_genes_id`,`synonym`)
) ENGINE=InnoDB AUTO_INCREMENT=649347 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sec_ac`
--

DROP TABLE IF EXISTS `sec_ac`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sec_ac` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sec` varchar(10) NOT NULL,
  `ac` varchar(10) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `sec` (`sec`,`ac`),
  KEY `ac` (`ac`,`sec`)
) ENGINE=InnoDB AUTO_INCREMENT=356873 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `species`
--

DROP TABLE IF EXISTS `species`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `species` (
  `id` int(11) NOT NULL,
  `taxon` varchar(255) DEFAULT NULL,
  `common_name` varchar(255) NOT NULL,
  `scientific_name` varchar(255) NOT NULL,
  `short_name` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `species`
--

LOCK TABLES `species` WRITE;
/*!40000 ALTER TABLE `species` DISABLE KEYS */;
INSERT INTO `species` VALUES (1,'taxon:3702','Arabidopsis','Arabidopsis thaliana','arabidopsis'),(2,'taxon:9301','Chicken','Gallus gallus','chicken'),(3,'taxon:9913','Cattle','Bos taurus','cow'),(4,'taxon:44689','Dictyostelium','Dictyostelium discoideum','dicty'),(5,'taxon:9615','Dog','Canis lupus familiaris','dog'),(6,'taxon:7227','Fruit fly','Drosophila melanogaster','fly'),(7,'taxon:9606','Human','Homo sapiens','human'),(8,'taxon:10090','Mouse','Mus musculus','mouse'),(9,'taxon:9823','Pig','Sus scrofa','pig'),(10,'taxon:10116','Rat','Rattus norvegicus','rat'),(11,'taxon:6239','Roundworm','Caenorhabditis elegans','worm'),(12,'taxon:559292','Yeast','Saccharomyces cerevisiae S288c','yeast'),(13,'taxon:7955','Zebrafish','Danio rerio','zebrafish'),(14,'taxon:562','Ecoli','Escherichia coli','ecoli');
/*!40000 ALTER TABLE `species` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `track_popular_genes`
--

DROP TABLE IF EXISTS `track_popular_genes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `track_popular_genes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `species_id` int(11) NOT NULL,
  `symbol` varchar(255) NOT NULL,
  `count` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `species_id` (`species_id`,`symbol`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `track_popular_terms`
--

DROP TABLE IF EXISTS `track_popular_terms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `track_popular_terms` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `go_id` varchar(10) NOT NULL,
  `count` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `go_id` (`go_id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `view_current_data`
--

DROP TABLE IF EXISTS `view_current_data`;
/*!50001 DROP VIEW IF EXISTS `view_current_data`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE VIEW `view_current_data` AS SELECT 
 1 AS `species_id`,
 1 AS `edition`,
 1 AS `symbol`,
 1 AS `accession`,
 1 AS `synonyms`,
 1 AS `go_id`,
 1 AS `qualifier`,
 1 AS `evidence`,
 1 AS `reference`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `view_current_editions`
--

DROP TABLE IF EXISTS `view_current_editions`;
/*!50001 DROP VIEW IF EXISTS `view_current_editions`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE VIEW `view_current_editions` AS SELECT 
 1 AS `species_id`,
 1 AS `current_edition`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `view_gene_mapping`
--

DROP TABLE IF EXISTS `view_gene_mapping`;
/*!50001 DROP VIEW IF EXISTS `view_gene_mapping`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE VIEW `view_gene_mapping` AS SELECT 
 1 AS `pp_current_genes_id`,
 1 AS `sec`*/;
SET character_set_client = @saved_cs_client;

--
-- Final view structure for view `view_current_data`
--

/*!50001 DROP VIEW IF EXISTS `view_current_data`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`gotrack`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `view_current_data` AS select `gene_annotation`.`species_id` AS `species_id`,`gene_annotation`.`edition` AS `edition`,`gene_annotation`.`symbol` AS `symbol`,`gene_annotation`.`accession` AS `accession`,`gene_annotation`.`synonyms` AS `synonyms`,`gene_annotation`.`go_id` AS `go_id`,`gene_annotation`.`qualifier` AS `qualifier`,`gene_annotation`.`evidence` AS `evidence`,`gene_annotation`.`reference` AS `reference` from (`gene_annotation` join `view_current_editions` on(((`view_current_editions`.`current_edition` = `gene_annotation`.`edition`) and (`view_current_editions`.`species_id` = `gene_annotation`.`species_id`)))) where (`gene_annotation`.`accession` is not null) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `view_current_editions`
--

/*!50001 DROP VIEW IF EXISTS `view_current_editions`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`gotrack`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `view_current_editions` AS select `edition`.`species_id` AS `species_id`,max(`edition`.`edition`) AS `current_edition` from `edition` group by `edition`.`species_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `view_gene_mapping`
--

/*!50001 DROP VIEW IF EXISTS `view_gene_mapping`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`gotrack`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `view_gene_mapping` AS select `pp_primary_accessions`.`pp_current_genes_id` AS `pp_current_genes_id`,`sec_ac`.`sec` AS `sec` from (`pp_primary_accessions` join `sec_ac` on((`sec_ac`.`ac` = `pp_primary_accessions`.`accession`))) union select `pp_primary_accessions`.`pp_current_genes_id` AS `pp_current_genes_id`,`sec_ac`.`ac` AS `ac` from (`pp_primary_accessions` join `sec_ac` on((`sec_ac`.`ac` = `pp_primary_accessions`.`accession`))) union select `pp_primary_accessions`.`pp_current_genes_id` AS `pp_current_genes_id`,`pp_primary_accessions`.`accession` AS `accession` from `pp_primary_accessions` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2015-09-02 14:41:33
