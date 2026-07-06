package org.evomaster.client.java.instrumentation.example.cassandra;

import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.mapping.CassandraPersistentEntity;
import org.springframework.data.cassandra.repository.support.MappingCassandraEntityInformation;

public interface MappingCassandraEntityOperations {
    <T, ID> MappingCassandraEntityInformation<?, ?> callMappingCassandraEntityInformation(CassandraPersistentEntity<T> entity, CassandraConverter converter);
}