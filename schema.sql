-- MySQL dump 10.11
--
-- Host: localhost    Database: lobby
-- ------------------------------------------------------
-- Server version	5.0.75-0ubuntu10.2

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
-- Table structure for table `Accounts`
--

DROP TABLE IF EXISTS `Accounts`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `Accounts` (
  `ID` int(10) unsigned NOT NULL auto_increment,
  `Enabled` tinyint(1) NOT NULL default '1',
  `Username` varchar(20) collate latin1_bin NOT NULL default '',
  `Nickname` varchar(25) collate latin1_bin default NULL,
  `Password` varchar(30) collate latin1_bin NOT NULL default '',
  `AccessBits` int(11) NOT NULL default '0',
  `LastUserID` int(11) NOT NULL default '0',
  `LastLogin` bigint(20) NOT NULL default '0',
  `LastIP` int(11) NOT NULL default '0',
  `RegistrationDate` bigint(20) NOT NULL default '0',
  `LastCountry` char(2) collate latin1_bin NOT NULL default 'XX',
  `Email` varchar(255) collate latin1_bin default NULL,
  `Comment` text collate latin1_bin,
  PRIMARY KEY  (`ID`,`Username`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=latin1 COLLATE=latin1_bin;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `BanDeletionHistory`
--

DROP TABLE IF EXISTS `BanDeletionHistory`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `BanDeletionHistory` (
  `BanID` int(10) unsigned NOT NULL default '0',
  `Author` varchar(30) NOT NULL default '',
  `DeletionDate` timestamp NOT NULL default CURRENT_TIMESTAMP,
  `Reason` text,
  PRIMARY KEY  (`BanID`),
  CONSTRAINT `BanDeletionHistory_ibfk_1` FOREIGN KEY (`BanID`) REFERENCES `BanEntries` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `BanEntries`
--

DROP TABLE IF EXISTS `BanEntries`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `BanEntries` (
  `ID` int(10) unsigned NOT NULL auto_increment,
  `Enabled` tinyint(1) NOT NULL default '1',
  `Owner` varchar(30) NOT NULL default '',
  `Date` timestamp NOT NULL default CURRENT_TIMESTAMP,
  `ExpirationDate` timestamp NULL default NULL,
  `Username` varchar(30) default NULL,
  `IP_start` int(10) unsigned default NULL,
  `IP_end` int(10) unsigned default NULL,
  `userID` int(11) default NULL,
  `PrivateReason` text NOT NULL,
  `PublicReason` text NOT NULL,
  PRIMARY KEY  (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=2363 DEFAULT CHARSET=latin1;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `MapGrades`
--

DROP TABLE IF EXISTS `MapGrades`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `MapGrades` (
  `UserID` int(10) unsigned NOT NULL default '0',
  `MapHash` char(8) collate latin1_bin NOT NULL default '',
  `Grade` tinyint(4) NOT NULL default '0',
  `Minutes` int(11) NOT NULL default '0',
  KEY `UserID` (`UserID`),
  CONSTRAINT `MapGrades_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `Accounts` (`ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_bin;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `OldAccounts`
--

DROP TABLE IF EXISTS `OldAccounts`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `OldAccounts` (
  `ID` int(10) unsigned NOT NULL auto_increment,
  `Transferred` tinyint(1) NOT NULL default '0',
  `Username` varchar(20) collate latin1_bin NOT NULL default '',
  `NewUsername` varchar(20) collate latin1_bin default NULL,
  `Password` varchar(30) collate latin1_bin NOT NULL default '',
  `AccessBits` int(11) NOT NULL default '0',
  `RegistrationDate` bigint(20) NOT NULL default '0',
  `LastCountry` char(2) collate latin1_bin NOT NULL default 'xx',
  PRIMARY KEY  (`ID`,`Username`)
) ENGINE=InnoDB AUTO_INCREMENT=86594 DEFAULT CHARSET=latin1 COLLATE=latin1_bin;
SET character_set_client = @saved_cs_client;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2009-06-07 20:51:35
