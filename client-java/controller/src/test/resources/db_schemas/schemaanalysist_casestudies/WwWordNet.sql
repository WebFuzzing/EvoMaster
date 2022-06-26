-- NOTE: The source of this database is a graduated student of mine who was developing
-- and Android application. We have permission to use this schema in our research.  But,
-- I cannot give you access to the version control repository right now.

CREATE TABLE android_metadata (locale TEXT);
CREATE TABLE lexrel (
            sourceword INTEGER NOT NULL,
            sourcesynset INTEGER NOT NULL,
            targetword INTEGER NOT NULL,
            targetsynset INTEGER NOT NULL,
            relation INTEGER NOT NULL,
            PRIMARY KEY (
                sourceword, sourcesynset,
                targetword, targetsynset,
                relation));
-- CREATE VIRTUAL TABLE search USING fts3(lemma, morphs, tokenize=porter);
CREATE TABLE "search_segdir"(level INTEGER,idx INTEGER,start_block INTEGER,leaves_end_block INTEGER,end_block INTEGER,root TEXT,PRIMARY KEY(level, idx));
CREATE TABLE "search_segments"(blockid INTEGER PRIMARY KEY, block TEXT);
CREATE TABLE semrel (
            sourcesynset INTEGER NOT NULL,
            targetsynset INTEGER NOT NULL,
            relation INTEGER NOT NULL,
            PRIMARY KEY (sourcesynset, targetsynset, relation));
CREATE TABLE sense (
            wordid INTEGER NOT NULL,
            synsetid INTEGER NOT NULL,
            cased TEXT,
            PRIMARY KEY (wordid, synsetid));
CREATE TABLE synset (
            samples TEXT,
            definition TEXT NOT NULL,
            synsetid INTEGER PRIMARY KEY,
            pos TEXT NOT NULL);
CREATE TABLE word (
            morphs TEXT,
            lemma TEXT NOT NULL,
            wordid INTEGER NOT NULL,
            pos TEXT NOT NULL,
            PRIMARY KEY (wordid, pos));
CREATE VIEW search_content AS
        SELECT rowid AS docid, lemma AS c0lemma, morphs AS c1morphs FROM word;
