package org.evomaster.client.java.instrumentation;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import org.bson.*;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.junit.jupiter.api.Assertions.*;

class MongoFindCommandTest {

    public static final org.bson.codecs.configuration.CodecRegistry DEFAULT_CODEC_REGISTRY = MongoClientSettings.getDefaultCodecRegistry();

    /**
     * Serializes and deserializes a MongoFindCommand to test that the query is correctly converted
     * to JSON and back.
     *
     * @param command the MongoFindCommand to serialize and deserialize
     * @return the deserialized MongoFindCommand
     * @throws Exception if an I/O error occurs during serialization or deserialization
     */
    private static MongoFindCommand serializeAndDeserializeCommand(MongoFindCommand command) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(command);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (MongoFindCommand) in.readObject();
        }
    }

    private static MongoFindCommand createMongoFindCommand(Object query, boolean success) {
        return new MongoFindCommand("db", "coll", "schema", query, success, 42L);
    }

    private static Object roundTripQuery(Object query) throws Exception {
        return serializeAndDeserializeCommand(createMongoFindCommand(query, true)).getQuery();
    }

    @Test
    void testMetadataIsPreserved() throws Exception {
        MongoFindCommand copy = serializeAndDeserializeCommand(createMongoFindCommand(new BsonDocument("a", new BsonInt32(1)), true));
        assertEquals("db", copy.getDatabaseName());
        assertEquals("coll", copy.getCollectionName());
        assertEquals("schema", copy.getDocumentsType());
        assertTrue(copy.isSuccessfullyExecuted());
    }

    @Test
    void testUnsuccessfulFlagIsPreserved() throws Exception {
        assertFalse(serializeAndDeserializeCommand(createMongoFindCommand(new BsonDocument("a", new BsonInt32(1)), false)).isSuccessfullyExecuted());
    }

    @Test
    void testNullQuery() throws Exception {
        assertNull(roundTripQuery(null));
    }

    @Test
    void testBsonDocumentWithRegularExpression() throws Exception {
        BsonDocument doc = new BsonDocument("name",
                new BsonDocument("$regex", new BsonRegularExpression("^abc", "i")));
        // sanity check: the raw query is not serializable by default
        assertThrows(NotSerializableException.class, () -> new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(new BsonRegularExpression("x")));

        Object deserializedQuery = roundTripQuery(doc);
        assertEquals(BsonDocument.class, deserializedQuery.getClass());
        assertEquals(doc, deserializedQuery);
    }

    @Test
    void testBsonRegularExpressionDirectValue() throws Exception {
        BsonDocument doc = new BsonDocument("name", new BsonRegularExpression("foo.*", "im"));
        BsonDocument query = (BsonDocument) roundTripQuery(doc);
        BsonRegularExpression regex = query.get("name").asRegularExpression();
        assertEquals("foo.*", regex.getPattern());
        assertEquals("im", regex.getOptions());
    }

    @Test
    void testFiltersConvertedToBsonDocument() throws Exception {
        final Bson bson = Filters.and(Filters.eq("a", 1), Filters.regex("b", "^x"));
        BsonDocument expected = bson.toBsonDocument(BsonDocument.class, DEFAULT_CODEC_REGISTRY);
        Object query = roundTripQuery(expected);
        assertEquals(BsonDocument.class, query.getClass());
        assertEquals(expected, query);
        assertEquals(2, ((BsonDocument) query).getArray("$and").size());
    }

    @Test
    void testBsonDocumentPreservesNumericTypes() throws Exception {
        BsonDocument doc = new BsonDocument("i", new BsonInt32(3)).append("l", new BsonInt64(3L));
        BsonDocument copy = (BsonDocument) roundTripQuery(doc);
        assertEquals(doc, copy);
        assertTrue(copy.get("i").isInt32());
        assertTrue(copy.get("l").isInt64());
    }

    @Test
    void testEmptyDocument() throws Exception {
        assertEquals(new BsonDocument(), roundTripQuery(new BsonDocument()));
    }

    @Test
    void testRegexInsideInArray() throws Exception {
        BsonDocument doc = new BsonDocument("tag", new BsonDocument("$in", new BsonArray(Arrays.asList(
                new BsonRegularExpression("^a"), new BsonRegularExpression("b$", "i")))));
        assertEquals(doc, roundTripQuery(doc));
    }

    @Test
    void testRegexInsideOr() throws Exception {
        Bson filter = Filters.or(Filters.regex("a", "^x"), Filters.eq("b", "y"), Filters.regex("c", "z", "s"));
        BsonDocument expected = filter.toBsonDocument(BsonDocument.class, DEFAULT_CODEC_REGISTRY);
        assertEquals(expected, roundTripQuery(expected));
    }

    @Test
    void testDeeplyNestedDocuments() throws Exception {
        BsonDocument doc = new BsonDocument("a", new BsonDocument("b", new BsonDocument("c",
                new BsonDocument("d", new BsonRegularExpression("deep")))));
        assertEquals(doc, roundTripQuery(doc));
    }

    @Test
    void testObjectIdAndDate() throws Exception {
        BsonDocument doc = new BsonDocument("_id", new BsonObjectId(new ObjectId("507f1f77bcf86cd799439011")))
                .append("created", new BsonDateTime(1700000000000L));
        BsonDocument copy = (BsonDocument) roundTripQuery(doc);
        assertEquals(new ObjectId("507f1f77bcf86cd799439011"), copy.getObjectId("_id").getValue());
        assertEquals(1700000000000L, copy.getDateTime("created").getValue());
    }

    @Test
    void testPrimitiveTypes() throws Exception {
        BsonDocument doc = new BsonDocument("s", new BsonString("text"))
                .append("b", BsonBoolean.TRUE)
                .append("d", new BsonDouble(1.5))
                .append("n", BsonNull.VALUE)
                .append("dec", new BsonDecimal128(new Decimal128(new BigDecimal("12.34"))));
        assertEquals(doc, roundTripQuery(doc));
    }

    @Test
    void testUnicodeStrings() throws Exception {
        BsonDocument doc = new BsonDocument("name", new BsonString("ñandú 日本語 😀 \"quoted\""));
        assertEquals(doc, roundTripQuery(doc));
    }

    @Test
    void testRegexWithSpecialCharacters() throws Exception {
        BsonRegularExpression regex = new BsonRegularExpression("^\\d+\\/[a-z]*\\\\\"$", "ix");
        BsonDocument copy = (BsonDocument) roundTripQuery(new BsonDocument("f", regex));
        assertEquals(regex, copy.getRegularExpression("f"));
    }

    @Test
    void testSerializingTwiceIsStable() throws Exception {
        BsonDocument doc = new BsonDocument("name", new BsonRegularExpression("^abc", "i"));
        MongoFindCommand once = serializeAndDeserializeCommand(createMongoFindCommand(doc, true));
        MongoFindCommand twice = serializeAndDeserializeCommand(once);
        assertEquals(once.getQuery(), twice.getQuery());
        assertEquals(doc, twice.getQuery());
    }

    @Test
    void testOriginalCommandIsNotModified() throws Exception {
        BsonDocument doc = new BsonDocument("name", new BsonRegularExpression("^abc"));
        MongoFindCommand original = createMongoFindCommand(doc, true);
        serializeAndDeserializeCommand(original);
        assertSame(doc, original.getQuery());
    }

    @Test
    void testNonBsonDocumentQueriesAreRejected() {
        // the conversion to BsonDocument is responsibility of the caller
        assertThrows(IllegalArgumentException.class, () -> createMongoFindCommand(new Object(), true));
        assertThrows(IllegalArgumentException.class, () -> createMongoFindCommand(new Document("a", 1), true));
        assertThrows(IllegalArgumentException.class, () -> createMongoFindCommand(Filters.eq("a", 1), true));
    }

    @Test
    void testCommandsInsideSerializableCollection() throws Exception {
        CopyOnWriteArraySet<MongoFindCommand> set = new CopyOnWriteArraySet<>();
        set.add(createMongoFindCommand(new BsonDocument("a", new BsonRegularExpression("x")), true));
        set.add(createMongoFindCommand(new BsonDocument("b", new BsonInt32(2)), false));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(set);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            Set<?> copy = (Set<?>) in.readObject();
            assertEquals(2, copy.size());
        }
    }

    @Test
    void testBsonDocumentIsRestoredAsBsonDocument() throws Exception {
        BsonDocument doc = new BsonDocument("a", new BsonInt32(1));
        Object query = roundTripQuery(doc);
        assertEquals(BsonDocument.class, query.getClass());
        assertEquals(doc, query);
    }
}
