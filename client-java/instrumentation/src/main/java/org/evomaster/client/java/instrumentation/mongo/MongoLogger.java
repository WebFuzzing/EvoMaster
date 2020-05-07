package org.evomaster.client.java.instrumentation.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class MongoLogger {

    public static final String GET_NAMESPACE = "getNamespace";
    public static final String GET_DATABASE_NAME = "getDatabaseName";
    public static final String GET_COLLECTION_NAME = "getCollectionName";
    public static final String GET_CODEC_REGISTRY = "getCodecRegistry";
    public static final String TO_BSON_DOCUMENT = "toBsonDocument";
    public static final String BUILDER = "builder";
    public static final String BUILD = "build";
    public static final String AS_BSON_READER = "asBsonReader";
    public static final String DECODE = "decode";
    private static MongoLogger instance = null;

    private PrintStream outputStream = null;

    public synchronized static MongoLogger getInstance() {
        if (instance == null) {
            instance = new MongoLogger();
        }
        return instance;
    }

    public void setOutputStream(PrintStream os) {
        this.outputStream = os;
    }

    private MongoLogger() {

    }

    public void reset() {
        this.outputStream = null;
    }

    public static final String PREFIX = "MONGO_LOGGER";

    private static Object invokeMethod(Class<?> receiverClass, Object receiver, String methodName)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return invokeMethod(receiverClass, receiver, methodName, new Class[]{}, new Object[]{});
    }

    private static Object invokeMethod(Class<?> receiverClass, Object receiver, String methodName, Class[] argumentTypes, Object[] arguments)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Objects.requireNonNull(receiverClass);
        Objects.requireNonNull(methodName);
        Objects.requireNonNull(argumentTypes);
        Objects.requireNonNull(arguments);
        if (argumentTypes.length != arguments.length) {
            throw new IllegalArgumentException("Mismatch number of argument types and concrete arguments. Argument types "
                    + argumentTypes.length + " while concrete arguments were "
                    + arguments.length);
        }

        Method getNamespaceMethod = receiverClass.getMethod(methodName, argumentTypes);
        boolean getNamespaceMethodIsAccessible = getNamespaceMethod.isAccessible();
        getNamespaceMethod.setAccessible(true);
        Object retVal = getNamespaceMethod.invoke(receiver, arguments);
        getNamespaceMethod.setAccessible(getNamespaceMethodIsAccessible);
        return retVal;
    }

    private static Object invokeConstructor(Class targetClass)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        return invokeConstructor(targetClass, new Class[]{}, new Object[]{});
    }

    private static Object invokeConstructor(Class targetClass, Class[] argumentTypes, Object[] arguments)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {

        Objects.requireNonNull(targetClass);
        Objects.requireNonNull(argumentTypes);
        Objects.requireNonNull(arguments);
        if (argumentTypes.length != arguments.length) {
            throw new IllegalArgumentException("Mismatch number of argument types and concrete arguments. Argument types "
                    + argumentTypes.length + " while concrete arguments were "
                    + arguments.length);
        }

        Constructor<?> documentCodecConstructor = targetClass.getConstructor(argumentTypes);
        boolean documentCodecConstructorIsAccessible = documentCodecConstructor.isAccessible();
        documentCodecConstructor.setAccessible(true);
        Object newInstance = documentCodecConstructor.newInstance(arguments);
        documentCodecConstructor.setAccessible(documentCodecConstructorIsAccessible);
        return newInstance;
    }


    private static String getDatabaseName(Object mongoCollection) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Class<?> mongoNamespaceClass = Class.forName("com.mongodb.MongoNamespace");
        final Class<?> mongoCollectionClass = Class.forName("com.mongodb.client.MongoCollection");
        Object nameSpaceInstance = invokeMethod(mongoCollectionClass, mongoCollection, GET_NAMESPACE);
        Object databaseNameInstance = invokeMethod(mongoNamespaceClass, nameSpaceInstance, GET_DATABASE_NAME);
        return (String) databaseNameInstance;
    }

    private static String getCollectionName(Object mongoCollection) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Class<?> mongoNamespaceClass = Class.forName("com.mongodb.MongoNamespace");
        final Class<?> mongoCollectionClass = Class.forName("com.mongodb.client.MongoCollection");
        Object nameSpaceInstance = invokeMethod(mongoCollectionClass, mongoCollection, GET_NAMESPACE);
        Object collectionNameInstance = invokeMethod(mongoNamespaceClass, nameSpaceInstance, GET_COLLECTION_NAME);
        return (String) collectionNameInstance;
    }

    public void logFind(Object mongoCollection, Object bson, boolean hasOperationFoundAnyDocument) {
        try {
            String databaseNameInstance = getDatabaseName(mongoCollection);
            String collectionNameInstance = getCollectionName(mongoCollection);
            Document documentInstance = translateToDocument(mongoCollection, bson);

            LoggedExecutedFindOperation log = new LoggedExecutedFindOperation(databaseNameInstance,
                    collectionNameInstance,
                    documentInstance,
                    hasOperationFoundAnyDocument);

            ObjectMapper mapper = new ObjectMapper();
            String jsonString = null;
            try {
                jsonString = mapper.writeValueAsString(log);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Cannot transform instance of " + LoggedExecutedFindOperation.class.getName() + " to JSON.", e);
            }

            String mongoOperation = String.format("%s:%s", PREFIX, jsonString);

            if (outputStream == null) {
                // System.out could be changed during execution
                System.out.println(mongoOperation);
            } else {
                outputStream.println(mongoOperation);
            }


        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException | InstantiationException e) {
            throw new RuntimeException(e);
        }


    }

    private static Document translateToDocument(Object mongoCollection, Object bson) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        final Class<?> mongoCollectionClass = Class.forName("com.mongodb.client.MongoCollection");
        final Class<?> codecRegistryClass = Class.forName("org.bson.codecs.configuration.CodecRegistry");
        final Class<?> bsonDocumentClass = Class.forName("org.bson.BsonDocument");
        final Class<?> documentCodecClass = Class.forName("org.bson.codecs.DocumentCodec");
        final Class<?> decoderContextClass = Class.forName("org.bson.codecs.DecoderContext");
        final Class<?> bsonClass = Class.forName("org.bson.conversions.Bson");
        final Class<?> bsonReaderClass = Class.forName("org.bson.BsonReader");


        Object codecRegistryInstance = invokeMethod(mongoCollectionClass, mongoCollection, GET_CODEC_REGISTRY);

        Object queryInstance = invokeMethod(bsonClass, bson, TO_BSON_DOCUMENT,
                new Class[]{Class.class, codecRegistryClass},
                new Object[]{bsonDocumentClass, codecRegistryInstance});


        Object documentCodecInstance = invokeConstructor(documentCodecClass);
        Object builderInstance = invokeMethod(decoderContextClass, null, BUILDER);

        Object decoderContextInstance = invokeMethod(builderInstance.getClass(), builderInstance, BUILD);
        Object bsonReaderInstance = invokeMethod(queryInstance.getClass(), queryInstance, AS_BSON_READER);

        Object documentInstance = invokeMethod(documentCodecInstance.getClass(), documentCodecInstance, DECODE,
                new Class<?>[]{bsonReaderClass, decoderContextClass},
                new Object[]{bsonReaderInstance, decoderContextInstance});

        return (Document) documentInstance;
    }

}
