namespace java com.foo.rpc.examples.spring.db.base

struct DbBaseDto {
    1: i64 id,
    2: string name
}

service DbBaseService {

    i64 create(1: required DbBaseDto dto),

    list<DbBaseDto> getAll(),

    DbBaseDto get(1: i64 id),

    list<DbBaseDto> getByName(1: string name)
}