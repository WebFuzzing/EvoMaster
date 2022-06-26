CREATE TABLE moz_anno_attributes (  id INTEGER PRIMARY KEY, name VARCHAR(32) UNIQUE NOT NULL);
CREATE TABLE moz_annos (  id INTEGER PRIMARY KEY, place_id INTEGER NOT NULL, anno_attribute_id INTEGER, mime_type VARCHAR(32) DEFAULT NULL, content VARCHAR, flags INTEGER DEFAULT 0, expiration INTEGER DEFAULT 0, type INTEGER DEFAULT 0, dateAdded INTEGER DEFAULT 0, lastModified INTEGER DEFAULT 0);
CREATE TABLE moz_bookmarks (  id INTEGER PRIMARY KEY, type INTEGER, fk INTEGER DEFAULT NULL, parent INTEGER, position INTEGER, title VARCHAR, keyword_id INTEGER, folder_type VARCHAR, dateAdded INTEGER, lastModified INTEGER, guid VARCHAR);
CREATE TABLE moz_bookmarks_roots (  root_name VARCHAR(16) UNIQUE, folder_id INTEGER);
CREATE TABLE moz_favicons (  id INTEGER PRIMARY KEY, url VARCHAR UNIQUE, data VARCHAR, mime_type VARCHAR(32), expiration INTEGER, guid VARCHAR);
CREATE TABLE moz_historyvisits (  id INTEGER PRIMARY KEY, from_visit INTEGER, place_id INTEGER, visit_date INTEGER, visit_type INTEGER, session INTEGER);
CREATE TABLE moz_hosts (  id INTEGER PRIMARY KEY, host VARCHAR NOT NULL UNIQUE, frecency INTEGER, typed INTEGER NOT NULL DEFAULT 0, prefix VARCHAR);
CREATE TABLE moz_inputhistory (  place_id INTEGER NOT NULL, input VARCHAR NOT NULL, use_count INTEGER, PRIMARY KEY (place_id, input));
CREATE TABLE moz_items_annos (  id INTEGER PRIMARY KEY, item_id INTEGER NOT NULL, anno_attribute_id INTEGER, mime_type VARCHAR(32) DEFAULT NULL, content VARCHAR, flags INTEGER DEFAULT 0, expiration INTEGER DEFAULT 0, type INTEGER DEFAULT 0, dateAdded INTEGER DEFAULT 0, lastModified INTEGER DEFAULT 0);
CREATE TABLE moz_keywords (  id INTEGER PRIMARY KEY, keyword VARCHAR UNIQUE);
CREATE TABLE moz_places (   id INTEGER PRIMARY KEY, url VARCHAR, title VARCHAR, rev_host VARCHAR, visit_count INTEGER DEFAULT 0, hidden INTEGER DEFAULT 0 NOT NULL, typed INTEGER DEFAULT 0 NOT NULL, favicon_id INTEGER, frecency INTEGER DEFAULT -1 NOT NULL, last_visit_date INTEGER , guid VARCHAR);

