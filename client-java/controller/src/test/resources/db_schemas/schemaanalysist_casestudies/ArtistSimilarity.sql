-- http://labrosa.ee.columbia.edu/millionsong/pages/getting-dataset#subset

CREATE TABLE artists ("artist_id" text PRIMARY KEY);
CREATE TABLE similarity ("target" text, "similar" text, FOREIGN KEY(target)
REFERENCES artists("artist_id"), FOREIGN KEY("similar") REFERENCES
artists("artist_id") );
CREATE INDEX idx_sim_target ON similarity ("similar","target");
CREATE INDEX idx_target_sim ON similarity ("target","similar");
