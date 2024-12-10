package com.foo.somedifferentpackage.examples.methodreplacement;

import org.evomaster.client.java.instrumentation.example.mongo.MappingMongoEntityOperations;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.repository.support.MappingMongoEntityInformation;
import org.springframework.lang.Nullable;

public class MappingMongoEntityOperationsImpl implements MappingMongoEntityOperations {
    @Override
    public <T, ID> MappingMongoEntityInformation<?, ?> callMappingMongoEntityInformation(MongoPersistentEntity<T> entity, @Nullable Class<ID> fallbackIdType){
        return new MappingMongoEntityInformation<>(entity, fallbackIdType);
    }
}
