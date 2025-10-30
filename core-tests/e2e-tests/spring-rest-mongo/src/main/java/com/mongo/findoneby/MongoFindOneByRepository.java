package com.mongo.findoneby;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoFindOneByRepository extends MongoRepository<Session,String> {
    Session findOneBySourceAndTypeAndId(String source, SessionType type, String id);
    Session findOneById(String id);
    Session findOneByType(SessionType sessionType);
    Session findOneBySource(String source);
    Session findOneByIdAndType(String id, SessionType type);
}
