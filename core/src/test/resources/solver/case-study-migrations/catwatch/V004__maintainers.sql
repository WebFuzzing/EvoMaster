CREATE TABLE maintainers (
    project_id integer NOT NULL REFERENCES project(id),
    maintainer text
);
