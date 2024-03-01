package org.evomaster.client.java.instrumentation.example.mongo;

import com.foo.somedifferentpackage.examples.methodreplacement.MongoOperationsImpl;
import com.mongodb.client.*;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class MongoInstrumentedTest {

    protected MongoOperations getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (MongoOperations)
                cl.loadClass(MongoOperationsImpl.class.getName())
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
    public void testFind() throws Exception {

        MongoOperations mongoInstrumented = getInstance();

        Document document = new Document("name", "Sun Bakery Trattoria");
        MongoCollectionMock collection = new MongoCollectionMock();
        FindIterableMock findIterable = new FindIterableMock();
        findIterable.add(document);
        collection.mockFindResult(findIterable);
        Bson bson = new BsonDocument();
        FindIterable<?> res = mongoInstrumented.callFind(collection, bson);
        assertTrue(res.iterator().hasNext());
    }
}

