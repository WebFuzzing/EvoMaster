package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.utils.SimpleLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Info related to MONGO command execution.
 */
public class MongoFindCommand implements Serializable {
    private static final String ORG_BSON_BSON_DOCUMENT = "org.bson.BsonDocument";
    private static final String COM_MONGODB_MONGO_CLIENT_SETTINGS = "com.mongodb.MongoClientSettings";
    private static final String GET_DEFAULT_CODEC_REGISTRY = "getDefaultCodecRegistry";
    private static final String ORG_BSON_CONVERSIONS_BSON = "org.bson.conversions.Bson";
    private static final String TO_BSON_DOCUMENT = "toBsonDocument";
    private static final String ORG_BSON_JSON_JSON_WRITER_SETTINGS = "org.bson.json.JsonWriterSettings";
    private static final String ORG_BSON_JSON_JSON_MODE = "org.bson.json.JsonMode";
    private static final String BUILDER = "builder";
    private static final String OUTPUT_MODE = "outputMode";
    private static final String BUILD = "build";
    private static final String TO_JSON = "toJson";
    /**
     * Name of the collection that the operation was applied to
     */
    private final String collectionName;

    /**
     * Name of the database that the operation was applied to
     */
    private final String databaseName;

    /**
     * Type of the documents of the collection
     */
    private final String documentsType;

    /**
     * Executed FIND query as an org.bson.conversions.Bson object.
     * Both org.bson.BsonDocument and org.bson.Document implement this interface,
     * so the actual type of this field will be one of those two.
     * <p>
     * Additionally, this field is made transient and non-final to allow custom serialization/
     * deserialization, because the Bson interface is not Serializable (and its values might not be either).
     * It is written/read manually in writeObject/readObject.
     */
    private transient /*non-final*/ Object bson;

    /**
     * If the operation was successfully executed
     */
    private final boolean successfullyExecuted;

    /**
     * Elapsed execution time
     */
    private final long executionTime;

    public MongoFindCommand(String databaseName, String collectionName, String documentsType, Object bson, boolean successfullyExecuted, long executionTime) {
        this.collectionName = collectionName;
        this.databaseName = databaseName;
        this.documentsType = documentsType;
        this.bson = bson;
        this.successfullyExecuted = successfullyExecuted;
        this.executionTime = executionTime;
    }

    public Object getQuery() {
        return bson;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getDocumentsType() {
        return documentsType;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Indicates whether the find command
     * was successfully executed or it
     * threw a RuntimeException due to
     * an invalid query.
     *
     * @return if the lazy command was successfully executed
     */
    public boolean isSuccessfullyExecuted() {
        return successfullyExecuted;
    }

    /*
     The query is a Bson object from the SUT, which might contain non-serializable
     types (eg, BsonRegularExpression), and the Bson interface itself is not Serializable.
     So, when sent between processes, we use its extended JSON representation,
     and rebuild it as a BsonDocument on the other side.
  */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        final String extendedJson = toExtendedJson(bson);
        out.writeObject(extendedJson);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        final String json = (String) in.readObject();
        bson = fromExtendedJson(json);
    }

    private static String toExtendedJson(Object bson) {
        if (bson == null) {
            return null;
        }
        try {
            ClassLoader cl = bson.getClass().getClassLoader();
            Class<?> bsonDocClass = Class.forName(ORG_BSON_BSON_DOCUMENT, true, cl);
            Object registry = Class.forName(COM_MONGODB_MONGO_CLIENT_SETTINGS, true, cl)
                    .getMethod(GET_DEFAULT_CODEC_REGISTRY).invoke(null);
            Object doc = Class.forName(ORG_BSON_CONVERSIONS_BSON, true, cl)
                    .getMethod(TO_BSON_DOCUMENT, Class.class, Class.forName("org.bson.codecs.configuration.CodecRegistry", true, cl))
                    .invoke(bson, bsonDocClass, registry);

            Class<?> settingsClass = Class.forName(ORG_BSON_JSON_JSON_WRITER_SETTINGS, true, cl);
            Class<?> modeClass = Class.forName(ORG_BSON_JSON_JSON_MODE, true, cl);
            Object builder = settingsClass.getMethod(BUILDER).invoke(null);
            builder = builder.getClass().getMethod(OUTPUT_MODE, modeClass).invoke(builder, modeClass.getField("EXTENDED").get(null));
            Object settings = builder.getClass().getMethod(BUILD).invoke(builder);
            return (String) bsonDocClass.getMethod(TO_JSON, settingsClass).invoke(doc, settings);
        } catch (Exception | LinkageError e) {
            SimpleLogger.uniqueWarn("Failed to convert Mongo query to JSON: " + e);
            return null;
        }
    }

    private static Object fromExtendedJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return Class.forName(ORG_BSON_BSON_DOCUMENT).getMethod("parse", String.class).invoke(null, json);
        } catch (Exception | LinkageError e) {
            SimpleLogger.uniqueWarn("Failed to parse Mongo query from JSON: " + e);
            return null;
        }
    }
}
