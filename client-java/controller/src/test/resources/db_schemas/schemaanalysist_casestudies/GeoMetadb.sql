-- This database can only initially be accessed through R.  
-- You have to go through several steps to use the bioconductor system.
-- 
-- http://www.bioconductor.org/packages/2.12/bioc/html/GEOmetadb.html
-- 

-- Note that there was one strange variable that did not contain a type.
-- Sqlite outputs it this way and it would not parse, I changed it to work. (Strange)
-- 

-- CREATE TABLE geoConvert(
--   from_acc TEXT,
--   to_acc TEXT,
--   to_type
-- );
-- 

CREATE TABLE gds 
( ID REAL,
	gds TEXT,
	title TEXT,
	description TEXT,
	type TEXT,
	pubmed_id TEXT,
	gpl TEXT,
	platform_organism TEXT,
	platform_technology_type TEXT,
	feature_count INTEGER,
	sample_organism TEXT,
	sample_type TEXT,
	channel_count TEXT,
	sample_count INTEGER,
	value_type TEXT,
	gse TEXT,
	"order" TEXT,
	update_date TEXT 
);
CREATE TABLE gds_subset 
( ID REAL,
	Name TEXT,
	gds TEXT,
	description TEXT,
	sample_id TEXT,
	type TEXT 
);
CREATE TABLE geoConvert(
  from_acc TEXT,
  to_acc TEXT,
  to_type TEXT
);
CREATE TABLE geodb_column_desc 
( TableName TEXT,
	FieldName TEXT,
	Description TEXT 
);
CREATE TABLE gpl 
( ID REAL,
	title TEXT,
	gpl TEXT,
	status TEXT,
	submission_date TEXT,
	last_update_date TEXT,
	technology TEXT,
	distribution TEXT,
	organism TEXT,
	manufacturer TEXT,
	manufacture_protocol TEXT,
	coating TEXT,
	catalog_number TEXT,
	support TEXT,
	description TEXT,
	web_link TEXT,
	contact TEXT,
	data_row_count REAL,
	supplementary_file TEXT,
	bioc_package TEXT 
);
CREATE TABLE gse 
( ID REAL,
	title TEXT,
	gse TEXT,
	status TEXT,
	submission_date TEXT,
	last_update_date TEXT,
	pubmed_id INTEGER,
	summary TEXT,
	type TEXT,
	contributor TEXT,
	web_link TEXT,
	overall_design TEXT,
	repeats TEXT,
	repeats_sample_list TEXT,
	variable TEXT,
	variable_description TEXT,
	contact TEXT,
	supplementary_file TEXT 
);
CREATE TABLE gse_gpl 
( gse TEXT,
	gpl TEXT 
);
CREATE TABLE gse_gsm 
( gse TEXT,
	gsm TEXT 
);
CREATE TABLE gsm 
( ID REAL,
	title TEXT,
	gsm TEXT,
	series_id TEXT,
	gpl TEXT,
	status TEXT,
	submission_date TEXT,
	last_update_date TEXT,
	type TEXT,
	source_name_ch1 TEXT,
	organism_ch1 TEXT,
	characteristics_ch1 TEXT,
	molecule_ch1 TEXT,
	label_ch1 TEXT,
	treatment_protocol_ch1 TEXT,
	extract_protocol_ch1 TEXT,
	label_protocol_ch1 TEXT,
	source_name_ch2 TEXT,
	organism_ch2 TEXT,
	characteristics_ch2 TEXT,
	molecule_ch2 TEXT,
	label_ch2 TEXT,
	treatment_protocol_ch2 TEXT,
	extract_protocol_ch2 TEXT,
	label_protocol_ch2 TEXT,
	hyb_protocol TEXT,
	description TEXT,
	data_processing TEXT,
	contact TEXT,
	supplementary_file TEXT,
	data_row_count REAL,
	channel_count REAL 
);
CREATE TABLE metaInfo (name varchar(50), value varchar(50));
CREATE TABLE sMatrix 
( ID INTEGER,
	sMatrix TEXT,
	gse TEXT,
	gpl TEXT,
	GSM_Count INTEGER,
	Last_Update_Date TEXT 
);
CREATE INDEX column_desc_idx on geodb_column_desc (TableName,FieldName);
CREATE INDEX gds_acc_idx on gds (gds);
CREATE INDEX gds_subset_idx on gds_subset (gds);
CREATE INDEX gpl_acc_idx on gpl (gpl);
CREATE INDEX gse_acc_idx on gse (gse);
CREATE INDEX gse_gpl_idx1 on gse_gpl (gse);
CREATE INDEX gse_gpl_idx2 on gse_gpl (gpl);
CREATE INDEX gse_gsm_idx1 on gse_gsm (gse);
CREATE INDEX gse_gsm_idx2 on gse_gsm (gsm);
CREATE INDEX gsm_acc_idx on gsm (gsm);
CREATE INDEX sMatrix_GPL_idx on sMatrix (gpl);
CREATE INDEX sMatrix_GSE_idx on sMatrix (gse);
CREATE INDEX sMatrix_Last_Update_Date_idx on sMatrix (Last_Update_Date);
CREATE INDEX sMatrix_Name_idx on sMatrix (sMatrix);
