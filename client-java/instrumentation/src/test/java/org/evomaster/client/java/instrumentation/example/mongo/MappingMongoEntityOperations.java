package org.evomaster.client.java.instrumentation.example.mongo;

import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.repository.support.MappingMongoEntityInformation;
import org.springframework.lang.Nullable;

public interface MappingMongoEntityOperations {
    <T, ID> MappingMongoEntityInformation<?, ?> callMappingMongoEntityInformation(MongoPersistentEntity<T> entity, @Nullable Class<ID> fallbackIdType);
}
