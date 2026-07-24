package com.foo.somedifferentpackage.examples.methodreplacement;

import org.evomaster.client.java.instrumentation.example.cassandra.MappingCassandraEntityOperations;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.mapping.CassandraPersistentEntity;
import org.springframework.data.cassandra.repository.support.MappingCassandraEntityInformation;

public class MappingCassandraEntityOperationsImpl implements MappingCassandraEntityOperations {
    @Override
    public <T, ID> MappingCassandraEntityInformation<?, ?> callMappingCassandraEntityInformation(CassandraPersistentEntity<T> entity, CassandraConverter converter) {
        return new MappingCassandraEntityInformation<>(entity, converter);
    }
}