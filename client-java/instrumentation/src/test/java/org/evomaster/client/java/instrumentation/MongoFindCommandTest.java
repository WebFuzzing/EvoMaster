package org.evomaster.client.java.instrumentation;

import com.mongodb.client.model.Filters;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonRegularExpression;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class MongoFindCommandTest {

    /**
     * Serializes and deserializes a MongoFindCommand to test that the query is correctly converted
     * to a BsonDocument and back.
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

    private static MongoFindCommand command(Object query, boolean success) {
        return new MongoFindCommand("db", "coll", "schema", query, success, 42L);
    }

    @Test
    void testMetadataIsPreserved() throws Exception {
        MongoFindCommand copy = serializeAndDeserializeCommand(command(new Document("a", 1), true));
        assertEquals("db", copy.getDatabaseName());
        assertEquals("coll", copy.getCollectionName());
        assertEquals("schema", copy.getDocumentsType());
        assertTrue(copy.isSuccessfullyExecuted());
    }

    @Test
    void testUnsuccessfulFlagIsPreserved() throws Exception {
        assertFalse(serializeAndDeserializeCommand(command(new Document("a", 1), false)).isSuccessfullyExecuted());
    }

    @Test
    void testNullQuery() throws Exception {
        assertNull(serializeAndDeserializeCommand(command(null, true)).getQuery());
    }

    @Test
    void testDocumentWithRegularExpression() throws Exception {
        Document doc = new Document("name", new Document("$regex", new BsonRegularExpression("^abc", "i")));
        // sanity check: the raw deserializedQuery is not serializable by default
        assertThrows(NotSerializableException.class, () -> new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(new BsonRegularExpression("x")));

        final MongoFindCommand command = command(doc, true);
        final MongoFindCommand deserializedCommand = serializeAndDeserializeCommand(command);
        Object deserializedQuery = deserializedCommand.getQuery();
        assertTrue(deserializedQuery instanceof BsonDocument);
        BsonDocument expected = doc.toBsonDocument(BsonDocument.class, com.mongodb.MongoClientSettings.getDefaultCodecRegistry());
        assertEquals(expected, deserializedQuery);
    }

    @Test
    void testBsonRegularExpressionDirectValue() throws Exception {
        Document doc = new Document("name", new BsonRegularExpression("foo.*", "im"));
        BsonDocument query = (BsonDocument) serializeAndDeserializeCommand(command(doc, true)).getQuery();
        BsonRegularExpression regex = query.get("name").asRegularExpression();
        assertEquals("foo.*", regex.getPattern());
        assertEquals("im", regex.getOptions());
    }

    @Test
    void testFiltersBsonIsConverted() throws Exception {
        Object query = serializeAndDeserializeCommand(command(Filters.and(Filters.eq("a", 1), Filters.regex("b", "^x")), true)).getQuery();
        assertTrue(query instanceof BsonDocument);
        BsonDocument bd = (BsonDocument) query;
        assertTrue(bd.containsKey("$and"));
        assertEquals(2, bd.getArray("$and").size());
    }

    @Test
    void testBsonDocumentPreservesNumericTypes() throws Exception {
        BsonDocument doc = new BsonDocument("i", new BsonInt32(3)).append("l", new org.bson.BsonInt64(3L));
        BsonDocument copy = (BsonDocument) serializeAndDeserializeCommand(command(doc, true)).getQuery();
        assertEquals(doc, copy);
        assertTrue(copy.get("i").isInt32());
        assertTrue(copy.get("l").isInt64());
    }

    private static BsonDocument toBson(Document doc) {
        return doc.toBsonDocument(BsonDocument.class, com.mongodb.MongoClientSettings.getDefaultCodecRegistry());
    }

    @Test
    void testEmptyDocument() throws Exception {
        assertEquals(new BsonDocument(), serializeAndDeserializeCommand(command(new Document(), true)).getQuery());
    }

    @Test
    void testRegexInsideInArray() throws Exception {
        Document doc = new Document("tag", new Document("$in",
                java.util.Arrays.asList(new BsonRegularExpression("^a"), new BsonRegularExpression("b$", "i"))));
        assertEquals(toBson(doc), serializeAndDeserializeCommand(command(doc, true)).getQuery());
    }

    @Test
    void testRegexInsideOr() throws Exception {
        Bson filter = Filters.or(Filters.regex("a", "^x"), Filters.eq("b", "y"), Filters.regex("c", "z", "s"));
        BsonDocument expected = filter.toBsonDocument(BsonDocument.class, com.mongodb.MongoClientSettings.getDefaultCodecRegistry());
        assertEquals(expected, serializeAndDeserializeCommand(command(filter, true)).getQuery());
    }

    @Test
    void testDeeplyNestedDocuments() throws Exception {
        Document doc = new Document("a", new Document("b", new Document("c", new Document("d", new BsonRegularExpression("deep")))));
        assertEquals(toBson(doc), serializeAndDeserializeCommand(command(doc, true)).getQuery());
    }

    @Test
    void testObjectIdAndDate() throws Exception {
        Document doc = new Document("_id", new ObjectId("507f1f77bcf86cd799439011"))
                .append("created", new java.util.Date(1700000000000L));
        BsonDocument copy = (BsonDocument) serializeAndDeserializeCommand(command(doc, true)).getQuery();
        assertEquals(new ObjectId("507f1f77bcf86cd799439011"), copy.getObjectId("_id").getValue());
        assertEquals(1700000000000L, copy.getDateTime("created").getValue());
    }

    @Test
    void testPrimitiveTypes() throws Exception {
        Document doc = new Document("s", "text")
                .append("b", true)
                .append("d", 1.5)
                .append("n", null)
                .append("dec", new org.bson.types.Decimal128(new java.math.BigDecimal("12.34")));
        final Object query = serializeAndDeserializeCommand(command(doc, true)).getQuery();
        assertEquals(toBson(doc), query);
    }

    @Test
    void testUnicodeStrings() throws Exception {
        Document doc = new Document("name", "ñandú 日本語 \uD83D\uDE00 \"quoted\"");
        assertEquals(toBson(doc), serializeAndDeserializeCommand(command(doc, true)).getQuery());
    }

    @Test
    void testRegexWithSpecialCharacters() throws Exception {
        BsonRegularExpression regex = new BsonRegularExpression("^\\d+\\/[a-z]*\\\\\"$", "ix");
        BsonDocument copy = (BsonDocument) serializeAndDeserializeCommand(command(new Document("f", regex), true)).getQuery();
        assertEquals(regex, copy.getRegularExpression("f"));
    }

    @Test
    void testSerializingTwiceIsStable() throws Exception {
        Document doc = new Document("name", new BsonRegularExpression("^abc", "i"));
        MongoFindCommand once = serializeAndDeserializeCommand(command(doc, true));
        MongoFindCommand twice = serializeAndDeserializeCommand(once);
        assertEquals(once.getQuery(), twice.getQuery());
        assertEquals(toBson(doc), twice.getQuery());
    }

    @Test
    void testOriginalCommandIsNotModified() throws Exception {
        Document doc = new Document("name", new BsonRegularExpression("^abc"));
        MongoFindCommand original = command(doc, true);
        serializeAndDeserializeCommand(original);
        assertSame(doc, original.getQuery());
    }

    @Test
    void testNonBsonQueryDegradesToNull() throws Exception {
        // cannot be converted to a BsonDocument, but must not break serialization of the whole command
        MongoFindCommand copy = serializeAndDeserializeCommand(command(new Object(), true));
        assertNull(copy.getQuery());
        assertEquals("coll", copy.getCollectionName());
    }

    @Test
    void testCommandsInsideSerializableCollection() throws Exception {
        java.util.concurrent.CopyOnWriteArraySet<MongoFindCommand> set = new java.util.concurrent.CopyOnWriteArraySet<>();
        set.add(command(new Document("a", new BsonRegularExpression("x")), true));
        set.add(command(new Document("b", 2), false));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(set);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            java.util.Set<?> copy = (java.util.Set<?>) in.readObject();
            assertEquals(2, copy.size());
        }
    }
}
