--
-- Name: contributor; Type: TABLE; Schema: public; Owner: -; Tablespace:
--

CREATE TABLE contributor (
    id bigint NOT NULL,
    organization_id bigint NOT NULL,
    snapshot_date timestamp NOT NULL,
    name character varying(255),
    organization_name character varying(255),
    organizational_commits_count integer,
    organizational_projects_count integer,
    personal_commits_count integer,
    personal_projects_count integer,
    url character varying(255),
    PRIMARY KEY (id, organization_id, snapshot_date)
);


--
-- Name: project; Type: TABLE; Schema: public; Owner: -; Tablespace:
--

CREATE TABLE project (
    id serial primary key,
    commits_count integer,
    contributors_count integer,
    description character varying(255),
    forks_count integer,
    git_hub_project_id bigint,
    last_pushed character varying(255),
    name character varying(255),
    organization_name character varying(255),
    primary_language character varying(255),
    score integer,
    snapshot_date timestamp,
    stars_count integer,
    url character varying(255)
);


--
-- Name: statistics; Type: TABLE; Schema: public; Owner: -; Tablespace:
--

CREATE TABLE statistics (
    id bigint NOT NULL,
    snapshot_date timestamp NOT NULL,
    all_contributors_count integer,
    all_forks_count integer,
    all_size_count integer,
    all_stars_count integer,
    members_count integer,
    organization_name character varying(255),
    private_project_count integer,
    program_languages_count integer,
    public_project_count integer,
    tags_count integer,
    teams_count integer,
    PRIMARY KEY (id, snapshot_date)
);


--
-- Name: language_list; Type: TABLE; Schema: public; Owner: -; Tablespace:
--

CREATE TABLE language_list (
    project_id integer NOT NULL REFERENCES project(id),
    language character varying(255)
);