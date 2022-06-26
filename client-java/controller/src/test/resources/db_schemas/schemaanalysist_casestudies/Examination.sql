DROP TABLE Exam;

DROP TABLE Examlog;

CREATE TABLE Exam 
(
	ekey      INTEGER PRIMARY KEY,
	fn        VARCHAR(15),
        ln        VARCHAR(30),
        exam      INTEGER,
        score     int,
        timeEnter DATE,
	CHECK (score >= 0),	 
	CHECK (score <= 100)	 
);

CREATE TABLE Examlog 
(
		lkey INTEGER PRIMARY KEY,
                ekey INTEGER,
                ekeyOLD INTEGER,
                fnNEW   VARCHAR(15),
                fnOLD   VARCHAR(15),
                lnNEW   VARCHAR(30),
                lnOLD   VARCHAR(30),
                examNEW INTEGER,
                examOLD INTEGER,
                scoreNEW int,
                scoreOLD int,
                sqlAction VARCHAR(15),
                examtimeEnter    DATE,
                examtimeUpdate   DATE,
                timeEnter        DATE,
  		FOREIGN KEY(ekey) REFERENCES Exam(ekey), 
		CHECK (scoreNEW >= 0),	 
		CHECK (scoreNEW <= 100),
		CHECK (scoreOLD >= 0),	 
		CHECK (scoreOLD <= 100)	 	 
);