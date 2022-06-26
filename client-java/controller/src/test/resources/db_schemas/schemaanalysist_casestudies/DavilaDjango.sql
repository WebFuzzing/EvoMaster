-- Source: https://github.com/jabauer/DAVILA

CREATE TABLE coordinate_systems (
    id integer NOT NULL PRIMARY KEY,
    short_name varchar(150) NOT NULL,
    long_name varchar(765) NOT NULL,
    notes varchar(765) NOT NULL,
    reference varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);

CREATE TABLE locations (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    lat integer,
    long integer,
    notes varchar(765) NOT NULL,
    coordinate_system_id integer FOREIGN KEY REFERENCES coordinate_systems,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE regions (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE in_regions (
    id integer NOT NULL PRIMARY KEY,
    location_id integer FOREIGN KEY REFERENCES locations,
    region_id integer FOREIGN KEY REFERENCES regions,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE continents (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE states (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    continent_id integer NOT NULL FOREIGN KEY REFERENCES continents,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE in_states (
    id integer NOT NULL PRIMARY KEY,
    location_id integer FOREIGN KEY REFERENCES locations,
    state_id integer FOREIGN KEY REFERENCES states,
    start_year integer,
    end_year integer,
    notes varchar(765)  NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE empires (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE in_empires (
    id integer NOT NULL PRIMARY KEY,
    state_id integer FOREIGN KEY REFERENCES states,
    empire_id integer FOREIGN KEY REFERENCES empires,
    start_year integer,
    end_year integer,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE individuals (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    sex varchar(765) NOT NULL,
    birth_date integer,
    death_date integer,
    state_id integer FOREIGN KEY REFERENCES states,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime,
    sort_name varchar(765) NOT NULL,
    american integer,
    birth_day_known integer,
    birth_month_know integer,
    birth_year_known integer,
    death_day_known integer,
    death_month_known integer,
    death_year_known integer
);
CREATE TABLE residence_types (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    temporary integer,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE residences (
    id integer NOT NULL PRIMARY KEY,
    individual_id integer FOREIGN KEY REFERENCES individuals,
    location_id integer FOREIGN KEY REFERENCES locations,
    residence_type_id integer FOREIGN KEY REFERENCES residence_types,
    start_year integer,
    end_year integer,
    birth_place integer,
    death_place integer,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE occupation_types (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE occupation_titles (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    occupation_type_id integer FOREIGN KEY REFERENCES occupation_types,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE occupations (
    id integer NOT NULL PRIMARY KEY,
    individual_id integer FOREIGN KEY REFERENCES individuals,
    occupation_title_id integer FOREIGN KEY REFERENCES occupation_titles,
    start_year integer,
    end_year integer,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE relationship_types (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE relationships (
    id integer NOT NULL PRIMARY KEY,
    individual_id_1 integer FOREIGN KEY REFERENCES individuals,
    individual_id_2 integer FOREIGN KEY REFERENCES individuals,
    relationship_type_id integer FOREIGN KEY REFERENCES relationship_types,
    start_year integer,
    end_year integer,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE assignment_types (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE assignment_titles (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    assignment_type_id integer FOREIGN KEY REFERENCES assignment_types,
    temporary integer,
    commissioned integer,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE assignments (
    id integer NOT NULL PRIMARY KEY,
    individual_id integer FOREIGN KEY REFERENCES individuals,
    assignment_title_id integer FOREIGN KEY REFERENCES assignment_titles,
    location_id integer FOREIGN KEY REFERENCES locations,
    start_year integer,
    start_certain integer,
    end_year integer,
    end_certain integer,
    notes varchar(765) NOT NULL
);
CREATE TABLE organization_types (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE organizations (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    start_year integer,
    end_year integer,
    magazine_sending integer,
    organization_type_id integer FOREIGN KEY REFERENCES organization_types,
    location_id integer FOREIGN KEY REFERENCES locations,
    org_bio varchar(765) NOT NULL,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE org_evolution_types (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE org_evolutions (
    id integer NOT NULL PRIMARY KEY,
    org_1_id integer FOREIGN KEY REFERENCES organizations,
    org_2_id integer FOREIGN KEY REFERENCES organizations,
    org_evolution_type_id integer FOREIGN KEY REFERENCES org_evolution_types,
    date integer,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime,
    day_known integer,
    month_known integer,
    year_known integer
);
CREATE TABLE role_types (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE role_titles (
    id integer NOT NULL PRIMARY KEY,
    name varchar(765) NOT NULL,
    role_type_id integer FOREIGN KEY REFERENCES role_types,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE members (
    id integer NOT NULL PRIMARY KEY,
    individual_id integer FOREIGN KEY REFERENCES individuals,
    organization_id integer FOREIGN KEY REFERENCES organizations,
    role_title_id integer FOREIGN KEY REFERENCES role_titles,
    start_year integer,
    end_year integer,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE letters (
    id integer NOT NULL PRIMARY KEY,
    from_individual_id integer FOREIGN KEY REFERENCES individuals,
    from_organization_id integer FOREIGN KEY REFERENCES organizations,
    from_location_id integer FOREIGN KEY REFERENCES locations,
    to_individual_id integer FOREIGN KEY REFERENCES individuals,
    to_organization_id integer FOREIGN KEY REFERENCES organizations,
    to_location_id integer FOREIGN KEY REFERENCES locations,
    circular integer,
    date_sent integer,
    date_received integer,
    date_docketed integer,
    notes varchar(765) NOT NULL,
    title varchar(765) NOT NULL,
    sent_day_known integer,
    sent_month_known integer,
    sent_year_known integer,
    received_day_known integer,
    received_month_known integer,
    received_year_known integer,
    docketed_day_known integer,
    docketed_month_known integer,
    docketed_year_known integer,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE enclosures (
    id integer NOT NULL PRIMARY KEY,
    main_letter_id integer FOREIGN KEY REFERENCES letters,
    enclosed_letter_id integer FOREIGN KEY REFERENCES letters,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE bibliographies (
    id integer NOT NULL PRIMARY KEY,
    entry varchar(765) NOT NULL,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE citations (
    id integer NOT NULL PRIMARY KEY,
    title varchar(765) NOT NULL,
    bibliography_id integer FOREIGN KEY REFERENCES bibliographies,
    pages varchar(765) NOT NULL,
    canonic_url varchar(765) NOT NULL,
    notes varchar(765) NOT NULL,
    created_at datetime,
    updated_at datetime
);
CREATE TABLE validations (
    id integer NOT NULL PRIMARY KEY,
    auth_user_id integer,
    content_type_id integer NOT NULL,
    object_id integer NOT NULL,
    supports integer,
    citation_id integer FOREIGN KEY REFERENCES citations,
    notes varchar(765) NOT NULL,
);;
