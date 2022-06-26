-- http://labrosa.ee.columbia.edu/millionsong/pages/getting-dataset

CREATE TABLE songs (track_id text PRIMARY KEY, title text, song_id text,
release text, artist_id text, artist_mbid text, artist_name text, duration
real, artist_familiarity real, artist_hotttnesss real, year int,
track_7digitalid int, shs_perf int, shs_work int);

CREATE INDEX idx_artist_id ON songs ("artist_id","release");
CREATE INDEX idx_artist_mbid ON songs ("artist_mbid","release");
CREATE INDEX idx_artist_name ON songs ("artist_name","title","release");
CREATE INDEX idx_duration ON songs ("duration","artist_id");
CREATE INDEX idx_familiarity ON songs ("artist_familiarity","artist_hotttnesss");
CREATE INDEX idx_hotttnesss ON songs ("artist_hotttnesss","artist_familiarity");
CREATE INDEX idx_shs_perf ON songs ("shs_perf");
CREATE INDEX idx_shs_work ON songs ("shs_work");
CREATE INDEX idx_title ON songs ("title","artist_name","release");
CREATE INDEX idx_year ON songs ("year","artist_id","title");
CREATE INDEX idx_year2 ON songs ("year","artist_name");
