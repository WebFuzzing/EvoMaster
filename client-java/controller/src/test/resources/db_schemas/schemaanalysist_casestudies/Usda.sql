DROP TABLE IF EXISTS data_src CASCADE;

DROP TABLE IF EXISTS datsrcln CASCADE;

DROP TABLE IF EXISTS deriv_cd CASCADE;

DROP TABLE IF EXISTS fd_group CASCADE;

DROP TABLE IF EXISTS food_des CASCADE;

DROP TABLE IF EXISTS footnote CASCADE;

DROP TABLE IF EXISTS nut_data CASCADE;

DROP TABLE IF EXISTS nutr_def CASCADE;

DROP TABLE IF EXISTS src_cd CASCADE;

DROP TABLE IF EXISTS weight CASCADE;

CREATE TABLE data_src (
    datasrc_id character(6) NOT NULL,
    authors varchar(100),
    title varchar(100) NOT NULL,
    "year" integer,
    journal varchar(100),
    vol_city varchar(100),
    issue_state varchar(100),
    start_page varchar(100),
    end_page varchar(100)
);

CREATE TABLE datsrcln (
    ndb_no character(5) NOT NULL,
    nutr_no character(3) NOT NULL,
    datasrc_id character(6) NOT NULL
);

CREATE TABLE deriv_cd (
    deriv_cd varchar(100) NOT NULL,
    derivcd_desc varchar(100) NOT NULL
);

CREATE TABLE fd_group (
    fdgrp_cd character(4) NOT NULL,
    fddrp_desc varchar(100) NOT NULL
);

CREATE TABLE food_des (
    ndb_no character(5) NOT NULL,
    fdgrp_cd character(4) NOT NULL,
    long_desc varchar(100) NOT NULL,
    shrt_desc varchar(100) NOT NULL,
    comname varchar(100),
    manufacname varchar(100),
    survey character(1),
    ref_desc varchar(100),
    refuse integer,
    sciname varchar(100),
    n_factor int,
    pro_factor int,
    fat_factor int,
    cho_factor int
);

CREATE TABLE footnote (
    ndb_no character(5) NOT NULL,
    footnt_no character(4) NOT NULL,
    footnt_typ character(1) NOT NULL,
    nutr_no character(3),
    footnt_txt varchar(100) NOT NULL
);

CREATE TABLE nut_data (
    ndb_no character(5) NOT NULL,
    nutr_no character(3) NOT NULL,
    nutr_val int NOT NULL,
    num_data_pts int NOT NULL,
    std_error int,
    src_cd integer NOT NULL,
    deriv_cd varchar(100),
    ref_ndb_no character(5),
    add_nutr_mark character(1),
    num_studies integer,
    min int,
    max int,
    df integer,
    low_eb int,
    up_eb int,
    stat_cmt varchar(100),
    cc character(1)
);

CREATE TABLE nutr_def (
    nutr_no character(3) NOT NULL,
    units varchar(100) NOT NULL,
    tagname varchar(100),
    nutrdesc varchar(100),
    num_dec smallint,
    sr_order integer
);

CREATE TABLE src_cd (
    src_cd integer NOT NULL,
    srccd_desc varchar(100) NOT NULL
);

CREATE TABLE weight (
    ndb_no character(5) NOT NULL,
    seq character(2) NOT NULL,
    amount int NOT NULL,
    msre_desc varchar(100) NOT NULL,
    gm_wgt int NOT NULL,
    num_data_pts integer,
    std_dev int
);
