-- http://labrosa.ee.columbia.edu/millionsong/pages/getting-dataset#subset

CREATE TABLE artists (artist_id text PRIMARY KEY);

CREATE TABLE mbtags (mbtag text PRIMARY KEY);
CREATE TABLE terms (term text PRIMARY KEY);

CREATE TABLE artist_mbtag (artist_id text, mbtag text, FOREIGN KEY(artist_id)
	REFERENCES artists(artist_id), FOREIGN KEY(mbtag) REFERENCES mbtags(mbtag)
);


CREATE TABLE artist_term (artist_id text, term text, FOREIGN KEY(artist_id)
	REFERENCES artists(artist_id), FOREIGN KEY(term) REFERENCES terms(term) );

CREATE INDEX idx_artist_id_mbtag ON artist_mbtag ("artist_id","mbtag");
CREATE INDEX idx_artist_id_term ON artist_term ("artist_id","term");
CREATE INDEX idx_mbtag_artist_id ON artist_mbtag ("mbtag","artist_id");
CREATE INDEX idx_term_artist_id ON artist_term ("term","artist_id");
