package org.evomaster.client.java.controller.mongo.utils;

import org.bson.BsonType;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.bson.types.Binary;
import org.bson.types.Code;
import org.bson.types.CodeWithScope;
import org.bson.types.Decimal128;
import org.bson.types.MaxKey;
import org.bson.types.MinKey;
import org.bson.types.ObjectId;
import org.bson.types.Symbol;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BsonHelperTest {

    @Test
    void testNewDocument() {
        Document original = new Document("name", "John");

        Object created = BsonHelper.newDocument(original);

        assertTrue(created instanceof Document);
        assertNotSame(original, created);
        assertTrue(((Document) created).isEmpty());
    }

    @Test
    void testNewDocumentRejectsInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> BsonHelper.newDocument(new Object()));
    }

    @Test
    void testAppendAndGetValue() {
        Document doc = new Document();

        BsonHelper.appendToDocument(doc, "age", 42);

        assertEquals(42, BsonHelper.getValue(doc, "age"));
    }

    @Test
    void testAppendRejectsInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> BsonHelper.appendToDocument(new Object(), "age", 42));
    }

    @Test
    void testDocumentContainsField() {
        Document doc = new Document("name", "Alice");

        assertTrue(BsonHelper.documentContainsField(doc, "name"));
        assertFalse(BsonHelper.documentContainsField(doc, "age"));
    }

    @Test
    void testDocumentKeys() {
        Document doc = new Document("name", "Alice").append("age", 30);

        Set<String> keys = BsonHelper.documentKeys(doc);

        assertNotNull(keys);
        assertEquals(new HashSet<>(Arrays.asList("name", "age")), keys);
    }

    @Test
    void testIsBsonDocument() {
        assertTrue(BsonHelper.isBsonDocument(new Document()));
        assertFalse(BsonHelper.isBsonDocument(null));
        assertFalse(BsonHelper.isBsonDocument(new Object()));
    }



    @Test
    void testGetTypeMappedTypes() {
        assertEquals(BsonHelper.NULL_TYPE, BsonHelper.getType(BsonType.NULL));
        assertEquals(List.class.getTypeName(), BsonHelper.getType(BsonType.ARRAY));
        assertEquals(Binary.class.getTypeName(), BsonHelper.getType(BsonType.BINARY));
        assertEquals(Boolean.class.getTypeName(), BsonHelper.getType(BsonType.BOOLEAN));
        assertEquals(Date.class.getTypeName(), BsonHelper.getType(BsonType.DATE_TIME));
        assertEquals(org.bson.BsonDbPointer.class.getTypeName(), BsonHelper.getType(BsonType.DB_POINTER));
        assertEquals(Document.class.getTypeName(), BsonHelper.getType(BsonType.DOCUMENT));
        assertEquals(Double.class.getTypeName(), BsonHelper.getType(BsonType.DOUBLE));
        assertEquals(Integer.class.getTypeName(), BsonHelper.getType(BsonType.INT32));
        assertEquals(Long.class.getTypeName(), BsonHelper.getType(BsonType.INT64));
        assertEquals(Decimal128.class.getTypeName(), BsonHelper.getType(BsonType.DECIMAL128));
        assertEquals(MaxKey.class.getTypeName(), BsonHelper.getType(BsonType.MAX_KEY));
        assertEquals(MinKey.class.getTypeName(), BsonHelper.getType(BsonType.MIN_KEY));
        assertEquals(Code.class.getTypeName(), BsonHelper.getType(BsonType.JAVASCRIPT));
        assertEquals(CodeWithScope.class.getTypeName(), BsonHelper.getType(BsonType.JAVASCRIPT_WITH_SCOPE));
        assertEquals(ObjectId.class.getTypeName(), BsonHelper.getType(BsonType.OBJECT_ID));
        assertEquals(org.bson.BsonRegularExpression.class.getTypeName(), BsonHelper.getType(BsonType.REGULAR_EXPRESSION));
        assertEquals(String.class.getTypeName(), BsonHelper.getType(BsonType.STRING));
        assertEquals(Symbol.class.getTypeName(), BsonHelper.getType(BsonType.SYMBOL));
        assertEquals(org.bson.BsonTimestamp.class.getTypeName(), BsonHelper.getType(BsonType.TIMESTAMP));
        assertEquals(org.bson.BsonUndefined.class.getTypeName(), BsonHelper.getType(BsonType.UNDEFINED));
    }


    @Test
    void testGetTypeFromNumber() {
        Object bsonType = BsonHelper.getTypeFromNumber(16);

        assertEquals(BsonType.INT32, bsonType);
    }

    @Test
    void testGetTypeFromAlias() {
        Object bsonType = BsonHelper.getTypeFromAlias("STRING");

        assertEquals(BsonType.STRING, bsonType);
    }

    @Test
    void testNullArguments() {
        Document doc = new Document();

        assertThrows(NullPointerException.class, () -> BsonHelper.newDocument(null));
        assertThrows(NullPointerException.class, () -> BsonHelper.appendToDocument(null, "a", 1));
        assertThrows(NullPointerException.class, () -> BsonHelper.appendToDocument(doc, null, 1));
        assertThrows(NullPointerException.class, () -> BsonHelper.getValue(null, "a"));
        assertThrows(NullPointerException.class, () -> BsonHelper.getValue(doc, null));
        assertThrows(NullPointerException.class, () -> BsonHelper.documentContainsField(null, "a"));
        assertThrows(NullPointerException.class, () -> BsonHelper.documentContainsField(doc, null));
        assertThrows(NullPointerException.class, () -> BsonHelper.documentKeys(null));
    }
}
