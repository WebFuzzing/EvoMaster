package org.evomaster.client.java.instrumentation.example.mongo;

import com.foo.somedifferentpackage.examples.methodreplacement.MappingMongoEntityOperationsImpl;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.repository.support.MappingMongoEntityInformation;

import static org.junit.jupiter.api.Assertions.*;


public class MappingMongoEntityInstrumentedTest {

    public class SomeType {
        public String someField;
    }

    protected MappingMongoEntityOperations getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (MappingMongoEntityOperations)
                cl.loadClass(MappingMongoEntityOperationsImpl.class.getName())
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

    @Test
    public void testConstructor() throws Exception {
        MappingMongoEntityOperations mongoInstrumented = getInstance();
        MongoPersistentEntity<?> entity = new MongoPersistentEntityMock<>();
        MappingMongoEntityInformation<?, ?> mappingMongoEntityInformation = mongoInstrumented.callMappingMongoEntityInformation(entity, SomeType.class);
        assertNotNull(mappingMongoEntityInformation);
    }
}

