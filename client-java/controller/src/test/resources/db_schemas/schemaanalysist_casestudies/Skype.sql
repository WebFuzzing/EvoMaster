CREATE TABLE bupdate (
    uri varchar(256),
    type varchar(256),
    meta varchar(256),
    prio integer,
    id integer,
    body varchar(256),
    terms varchar(256)
);

CREATE TABLE events (
    id integer,
    event integer,
    event_time integer
);

CREATE TABLE install (
    uri varchar(256),
    type varchar(256),
    meta varchar(256),
    prio integer,
    id integer,
    body varchar(256),
    terms varchar(256)
);

CREATE TABLE itemkeys (
    id integer,
    keys varchar(256)
);

CREATE TABLE reported (
    id integer,
    exposed_cnt integer,
    exposed_time integer,
    exec_cnt integer,
    close_cnt integer
);

CREATE TABLE stats (
    id integer,
    exposed_cnt integer,
    exposed_time integer,
    exec_cnt integer,
    close_cnt integer,
    last_expose_start integer,
    exposing_now integer
);

CREATE INDEX bupdate_id on bupdate (id);

CREATE INDEX bupdate_uri on bupdate (uri);

CREATE INDEX events_id on events (id);

CREATE INDEX install_id on install (id);

CREATE INDEX install_uri on install (uri);

CREATE INDEX itemkeys_id on itemkeys (id);

CREATE INDEX reported_id on reported(id);

CREATE INDEX stats_id on stats (id);
