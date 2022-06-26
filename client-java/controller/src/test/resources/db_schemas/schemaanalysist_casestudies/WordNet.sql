-- https://code.google.com/p/synonym/downloads/detail?name=wordnet30.1.tar.gz&can=2&q=

CREATE TABLE "categorydef" (
  "categoryid" decimal(2,0) NOT NULL default '0',
  "name" varchar(32) default NULL,
  "pos" char(1) default NULL,
  PRIMARY KEY  ("categoryid")
);
CREATE TABLE "lexlinkref" (
  "synset1id" decimal(9,0) NOT NULL default '0',
  "word1id" decimal(6,0) NOT NULL default '0',
  "synset2id" decimal(9,0) NOT NULL default '0',
  "word2id" decimal(6,0) NOT NULL default '0',
  "linkid" decimal(2,0) NOT NULL default '0',
  PRIMARY KEY  ("word1id","synset1id","word2id","synset2id","linkid")
);
CREATE TABLE "linkdef" (
  "linkid" decimal(2,0) NOT NULL default '0',
  "name" varchar(50) default NULL,
  "recurses" char(1) NOT NULL default 'N',
  PRIMARY KEY  ("linkid")
);
CREATE TABLE "sample" (
  "synsetid" decimal(9,0) NOT NULL default '0',
  "sampleid" decimal(2,0) NOT NULL default '0',
  "sample" text NOT NULL,
  PRIMARY KEY  ("synsetid","sampleid")
);
CREATE TABLE "semlinkref" (
  "synset1id" decimal(9,0) NOT NULL default '0',
  "synset2id" decimal(9,0) NOT NULL default '0',
  "linkid" decimal(2,0) NOT NULL default '0',
  PRIMARY KEY  ("synset1id","synset2id","linkid")
);
CREATE TABLE "sense" (
  "wordid" decimal(6,0) NOT NULL default '0',
  "casedwordid" decimal(6,0) default NULL,
  "synsetid" decimal(9,0) NOT NULL default '0',
  "rank" decimal(2,0) NOT NULL default '0',
  "lexid" decimal(2,0) NOT NULL default '0',
  "tagcount" decimal(5,0) default NULL,
  PRIMARY KEY  ("synsetid","wordid")
);
CREATE TABLE "synset" (
  "synsetid" decimal(9,0) NOT NULL default '0',
  "pos" char(1) default NULL,
  "categoryid" decimal(2,0) NOT NULL default '0',
  "definition" text,
  PRIMARY KEY  ("synsetid")
);
CREATE TABLE "word" (
  "wordid" decimal(6,0) NOT NULL default '0',
  "lemma" varchar(80) NOT NULL UNIQUE default '',
  PRIMARY KEY  ("wordid")
);

