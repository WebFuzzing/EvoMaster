package org.evomaster.client.java.instrumentation.mongo;

import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.Method;
import java.util.Objects;

public class BsonDocumentConverter {

    private static final String ORG_BSON_DOCUMENT = "org.bson.Document";
    private static final String ORG_BSON_BSON_DOCUMENT = "org.bson.BsonDocument";
    private static final String ORG_BSON_CONVERSIONS_BSON = "org.bson.conversions.Bson";
    private static final String TO_BSON_DOCUMENT = "toBsonDocument";
    public static final String ORG_BSON_CODECS_DOCUMENT_CODEC = "org.bson.codecs.DocumentCodec";
    public static final String ORG_BSON_BSON_DOCUMENT_READER = "org.bson.BsonDocumentReader";
    public static final String ORG_BSON_CODECS_DECODER_CONTEXT = "org.bson.codecs.DecoderContext";
    public static final String ORG_BSON_BSON_READER = "org.bson.BsonReader";
    public static final String DECODE = "decode";
    public static final String BUILDER = "builder";
    public static final String BUILD = "build";
    private static final String ORG_BSON_CODECS_CONFIGURATION_CODEC_REGISTRY = "org.bson.codecs.configuration.CodecRegistry";
    private static final String COM_MONGODB_MONGO_CLIENT_SETTINGS = "com.mongodb.MongoClientSettings";
    private static final String COM_MONGODB_MONGO_CLIENT = "com.mongodb.MongoClient";
    private static final String GET_DEFAULT_CODEC_REGISTRY = "getDefaultCodecRegistry";

    /**
     * Checks if the given object is an instance of a org.bson.BsonDocument.
     *
     * @param value the object to be checked; can be null
     * @return true if the object is an instance of a BsonDocument, false otherwise or if an exception occurs
     */
    public static boolean isBsonDocument(Object value) {
        try {
            Class<?> bsonDocClass = loadClass(ORG_BSON_BSON_DOCUMENT, value);
            if (bsonDocClass.isInstance(value)) {
                return true;
            }
            return false;
        } catch (Exception | LinkageError e) {
            SimpleLogger.uniqueWarn("Failed to check if value is a BsonDocument: " + e);
            return false;
        }
    }

    /**
     * Converts an object implementing the org.bson.conversions.Bson interface to a BsonDocument.
     * If the input is already an instance of BsonDocument, it is returned as-is.
     * If the input cannot be converted, null is returned, and a warning is logged.
     *
     * @param bson the object to be converted; it must implement the org.bson.conversions.Bson interface.
     *             Passing a null value is supported and will result in a null return value.
     * @return the converted BsonDocument object, or null if the input is null, if it is not
     *         compatible with the required interface, or if an error occurs during conversion.
     */
    public static /*org.bson.BsonDocument*/ Object toBsonDocument(/*org.bson.conversions.Bson*/ Object bson) {
        if (bson == null) {
            return null;
        }
        try {
            Class<?> bsonClass = loadClass(ORG_BSON_CONVERSIONS_BSON, bson);
            if (!bsonClass.isInstance(bson)) {
                throw new IllegalArgumentException("The provided object is not an instance of org.bson.conversions.Bson but of class " + bson.getClass().getName());

            }
            Class<?> bsonDocumentClass = loadClass(ORG_BSON_BSON_DOCUMENT, bson);
            if (bsonDocumentClass.isInstance(bson)) {
                return bson;
            }
            return invokeToBsonDocument(bsonClass, bsonDocumentClass, bson);
        } catch (Exception | LinkageError e) {
            SimpleLogger.uniqueWarn("Failed to convert Mongo query from Bson: " + e);
            return null;
        }
    }

    /**
     * Calls the no-arg Bson.toBsonDocument(), which only exists since driver 4.2.
     * For older drivers, falls back to toBsonDocument(Class, CodecRegistry) with the default registry.
     */
    private static Object invokeToBsonDocument(Class<?> bsonClass, Class<?> bsonDocumentClass, Object bson) throws Exception {
        try {
            return bsonClass.getMethod(TO_BSON_DOCUMENT).invoke(bson);
        } catch (NoSuchMethodException e) {
            // driver < 4.2
            Class<?> codecRegistryClass = loadClass(ORG_BSON_CODECS_CONFIGURATION_CODEC_REGISTRY, bson);
            Object registry = getDefaultCodecRegistry(bson);
            return bsonClass.getMethod(TO_BSON_DOCUMENT, Class.class, codecRegistryClass)
                    .invoke(bson, bsonDocumentClass, registry);
        }
    }

    private static Object getDefaultCodecRegistry(Object instance) throws Exception {
        try {
            // driver >= 3.7
            return loadClass(COM_MONGODB_MONGO_CLIENT_SETTINGS, instance)
                    .getMethod(GET_DEFAULT_CODEC_REGISTRY).invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // legacy driver (eg, 3.2)
            return loadClass(COM_MONGODB_MONGO_CLIENT, instance)
                    .getMethod(GET_DEFAULT_CODEC_REGISTRY).invoke(null);
        }
    }

    /**
     * Converts an object of type org.bson.BsonDocument to a corresponding org.bson.Document object.
     * If the input object is null, the method returns null. It also handles cases where the input
     * object is not of the expected type and logs a warning in such scenarios.
     *
     * @param bsonDocument the object to be converted; it is expected to be an instance of
     *                     org.bson.BsonDocument. Passing null will result in a null return value.
     * @return the converted org.bson.Document object, or null if the input is null, invalid,
     *         or if an error occurs during conversion.
     */
    public static /*org.bson.Document*/ Object toDocument(/*org.bson.BsonDocument*/ Object bsonDocument) {
        if (bsonDocument == null) {
            return null;
        }
        try {
            Class<?> bsonDocumentClass = loadClass(ORG_BSON_BSON_DOCUMENT, bsonDocument);
            if (!bsonDocumentClass.isInstance(bsonDocument)) {
                throw new IllegalArgumentException("The provided object is not an instance of org.bson.conversions.Bson but of class " + bsonDocument.getClass().getName());
            }
            Class<?> documentClass = loadClass(ORG_BSON_DOCUMENT, bsonDocument);
            if (documentClass.isInstance(bsonDocument)) {
                return bsonDocument;
            }
            Class<?> documentCodecClass = loadClass(ORG_BSON_CODECS_DOCUMENT_CODEC, bsonDocument);
            Object documentCodec = documentCodecClass.getDeclaredConstructor().newInstance();
            final Class<?> bsonDocumentReaderClass = loadClass(ORG_BSON_BSON_DOCUMENT_READER, bsonDocument);
            final Class<?> decoderContextClass = loadClass(ORG_BSON_CODECS_DECODER_CONTEXT, bsonDocument);
            final Class<?> bsonReaderClass = loadClass(ORG_BSON_BSON_READER, bsonDocument);

            final Method decodeMethod = documentCodecClass.getMethod(DECODE, bsonReaderClass, decoderContextClass);
            Object bsonReader = bsonDocumentReaderClass.getConstructor(bsonDocumentClass).newInstance(bsonDocument);
            Object builder = decoderContextClass.getMethod(BUILDER).invoke(null);
            Object decoderContext = builder.getClass().getMethod(BUILD).invoke(builder);
            Object document = decodeMethod.invoke(documentCodec, bsonReader, decoderContext);
            return document;
        } catch (Exception | LinkageError e) {
            SimpleLogger.uniqueWarn("Failed to convert Mongo query from Bson to Document: " + e);
            return null;
        }
    }

    /**
     * Loads a class by its fully qualified name using the class loader of a given instance.
     *
     * @param className the fully qualified name of the class to be loaded; must not be null
     * @param instance an object whose associated class loader will be used to load the class;
     *                 must not be null
     * @return the {@code Class<?>} object representing the loaded class
     * @throws ClassNotFoundException if the class cannot be found
     * @throws NullPointerException if {@code instance} is null
     */
    private static Class<?> loadClass(String className, Object instance) throws ClassNotFoundException {
        Objects.requireNonNull(instance);
        ClassLoader classLoader = instance.getClass().getClassLoader();
        return Class.forName(className, true, classLoader);
    }
}
