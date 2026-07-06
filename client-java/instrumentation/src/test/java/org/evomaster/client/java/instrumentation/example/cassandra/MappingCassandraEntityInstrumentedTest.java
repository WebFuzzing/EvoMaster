package org.evomaster.client.java.instrumentation.example.cassandra;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.foo.somedifferentpackage.examples.methodreplacement.MappingCassandraEntityOperationsImpl;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.mapping.CassandraPersistentEntity;
import org.springframework.data.cassandra.repository.support.MappingCassandraEntityInformation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class MappingCassandraEntityInstrumentedTest {

    public static class SomeType {
        public String someField;
    }

    protected MappingCassandraEntityOperations getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (MappingCassandraEntityOperations)
                cl.loadClass(MappingCassandraEntityOperationsImpl.class.getName())
                        .newInstance();
    }

    @BeforeEach
    public void init(){
        ExecutionTracer.reset();
        assertEquals(0 , ExecutionTracer.getNumberOfObjectives());
    }

    @AfterEach
    public void checkInstrumentation(){
        assertTrue(ExecutionTracer.getNumberOfObjectives() > 0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConstructor() throws Exception {
        MappingCassandraEntityOperations cassandraInstrumented = getInstance();

        CassandraPersistentEntity<SomeType> entity = mock(CassandraPersistentEntity.class);
        when(entity.getType()).thenReturn(SomeType.class);
        when(entity.getTableName()).thenReturn(CqlIdentifier.fromCql("some_table"));

        CassandraConverter converter = mock(CassandraConverter.class);

        MappingCassandraEntityInformation<?, ?> mappingCassandraEntityInformation =
                cassandraInstrumented.callMappingCassandraEntityInformation(entity, converter);
        assertNotNull(mappingCassandraEntityInformation);
    }
}