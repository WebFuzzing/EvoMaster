package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.MongoFindCommand;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.mongo.BsonDocumentConverter;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.object.CustomTypeToOasConverter;
import org.evomaster.client.java.instrumentation.object.GeoJsonPointToOasConverter;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;


public abstract class MongoOperationClassReplacement extends ThirdPartyMethodReplacementClass {

    protected static void handleMongo(Object mongoCollection, Object bson, boolean successfullyExecuted, long executionTime) {
        List<CustomTypeToOasConverter> converters = Collections.singletonList(new GeoJsonPointToOasConverter());
        String schema = ClassToSchema.getOrDeriveSchemaWithItsRef(extractDocumentsType(mongoCollection), true, converters);
        Object bsonDocument = BsonDocumentConverter.toBsonDocument(bson);
        MongoFindCommand info = new MongoFindCommand(getDatabaseName(mongoCollection), getCollectionName(mongoCollection), schema, bsonDocument, successfullyExecuted, executionTime);
        ExecutionTracer.addMongoInfo(info);
    }


    /**
     * Executes an operation that, unlike find, is executed eagerly (eg, count, delete and update),
     * so we know right away whether the filter was valid. The query is stored in any case.
     *
     * @param singleton       the replacement class holding the replacement with the given id
     * @param id              the id of the replacement, used to retrieve the original method
     * @param mongoCollection the collection on which the operation is executed
     * @param args            the arguments of the original call
     * @param filter          the query used in the operation
     * @return the result of the original call
     */
    protected static Object handleEagerQuery(ThirdPartyMethodReplacementClass singleton, String id, Object mongoCollection, List<Object> args, Object filter) {
        long start = System.currentTimeMillis();
        Method method = getOriginal(singleton, id, mongoCollection);
        try {
            Object result = method.invoke(mongoCollection, args.toArray());
            long end = System.currentTimeMillis();
            handleMongo(mongoCollection, filter, true, end - start);
            return result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            long end = System.currentTimeMillis();
            handleMongo(mongoCollection, filter, false, end - start);
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    private static Class<?> extractDocumentsType(Object collection) {
        try {
            Class<?> collectionClass = getCollectionClass(collection);
            return (Class<?>) collectionClass.getMethod("getDocumentClass").invoke(collection);
        } catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException |
                 IllegalAccessException e) {
            throw new RuntimeException("Failed to retrieve document's type from collection", e);
        }
    }

    private static String getDatabaseName(Object collection) {
        try {
            Class<?> collectionClass = getCollectionClass(collection);
            Object namespace = collectionClass.getMethod("getNamespace").invoke(collection);
            return (String) namespace.getClass().getMethod("getDatabaseName").invoke(namespace);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 ClassNotFoundException e) {
            throw new RuntimeException("Failed to retrieve name of the database in which collection is", e);
        }
    }

    private static String getCollectionName(Object collection) {
        try {
            Class<?> collectionClass = getCollectionClass(collection);
            Object namespace = collectionClass.getMethod("getNamespace").invoke(collection);
            return (String) namespace.getClass().getMethod("getCollectionName").invoke(namespace);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 ClassNotFoundException e) {
            throw new RuntimeException("Failed to retrieve collection name", e);
        }
    }

    private static Class<?> getCollectionClass(Object collection) throws ClassNotFoundException {
        // collection is an implementation of interface MongoCollection
        return collection.getClass().getInterfaces()[0];
    }
}
