package org.evomaster.client.java.instrumentation.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MongoLogger {

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

    public void logFind(Object mongoCollection, Object bson, boolean hasOperationFoundAnyDocument) {
        try {
            Class<?> mongoCollectionClass = Class.forName("com.mongodb.client.MongoCollection");
            Class<?> codecRegistryClass = Class.forName("org.bson.codecs.configuration.CodecRegistry");


            Method getNamespaceMethod = mongoCollectionClass.getMethod("getNamespace");
            boolean getNamespaceMethodIsAccessible = getNamespaceMethod.isAccessible();
            getNamespaceMethod.setAccessible(true);
            Object nameSpaceInstance = getNamespaceMethod.invoke(mongoCollection);
            getNamespaceMethod.setAccessible(getNamespaceMethodIsAccessible);

            Method getDatabaseNameMethod = nameSpaceInstance.getClass().getMethod("getDatabaseName");
            boolean getDatabaseNameMethodIsAccessible = getDatabaseNameMethod.isAccessible();
            getDatabaseNameMethod.setAccessible(true);
            Object databaseNameInstance = getDatabaseNameMethod.invoke(nameSpaceInstance);
            getDatabaseNameMethod.setAccessible(getDatabaseNameMethodIsAccessible);

            Method getCollectionNameMethod = nameSpaceInstance.getClass().getMethod("getCollectionName");
            boolean getCollectionNameMethodIsAccessible = getCollectionNameMethod.isAccessible();
            getCollectionNameMethod.setAccessible(true);
            Object collectionNameInstance = getCollectionNameMethod.invoke(nameSpaceInstance);
            getCollectionNameMethod.setAccessible(getCollectionNameMethodIsAccessible);

            Method getCodecRegistryMethod = mongoCollectionClass.getMethod("getCodecRegistry");
            boolean getCodecRegistryMethodIsAccessible = getCodecRegistryMethod.isAccessible();
            getCodecRegistryMethod.setAccessible(true);
            Object codecRegistryInstance = getCodecRegistryMethod.invoke(mongoCollection);
            getCodecRegistryMethod.setAccessible(getCodecRegistryMethodIsAccessible);

            Class bsonDocumentClass = Class.forName("org.bson.BsonDocument");
            Object bsonInstance = bson;
            Method toBsonDocumentMethod = bsonInstance.getClass().getMethod("toBsonDocument", bsonDocumentClass.getClass(), codecRegistryClass);
            boolean toBsonDocumentMethodIsAccessible = toBsonDocumentMethod.isAccessible();
            toBsonDocumentMethod.setAccessible(true);
            Object queryInstance = toBsonDocumentMethod.invoke(bsonInstance, bsonDocumentClass, codecRegistryInstance);
            toBsonDocumentMethod.setAccessible(toBsonDocumentMethodIsAccessible);


            Class<?> documentCodecClass = Class.forName("org.bson.codecs.DocumentCodec");
            Constructor<?> documentCodecConstructor = documentCodecClass.getConstructor();
            boolean documentCodecConstructorIsAccessible = documentCodecConstructor.isAccessible();
            documentCodecConstructor.setAccessible(true);
            Object documentCodecInstance = documentCodecConstructor.newInstance();
            documentCodecConstructor.setAccessible(documentCodecConstructorIsAccessible);

            Class<?> decoderContextClass = Class.forName("org.bson.codecs.DecoderContext");
            Method builderMethod = decoderContextClass.getMethod("builder");
            boolean builderMethodIsAccessible = builderMethod.isAccessible();
            builderMethod.setAccessible(true);
            Object builderInstance = builderMethod.invoke(null);
            builderMethod.setAccessible(builderMethodIsAccessible);

            Method buildMethod = builderInstance.getClass().getMethod("build");
            boolean buildMethodIsAccessible = buildMethod.isAccessible();
            buildMethod.setAccessible(true);
            Object decoderContextInstance = buildMethod.invoke(builderInstance);
            buildMethod.setAccessible(buildMethodIsAccessible);

            Method asBsonReaderMethod = queryInstance.getClass().getMethod("asBsonReader");
            boolean asBsonReaderMethodIsAccessible = asBsonReaderMethod.isAccessible();
            asBsonReaderMethod.setAccessible(true);
            Object bsonReaderInstance = asBsonReaderMethod.invoke(queryInstance);
            asBsonReaderMethod.setAccessible(asBsonReaderMethodIsAccessible);

            Method decodeMethod = documentCodecInstance.getClass().getMethod("decode", Class.forName("org.bson.BsonReader"), Class.forName("org.bson.codecs.DecoderContext"));
            boolean decodeMethodIsAccessible = decodeMethod.isAccessible();
            decodeMethod.setAccessible(true);
            Object documentInstance = decodeMethod.invoke(documentCodecInstance, bsonReaderInstance, decoderContextInstance);
            decodeMethod.setAccessible(decodeMethodIsAccessible);

            LoggedExecutedFindOperation log = new LoggedExecutedFindOperation((String) databaseNameInstance,
                    (String) collectionNameInstance,
                    (Document)documentInstance,
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
}
