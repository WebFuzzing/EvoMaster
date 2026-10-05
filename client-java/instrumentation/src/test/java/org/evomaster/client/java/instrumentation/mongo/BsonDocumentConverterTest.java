package org.evomaster.client.java.instrumentation.mongo;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonRegularExpression;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BsonDocumentConverterTest {

    // isBsonDocument

    @Test
    void isBsonDocumentTrueForBsonDocument() {
        assertTrue(BsonDocumentConverter.isBsonDocument(new BsonDocument("a", new BsonInt32(1))));
    }

    @Test
    void isBsonDocumentFalseForDocument() {
        assertFalse(BsonDocumentConverter.isBsonDocument(new Document("a", 1)));
    }

    @Test
    void isBsonDocumentFalseForNull() {
        assertFalse(BsonDocumentConverter.isBsonDocument(null));
    }

    @Test
    void isBsonDocumentFalseForUnrelatedObject() {
        assertFalse(BsonDocumentConverter.isBsonDocument("not a document"));
    }

    // toBsonDocument

    @Test
    void toBsonDocumentNullReturnsNull() {
        assertNull(BsonDocumentConverter.toBsonDocument(null));
    }

    @Test
    void toBsonDocumentReturnsSameInstanceForBsonDocument() {
        BsonDocument bsonDocument = new BsonDocument("a", new BsonInt32(1));
        assertSame(bsonDocument, BsonDocumentConverter.toBsonDocument(bsonDocument));
    }

    @Test
    void toBsonDocumentConvertsDocument() {
        Document document = new Document("name", "foo").append("age", 5);

        Object result = BsonDocumentConverter.toBsonDocument(document);

        assertTrue(result instanceof BsonDocument);
        BsonDocument bson = (BsonDocument) result;
        assertEquals(new BsonString("foo"), bson.get("name"));
        assertEquals(new BsonInt32(5), bson.get("age"));
    }

    @Test
    void toBsonDocumentConvertsDocumentWithLongAndRegex() {
        Document document = new Document("n", 10L)
                .append("r", new BsonRegularExpression("^a", "i"));

        BsonDocument bson = (BsonDocument) BsonDocumentConverter.toBsonDocument(document);

        assertNotNull(bson);
        assertEquals(new BsonInt64(10L), bson.get("n"));
        assertEquals(new BsonRegularExpression("^a", "i"), bson.get("r"));
    }

    @Test
    void toBsonDocumentConvertsNestedDocument() {
        Document document = new Document("outer", new Document("inner", 1));

        BsonDocument bson = (BsonDocument) BsonDocumentConverter.toBsonDocument(document);

        assertNotNull(bson);
        assertEquals(new BsonInt32(1), bson.getDocument("outer").get("inner"));
    }

    @Test
    void toBsonDocumentConvertsGenericBson() {
        Bson bson = new Bson() {
            @Override
            public <TDocument> BsonDocument toBsonDocument(Class<TDocument> documentClass, org.bson.codecs.configuration.CodecRegistry codecRegistry) {
                return new BsonDocument("x", new BsonInt32(7));
            }
        };

        Object result = BsonDocumentConverter.toBsonDocument(bson);

        assertEquals(new BsonDocument("x", new BsonInt32(7)), result);
    }

    @Test
    void toBsonDocumentReturnsNullIfNotBson() {
        assertNull(BsonDocumentConverter.toBsonDocument("not bson"));
    }

    // toDocument

    @Test
    void toDocumentNullReturnsNull() {
        assertNull(BsonDocumentConverter.toDocument(null));
    }

    @Test
    void toDocumentReturnsNullIfNotBsonDocument() {
        assertNull(BsonDocumentConverter.toDocument("not a bson document"));
    }

    @Test
    void toDocumentReturnsNullForPlainDocument() {
        // org.bson.Document is not an org.bson.BsonDocument
        assertNull(BsonDocumentConverter.toDocument(new Document("a", 1)));
    }

    @Test
    void toDocumentConvertsBsonDocument() {
        BsonDocument bsonDocument = new BsonDocument("name", new BsonString("foo"))
                .append("age", new BsonInt32(5));

        Object result = BsonDocumentConverter.toDocument(bsonDocument);

        assertNotNull(result);
        assertTrue(result instanceof Document);
        Document document = (Document) result;
        assertEquals("foo", document.get("name"));
        assertEquals(5, document.get("age"));
    }

    @Test
    void toDocumentRoundTrip() {
        Document original = new Document("a", 1).append("b", "text");

        Object bson = BsonDocumentConverter.toBsonDocument(original);
        Object back = BsonDocumentConverter.toDocument(bson);

        assertEquals(original, back);
    }
}
