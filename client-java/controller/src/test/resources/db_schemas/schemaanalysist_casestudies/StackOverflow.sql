-- http://2013.msrconf.org/

--
-- Name: comments; Type: TABLE; Schema: public; Owner: ponza; Tablespace: 
--

CREATE TABLE comments (
    id integer NOT NULL,
    post_id integer,
    score integer,
    text text,
    creation_date date,
    user_id integer
);

-- ALTER TABLE public.comments OWNER TO ponza;

--
-- Name: posts; Type: TABLE; Schema: public; Owner: ponza; Tablespace: 
--

CREATE TABLE posts (
    id integer NOT NULL,
    post_type_id smallint,
    parent_id integer,
    accepted_answer_id integer,
    creation_date date,
    score smallint,
    view_count integer,
    body text,
    owner_user_id integer,
    last_editor_user_id integer,
    last_editor_display_name text,
    last_edit_date date,
    last_activity_date date,
    community_owned_date date,
    closed_date date,
    title text,
    tags text,
    answer_count smallint,
    comment_count smallint,
    favorite_count integer
);

-- ALTER TABLE public.posts OWNER TO ponza;

--
-- Name: users; Type: TABLE; Schema: public; Owner: ponza; Tablespace: 
--

CREATE TABLE users (
    id integer NOT NULL,
    reputation integer,
    creation_date date,
    display_name text,
    email_hash text,
    last_access_date date,
    website_url text,
    location text,
    age smallint,
    about_me text,
    views integer,
    up_votes integer,
    down_votes integer
);


-- ALTER TABLE public.users OWNER TO ponza;

--
-- Name: votes; Type: TABLE; Schema: public; Owner: ponza; Tablespace: 
--

CREATE TABLE votes (
    id integer NOT NULL,
    post_id integer NOT NULL,
    vote_type_id smallint,
    creation_date date
);

-- ALTER TABLE public.votes OWNER TO ponza;

