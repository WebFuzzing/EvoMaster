CREATE TABLE places (
  host TEXT NOT NULL, 
  path TEXT NOT NULL,
  title TEXT,
  visit_count INTEGER,
  fav_icon_url TEXT,
  PRIMARY KEY(host, path)
);

CREATE TABLE cookies (
  id INTEGER PRIMARY KEY NOT NULL, 
  name TEXT NOT NULL, 
  value TEXT, 
  expiry INTEGER, 
  last_accessed INTEGER, 
  creation_time INTEGER, 
  host TEXT, 
  path TEXT,  
  UNIQUE(name, host, path),
  FOREIGN KEY(host, path) REFERENCES places(host, path),
  CHECK (expiry = 0 OR expiry > last_accessed),
  CHECK (last_accessed >= creation_time)
);