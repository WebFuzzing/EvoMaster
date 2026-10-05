package org.evomaster.client.java.instrumentation.mongo;

import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.Method;

public class BsonDocumentConverter {

    private static final String ORG_BSON_DOCUMENT = "org.bson.Document";
    private static final String ORG_BSON_BSON_DOCUMENT = "org.bson.BsonDocument";
    private static final String ORG_BSON_CONVERSIONS_BSON = "org.bson.conversions.Bson";
    private static final String TO_BSON_DOCUMENT = "toBsonDocument";

    /**
     * Checks if the given object is an instance of a org.bson.BsonDocument.
     *
     * @param value the object to be checked; can be null
     * @return true if the object is an instance of a BsonDocument, false otherwise or if an exception occurs
     */
    public static boolean isBsonDocument(Object value) {
        try {
            Class<?> bsonDocClass = Class.forName(ORG_BSON_BSON_DOCUMENT);
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
            Class<?> bsonClass = Class.forName(ORG_BSON_CONVERSIONS_BSON);
            if (!bsonClass.isInstance(bson)) {
                throw new IllegalArgumentException("The provided object is not an instance of org.bson.conversions.Bson but of class " + bson.getClass().getName());

            }
            Class<?> bsonDocumentClass = Class.forName(ORG_BSON_BSON_DOCUMENT);
            if (bsonDocumentClass.isInstance(bson)) {
                return bson;
            }
            Object bsonDocument = bsonClass.getMethod(TO_BSON_DOCUMENT).invoke(bson);
            return bsonDocument;
        } catch (Exception | LinkageError e) {
            SimpleLogger.uniqueWarn("Failed to convert Mongo query from Bson: " + e);
            return null;
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
            Class<?> bsonDocumentClass = Class.forName(ORG_BSON_BSON_DOCUMENT);
            if (!bsonDocumentClass.isInstance(bsonDocument)) {
                throw new IllegalArgumentException("The provided object is not an instance of org.bson.conversions.Bson but of class " + bsonDocument.getClass().getName());
            }
            Class<?> documentClass = Class.forName(ORG_BSON_DOCUMENT);
            if (documentClass.isInstance(bsonDocument)) {
                return bsonDocument;
            }
            Class<?> documentCodecClass = Class.forName("org.bson.codecs.DocumentCodec");
            Object documentCodec = documentCodecClass.getDeclaredConstructor().newInstance();
            final Class<?> bsonDocumentReaderClass = Class.forName("org.bson.BsonDocumentReader");
            final Class<?> decoderContextClass = Class.forName("org.bson.codecs.DecoderContext");
            final Class<?> bsonReaderClass = Class.forName("org.bson.BsonReader");

            final Method decodeMethod = documentCodecClass.getMethod("decode", bsonReaderClass, decoderContextClass);
            Object bsonReader = bsonDocumentReaderClass.getConstructor(bsonDocumentClass).newInstance(bsonDocument);
            Object builder = decoderContextClass.getMethod("builder").invoke(null);
            Object decoderContext = builder.getClass().getMethod("build").invoke(builder);
            Object document = decodeMethod.invoke(documentCodec, bsonReader, decoderContext);
            return document;
        } catch (Exception | LinkageError e) {
            SimpleLogger.uniqueWarn("Failed to convert Mongo query from Bson to Document: " + e);
            return null;
        }
    }
}
