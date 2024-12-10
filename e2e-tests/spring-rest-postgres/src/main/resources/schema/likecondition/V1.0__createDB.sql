CREATE TABLE LikeConditionTable (
    id BIGINT,
    name varchar(10) NOT NULL,
    constraint pk_x primary key (id),
    constraint like_foo check ( name LIKE 'foo%' )
);


