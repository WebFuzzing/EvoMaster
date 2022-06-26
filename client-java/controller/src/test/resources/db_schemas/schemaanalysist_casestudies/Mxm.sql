-- http://labrosa.ee.columbia.edu/millionsong/musixmatch#getting

CREATE TABLE words (word TEXT PRIMARY KEY);

CREATE TABLE lyrics (track_id INTEGER, mxm_tid INTEGER, word TEXT, count INTEGER, is_test INTEGER,
	FOREIGN KEY(word) REFERENCES words(word));

CREATE INDEX idx_lyrics1 ON lyrics ("track_id");
CREATE INDEX idx_lyrics2 ON lyrics ("mxm_tid");
CREATE INDEX idx_lyrics3 ON lyrics ("word");
CREATE INDEX idx_lyrics4 ON lyrics ("count");
CREATE INDEX idx_lyrics5 ON lyrics ("is_test");
