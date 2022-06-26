DROP TABLE IF EXISTS Allergies CASCADE;

DROP TABLE IF EXISTS DeclaredHCP CASCADE;
DROP TABLE IF EXISTS FakeEmail CASCADE;
DROP TABLE IF EXISTS GlobalVariables CASCADE;
DROP TABLE IF EXISTS HCPAssignedHos CASCADE;
DROP TABLE IF EXISTS HCPRelations CASCADE;
DROP TABLE IF EXISTS ReferralMessage CASCADE;

DROP TABLE IF EXISTS LabProcedure CASCADE;
DROP TABLE IF EXISTS LoginFailures CASCADE;
DROP TABLE IF EXISTS LOINC CASCADE;
DROP TABLE IF EXISTS message CASCADE;
DROP TABLE IF EXISTS referrals CASCADE;
DROP TABLE IF EXISTS AdverseEvents CASCADE;

DROP TABLE IF EXISTS OVDiagnosis CASCADE;
DROP TABLE IF EXISTS OVMedication CASCADE;
DROP TABLE IF EXISTS OVReactionOverride CASCADE;
DROP TABLE IF EXISTS OVProcedure CASCADE;
DROP TABLE IF EXISTS OVSurvey CASCADE;

DROP TABLE IF EXISTS Appointment CASCADE;
DROP TABLE IF EXISTS AppointmentType CASCADE;

DROP TABLE IF EXISTS ReportRequests CASCADE;
DROP TABLE IF EXISTS Representatives CASCADE;
DROP TABLE IF EXISTS ResetPasswordFailures CASCADE;
DROP TABLE IF EXISTS TransactionLog CASCADE;

DROP TABLE IF EXISTS Medication CASCADE;
DROP TABLE IF EXISTS PersonalAllergies CASCADE;
DROP TABLE IF EXISTS PersonalHealthInformation CASCADE;
DROP TABLE IF EXISTS PersonalImmunizations CASCADE;
DROP TABLE IF EXISTS PersonalRelations CASCADE;
DROP TABLE IF EXISTS PersonalStaticInformation CASCADE;
DROP TABLE IF EXISTS TransactionFailureAttempts CASCADE;

DROP TABLE IF EXISTS CPTCodes CASCADE;
DROP TABLE IF EXISTS ICDCodes CASCADE;
DROP TABLE IF EXISTS DrugReactionOverrideCodes CASCADE;
DROP TABLE IF EXISTS OfficeVisits CASCADE;
DROP TABLE IF EXISTS Hospitals CASCADE;
DROP TABLE IF EXISTS Personnel CASCADE;
DROP TABLE IF EXISTS NDCodes CASCADE;
DROP TABLE IF EXISTS DrugInteractions CASCADE;
DROP TABLE IF EXISTS Patients CASCADE;
DROP TABLE IF EXISTS HistoryPatients CASCADE;
DROP TABLE IF EXISTS Users CASCADE;
DROP TABLE IF EXISTS UserPrefs CASCADE;

DROP TABLE IF EXISTS RemoteMonitoringData CASCADE;
DROP TABLE IF EXISTS RemoteMonitoringLists CASCADE;

DROP TABLE IF EXISTS ProfilePhotos CASCADE;

DROP TABLE IF EXISTS PatientSpecificInstructions CASCADE;

CREATE TABLE Users(
	MID                 INT ,
	Password            VARCHAR(20),
	Role                VARCHAR(20) NOT NULL,
	sQuestion           VARCHAR(100), 
	sAnswer             VARCHAR(30),
	PRIMARY KEY (MID),
	CHECK (Role IN ('patient','admin','hcp','uap','er','tester','pha', 'lt'))	
);

CREATE TABLE Hospitals(
	HospitalID   varchar(10),
	HospitalName varchar(30) NOT NULL, 
	PRIMARY KEY (hospitalID)
);

CREATE TABLE Personnel(
	MID INT default NULL,
	AMID INT default NULL,
	role VARCHAR(20) NOT NULL,
	enabled int NOT NULL,
	lastName varchar(20) NOT NULL,
	firstName varchar(20) NOT NULL,
	address1 varchar(20) NOT NULL,
	address2 varchar(20) NOT NULL,
	city varchar(15) NOT NULL,
	state VARCHAR(2) NOT NULL,
	zip varchar(10),
	zip1 varchar(5),
	zip2 varchar(4),
	phone varchar(12),
	phone1 varchar(3),
	phone2 varchar(3),
	phone3 varchar(4),
	specialty varchar(40),
	email varchar(55),
	MessageFilter varchar(60),
	PRIMARY KEY  (MID),
	CHECK (role IN ('admin','hcp','uap','er','tester','pha', 'lt')),
	CHECK (state IN ('','AK','AL','AR','AZ','CA','CO','CT','DE','DC','FL','GA','HI','IA','ID','IL','IN','KS','KY','LA','MA','MD','ME','MI','MN','MO','MS','MT','NC','ND','NE','NH','NJ','NM','NV','NY','OH','OK','OR','PA','RI','SC','SD','TN','TX','UT','VA','VT','WA','WI','WV','WY'))	
);

CREATE TABLE Patients(
	MID INT   , 
	lastName varchar(20), 
	firstName varchar(20),
	email varchar(55),
	address1 varchar(20),
	address2 varchar(20),
	city varchar(15),
	state VARCHAR(2),
	zip1 varchar(5),
	zip2 varchar(4),
	phone1 varchar(3),
    phone2 varchar(3),
    phone3 varchar(4),
	eName varchar(40),
	ePhone1 varchar(3),
	ePhone2 varchar(3),
	ePhone3 varchar(4),
	iCName varchar(20),
	iCAddress1 varchar(20),
	iCAddress2 varchar(20),
	iCCity varchar(15),
	ICState VARCHAR(2),
	iCZip1 varchar(5),
	iCZip2 varchar(4),
	iCPhone1 varchar(3),
	iCPhone2 varchar(3),
	iCPhone3 varchar(4),			
	iCID varchar(20), 
	DateOfBirth DATE,
	DateOfDeath DATE,
	CauseOfDeath VARCHAR(10),
	MotherMID INTEGER,
	FatherMID INTEGER,
	BloodType VARCHAR(3),
	Ethnicity VARCHAR(20),
	Gender VARCHAR(13),
	TopicalNotes VARCHAR(200),
	CreditCardType VARCHAR(20),
	CreditCardNumber VARCHAR(19),
	MessageFilter varchar(60),
	DirectionsToHome varchar(512),
	Religion varchar(64),
	Language varchar(32),
	SpiritualPractices varchar(100),
	AlternateName varchar(32),
	PRIMARY KEY (MID),
	CHECK (state IN ('AK','AL','AR','AZ','CA','CO','CT','DE','DC','FL','GA','HI','IA','ID','IL','IN','KS','KY','LA','MA','MD','ME','MI','MN','MO','MS','MT','NC','ND','NE','NH','NJ','NM','NV','NY','OH','OK','OR','PA','RI','SC','SD','TN','TX','UT','VA','VT','WA','WI','WV','WY')),		
	CHECK (ICstate IN ('AK','AL','AR','AZ','CA','CO','CT','DE','DC','FL','GA','HI','IA','ID','IL','IN','KS','KY','LA','MA','MD','ME','MI','MN','MO','MS','MT','NC','ND','NE','NH','NJ','NM','NV','NY','OH','OK','OR','PA','RI','SC','SD','TN','TX','UT','VA','VT','WA','WI','WV','WY'))
);

CREATE TABLE HistoryPatients(
	ID INT   ,
	changeDate date NOT NULL,
	changeMID INT  NOT NULL,
	MID INT  NOT NULL, 
	lastName varchar(20)  , 
	firstName varchar(20)  , 
	email varchar(55)  , 
	address1 varchar(20)  , 
	address2 varchar(20)  , 
	city varchar(15)  , 
	state CHAR(2) , 
	zip1 varchar(5)  , 
	zip2 varchar(4)  ,
	phone1 varchar(3) ,
    	phone2 varchar(3) ,
    	phone3 varchar(4) ,
	eName varchar(40)  , 
	ePhone1 varchar(3)  , 
	ePhone2 varchar(3)  , 		
	ePhone3 varchar(4)  , 	
	iCName varchar(20)  , 
	iCAddress1 varchar(20)  , 
	iCAddress2 varchar(20)  , 
	iCCity varchar(15)  , 
	ICState VARCHAR(2) , 
	iCZip1 varchar(5)  , 
	iCZip2 varchar(4)  ,
	iCPhone1 varchar(3)  ,
	iCPhone2 varchar(3)  ,
	iCPhone3 varchar(4)  ,			
	iCID varchar(20)  , 
	DateOfBirth date,
	DateOfDeath date,
	CauseOfDeath VARCHAR(10) ,
	MotherMID INTEGER,
	FatherMID INTEGER,
	BloodType VARCHAR(3) ,
	Ethnicity VARCHAR(20) ,
	Gender VARCHAR(13),
	TopicalNotes VARCHAR(200) ,
	CreditCardType VARCHAR(20) ,
	CreditCardNumber VARCHAR(19) ,
	MessageFilter varchar(60) ,
	DirectionsToHome varchar(100) ,
	Religion varchar(64) ,
	Language varchar(32) ,
	SpiritualPractices varchar(100) ,
	AlternateName varchar(32),
	PRIMARY KEY (ID),
	CHECK (state IN ('AK','AL','AR','AZ','CA','CO','CT','DE','DC','FL','GA','HI','IA','ID','IL','IN','KS','KY','LA','MA','MD','ME','MI','MN','MO','MS','MT','NC','ND','NE','NH','NJ','NM','NV','NY','OH','OK','OR','PA','RI','SC','SD','TN','TX','UT','VA','VT','WA','WI','WV','WY')),		
	CHECK (ICstate IN ('AK','AL','AR','AZ','CA','CO','CT','DE','DC','FL','GA','HI','IA','ID','IL','IN','KS','KY','LA','MA','MD','ME','MI','MN','MO','MS','MT','NC','ND','NE','NH','NJ','NM','NV','NY','OH','OK','OR','PA','RI','SC','SD','TN','TX','UT','VA','VT','WA','WI','WV','WY'))	
);

CREATE TABLE LoginFailures(
	ipaddress varchar(100) NOT NULL, 
	failureCount int NOT NULL, 
	lastFailure TIMESTAMP NOT NULL,
	PRIMARY KEY (ipaddress)
);

CREATE TABLE ResetPasswordFailures(
	ipaddress varchar(128) NOT NULL, 
	failureCount int NOT NULL, 
	lastFailure TIMESTAMP NOT NULL,
	PRIMARY KEY (ipaddress)
);

CREATE TABLE icdcodes (
  Code numeric(5,2) NOT NULL,
  Description VARCHAR(50) NOT NULL,
  Chronic VARCHAR(3) NOT NULL,
  PRIMARY KEY (Code),
  CHECK (Chronic IN ('no','yes'))		
);

CREATE TABLE CPTCodes(
	Code varchar(5) NOT NULL,
	Description varchar(30) NOT NULL,
	Attribute varchar(30),
	PRIMARY KEY (Code)
);

CREATE TABLE DrugReactionOverrideCodes(
	Code varchar(5) NOT NULL,
	Description varchar(80) NOT NULL,
	PRIMARY KEY (Code)
);
	
CREATE TABLE NDCodes(
	Code varchar(10) NOT NULL, 
	Description varchar(100) NOT NULL, 
	PRIMARY KEY  (Code)
);

CREATE TABLE DrugInteractions(
	FirstDrug varchar(9) NOT NULL,
	SecondDrug varchar(9) NOT NULL,
	Description varchar(100) NOT NULL,
	PRIMARY KEY  (FirstDrug,SecondDrug)
);

CREATE TABLE TransactionLog(
	transactionID int NOT NULL, 
	loggedInMID INT NOT NULL, 
	secondaryMID INT NOT NULL, 
	transactionCode int NOT NULL, 
	timeLogged timestamp NOT NULL,
	addedInfo VARCHAR(255),
	PRIMARY KEY (transactionID)
);

CREATE TABLE HCPRelations(
	HCP INT NOT NULL, 
	UAP INT NOT NULL,
	PRIMARY KEY (HCP, UAP)
);

CREATE TABLE PersonalRelations(
	PatientID INT NOT NULL,
	RelativeID INT NOT NULL,
	RelativeType VARCHAR( 35 ) NOT NULL 
);

CREATE TABLE Representatives(
	representerMID INT, 
	representeeMID INT,
	PRIMARY KEY  (representerMID,representeeMID)
);

CREATE TABLE HCPAssignedHos(
	hosID VARCHAR(10) NOT NULL, 
	HCPID INT  NOT NULL, 
	PRIMARY KEY (hosID,HCPID)
);

CREATE TABLE DeclaredHCP(
	PatientID INT NOT NULL, 
	HCPID INT NOT NULL, 
	PRIMARY KEY  (PatientID,HCPID)
);

CREATE TABLE OfficeVisits(
	ID int primary key,
	visitDate date,  
	HCPID INT, 
	notes varchar(50), 
	PatientID INT, 
	HospitalID VARCHAR(10)
);

CREATE TABLE PersonalHealthInformation (
	PatientID INT  NOT NULL,
	Height int,  
	Weight int,  
	Smoker int NOT NULL,
	SmokingStatus int NOT NULL,
	BloodPressureN int,  
	BloodPressureD int,  
	CholesterolHDL int,
	CholesterolLDL int,
	CholesterolTri int,
	HCPID int,  
	AsOfDate timestamp NOT NULL 
);

CREATE TABLE PersonalAllergies(
	PatientID INT  NOT NULL,
	Allergy VARCHAR( 50 ) NOT NULL 
);

CREATE TABLE Allergies(
	ID INT primary key,
	PatientID INT NOT NULL,
	Description VARCHAR( 50 ) NOT NULL,
	FirstFound TIMESTAMP NOT NULL
);

CREATE TABLE OVProcedure(
	ID INT primary key,
	VisitID INT  NOT NULL,
	CPTCode VARCHAR( 5 ) NOT NULL,
	HCPID VARCHAR( 10 ) NOT NULL
);

CREATE TABLE OVMedication (
    ID INT primary key,
	VisitID INT  NOT NULL,
	NDCode VARCHAR( 9 ) NOT NULL,
	StartDate DATE,
	EndDate DATE,
	Dosage INT,
	Instructions VARCHAR(500)
);

CREATE TABLE OVReactionOverride (
	ID INT primary key,
	OVMedicationID INT NOT NULL,
	OverrideCode VARCHAR(5),
	OverrideComment VARCHAR(255),
	FOREIGN KEY (OVMedicationID) REFERENCES OVMedication (ID)
);

CREATE TABLE OVDiagnosis (
    ID INT primary key,
	VisitID INT  NOT NULL,
	ICDCode DECIMAL( 5, 2 ) NOT NULL
);

CREATE TABLE GlobalVariables (
	Name VARCHAR(20) primary key,
	Value VARCHAR(20)
);

CREATE TABLE FakeEmail(
	ID INT primary key,
	ToAddr VARCHAR(100),
	FromAddr VARCHAR(100),
	Subject VARCHAR(500),
	Body VARCHAR(2000),
	AddedDate timestamp NOT NULL
);

CREATE TABLE ReportRequests (
    ID INT primary key,
    RequesterMID INT ,
    PatientMID INT ,
    ApproverMID INT ,
    RequestedDate timestamp,
    ApprovedDate timestamp,
    ViewedDate timestamp,
    Status varchar(30),
    Comment VARCHAR(50)
);

CREATE TABLE OVSurvey (
	VisitID int  primary key,
	SurveyDate timestamp not null,
	WaitingRoomMinutes int,
	ExamRoomMinutes int,
  	VisitSatisfaction int,	
	TreatmentSatisfaction int
);

CREATE TABLE LOINC (
	LaboratoryProcedureCode VARCHAR (7), 
	Component VARCHAR(100),
	KindOfProperty VARCHAR(100),
	TimeAspect VARCHAR(100),
	System VARCHAR(100),
	ScaleType VARCHAR(100),
	MethodType VARCHAR(100)
);

CREATE TABLE LabProcedure (
	LaboratoryProcedureID INT primary key,
	PatientMID int , 
	LaboratoryProcedureCode VARCHAR (7), 
	Rights VARCHAR(10),
	Status VARCHAR(20),
	Commentary VARCHAR(50),
	Results VARCHAR(50),
	NumericalResults VARCHAR(20),
	NumericalResultsUnit VARCHAR(20),
	UpperBound VARCHAR(20),
	LowerBound VARCHAR(20),	
	OfficeVisitID INT ,
	LabTechID INT,
	PriorityCode INT ,
	ViewedByPatient BOOLEAN NOT NULL,
	UpdatedDate timestamp NOT NULL
);

CREATE TABLE message (
	message_id          INT ,
	parent_msg_id       INT ,
	from_id             INT  NOT NULL,
	to_id               INT  NOT NULL,
	sent_date           TIMESTAMP NOT NULL,
	message             VARCHAR(50),
	subject				VARCHAR(50),
	been_read			INT 
	
);

CREATE TABLE Appointment (
	appt_id				INT primary key,
	doctor_id           INT  NOT NULL,
	patient_id          INT  NOT NULL,
	sched_date          TIMESTAMP NOT NULL,
	appt_type           VARCHAR(30) NOT NULL,
	comment				VARCHAR(50)
);

CREATE TABLE AppointmentType (
	apptType_id			INT primary key,
	appt_type           VARCHAR(30) NOT NULL,
	duration			INT  NOT NULL
);

CREATE TABLE referrals (
	id          INT,
	PatientID          int  NOT NULL,
	SenderID               int  NOT NULL,
	ReceiverID           int  NOT NULL,
	ReferralDetails             VARCHAR(50),
	OVID		int  NOT NULL,
	viewed_by_patient 	boolean NOT NULL,
	viewed_by_HCP 	boolean NOT NULL,
	TimeStamp TIMESTAMP NOT NULL,
	PriorityCode INT ,
	PRIMARY KEY (id)
);

CREATE TABLE RemoteMonitoringData (
	id          INT,
	PatientID          int  NOT NULL,
	systolicBloodPressure int,
	diastolicBloodPressure int,
	glucoseLevel int,
	height int,
	weight int,
	pedometerReading int,
	timeLogged timestamp NOT NULL,
	ReporterRole		varchar(50),
	ReporterID INT,
	PRIMARY KEY (id)
);

CREATE TABLE RemoteMonitoringLists (
	PatientMID int  default 0, 
	HCPMID int  default 0,
	SystolicBloodPressure BOOLEAN,
	DiastolicBloodPressure BOOLEAN,
	GlucoseLevel BOOLEAN,
	Height BOOLEAN,
	Weight BOOLEAN,
	PedometerReading BOOLEAN,
	PRIMARY KEY  (PatientMID,HCPMID)
);

CREATE TABLE AdverseEvents (
	id INT primary key,
	Status VARCHAR(10),
	PatientMID INT,
	PresImmu VARCHAR(50),
	Code VARCHAR(20),
	Comment VARCHAR(2000),
	Prescriber INT,
	TimeLogged timestamp
);

CREATE TABLE ProfilePhotos (
	MID int primary key,
	Photo varchar(50),
	UpdatedDate timestamp NOT NULL
);

CREATE TABLE PatientSpecificInstructions (
    id int primary key,
    VisitID INT,
    Modified TIMESTAMP NOT NULL,
    Name VARCHAR(100),
    URL VARCHAR(250),
    Comment VARCHAR(500)
);

CREATE TABLE ReferralMessage(
	messageID  INT  NOT NULL, 
	referralID INT  NOT NULL, 
	PRIMARY KEY (messageID,referralID)
);


