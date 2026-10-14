package org.evomaster.e2etests.spring.rest.mongolegacy.persons;

import com.mongodb.client.model.Filters;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.evomaster.client.java.instrumentation.mongo.BsonDocumentConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that the conversion of Bson queries works with the legacy MongoDB driver 3.8
 */
public class LegacyMongoBsonDocumentConverterTest {

    @Test
    public void testLegacyDriverIsOnClasspath() {
        // the no-arg Bson.toBsonDocument() was introduced in driver 4.2
        assertThrows(NoSuchMethodException.class, () -> Bson.class.getMethod("toBsonDocument"));
    }

    @Test
    public void testToBsonDocumentFromFilters() {
        Bson filter = Filters.and(Filters.eq("name", "foo"), Filters.gte("age", 42));
        Object converted = BsonDocumentConverter.toBsonDocument(filter);

        assertNotNull(converted);
        assertTrue(converted instanceof BsonDocument);
        assertEquals(
                BsonDocument.parse("{\"name\": \"foo\", \"age\": {\"$gte\": 42}}"),
                converted);
    }
}
