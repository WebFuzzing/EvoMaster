package org.evomaster.client.java.instrumentation.example.methodreplacement.mongo;

import com.foo.somedifferentpackage.examples.methodreplacement.mongo.MockedMongoCollection;
import com.foo.somedifferentpackage.examples.methodreplacement.mongo.SimpleMongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.conversions.Bson;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.mongo.MongoLogger;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by arcuri82 on 27-Jun-19.
 */
public class MongoInstrumentedTest {

    @Test
    public void testLoadClasses() throws Exception {
        ClassLoader cl = new InstrumentingClassLoader("com.foo");
        Class<?> mockedMongoCollectionClass = cl.loadClass(MockedMongoCollection.class.getName());
        Class<?> mongoCollectionClass= cl.loadClass(MongoCollection.class.getName());
        Class<?> simpleMongoClientClass = cl.loadClass(SimpleMongoClient.class.getName());
        Class<?> bsonClass = cl.loadClass(Bson.class.getName());

        assertNotNull(mockedMongoCollectionClass);
        assertNotNull(mongoCollectionClass);
        assertNotNull(simpleMongoClientClass);
        assertNotNull(bsonClass);

        assertTrue(mongoCollectionClass.isAssignableFrom(mockedMongoCollectionClass));

    }

    @Test
    public void testInvokeFind0() throws Exception {
        ClassLoader cl = new InstrumentingClassLoader("com.foo");
        Class<?> mongoCollectionClass= cl.loadClass(MongoCollection.class.getName());
        Class<?> mockedMongoCollectionClass = cl.loadClass(MockedMongoCollection.class.getName());
        Class<?> simpleMongoClientClass = cl.loadClass(SimpleMongoClient.class.getName());
        Class<?> bsonClass = cl.loadClass(Bson.class.getName());

        Object simpleMongoClientInstance = simpleMongoClientClass.newInstance();
        Object mockedMongoCollectionInstance = mockedMongoCollectionClass.newInstance();

        assertTrue(mockedMongoCollectionClass.isAssignableFrom(mockedMongoCollectionInstance.getClass()));
        assertTrue(mongoCollectionClass.isAssignableFrom(mockedMongoCollectionInstance.getClass()));

        Method m = simpleMongoClientClass.getMethod("invokeFind", mongoCollectionClass, bsonClass, Class.class);
        assertNotNull(m);

        MongoLogger.getInstance().reset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MongoLogger.getInstance().setOutputStream(new PrintStream(baos));
        m.invoke(simpleMongoClientInstance, mockedMongoCollectionInstance, null, null);
        String output = new String(baos.toByteArray(),StandardCharsets.UTF_8);
        assertNotNull(output);
    }


}