package org.evomaster.client.java.controller.internal.db.mongo;

import com.mongodb.client.model.Filters;
import org.bson.*;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.conversions.Bson;
import org.evomaster.client.java.controller.mongo.MongoHeuristicsCalculator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;


class MongoHeuristicCalculatorTest {

    @Test
    public void testEquals() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.eq("age", 10);
        Bson bsonFalse = Filters.eq("age", 26);
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(16.0, distanceNotMatch);
    }

    @Test
    public void testNotEquals() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue1 = Filters.ne("age", 26);
        Bson bsonTrue2 = Filters.ne("some-field", 26);
        Bson bsonFalse = Filters.ne("age", 10);
        Double distanceMatch1 = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue1), doc);
        Double distanceMatch2 = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue2), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch1);
        assertEquals(0.0, distanceMatch2);
        assertEquals(1.0, distanceNotMatch);
    }

    @Test
    public void testGreaterThan() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.gt("age", 5);
        Bson bsonFalse = Filters.gt("age", 13);
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(4.0, distanceNotMatch);
    }

    @Test
    public void testGreaterThanEquals() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.gte("age", 5);
        Bson bsonFalse = Filters.gte("age", 13);
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(3.0, distanceNotMatch);
    }

    @Test
    public void testLessThan() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.lt("age", 11);
        Bson bsonFalse = Filters.lt("age", 7);
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(4.0, distanceNotMatch);
    }

    @Test
    public void testLessThanEquals() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.lte("age", 11);
        Bson bsonFalse = Filters.lte("age", 7);
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(3.0, distanceNotMatch);
    }

    @Test
    public void testOr() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.or(Filters.gt("age", 9), Filters.lt("age", 20));
        Bson bsonFalse = Filters.or(Filters.gt("age", 17), Filters.lt("age", 8));
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(3.0, distanceNotMatch);
    }

    @Test
    public void testAnd() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.and(Filters.gt("age", 9), Filters.lt("age", 20));
        Bson bsonFalse = Filters.and(Filters.gt("age", 10), Filters.lt("age", 8));
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(4.0, distanceNotMatch);
    }

    @Test
    public void testImplicitAnd() {
        Document doc = new Document().append("age", 10).append("kg", 50);
        Bson bsonTrue = BsonDocument.parse("{age: 10, kg: {$gt: 40}}");
        Bson bsonFalse = BsonDocument.parse("{age: 9, kg: {$gt: 40}}");
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(1.0, distanceNotMatch);
    }

    @Test
    public void testIn() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.in("age", new ArrayList<>(Arrays.asList(1, 10, 8)));
        Bson bsonFalse = Filters.in("age", new ArrayList<>(Arrays.asList(1, 15)));
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(5.0, distanceNotMatch);
    }

    @Test
    public void testNotIn() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.nin("age", new ArrayList<>(Arrays.asList(1, 8)));
        Bson bsonFalse = Filters.nin("age", new ArrayList<>(Arrays.asList(1, 10)));
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(1.0, distanceNotMatch);
    }

    @Test
    public void testAll() {
        Document doc = new Document().append("employees", new ArrayList<>(Arrays.asList(1, 5, 6)));
        Bson bsonTrue = Filters.all("employees", new ArrayList<>(Arrays.asList(1, 5, 6)));
        Bson bsonFalse = Filters.all("employees", new ArrayList<>(Arrays.asList(1, 7, 8)));
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(3.0, distanceNotMatch);
    }

    @Test
    public void testSize() {
        Document doc = new Document().append("employees", new ArrayList<>(Arrays.asList(1, 5, 6)));
        Bson bsonTrue = Filters.size("employees", 3);
        Bson bsonFalse = Filters.size("employees", 5);
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(2.0, distanceNotMatch);
    }

    @Test
    public void testMod() {
        Document doc = new Document().append("age", 20);
        Bson bsonTrue = Filters.mod("age", 3, 2);
        Bson bsonFalse = Filters.mod("age", 3, 0);
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(2.0, distanceNotMatch);
    }

    @Test
    public void testNot() {
        Document doc = new Document().append("age", 20);
        Bson bsonTrue = Filters.not(Filters.gt("age", 30));
        Bson bsonFalse = Filters.not(Filters.gt("age", 10));
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(10.0, distanceNotMatch);
    }

    @Test
    public void testExistsTrueVersion() {
        Document doc = new Document().append("age", 20);
        Bson bsonTrue = Filters.exists("age", true);
        Bson bsonFalse = Filters.exists("name", true);
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(65563.0, distanceNotMatch);
    }

    @Test
    public void testExistsFalseVersion() {
        Document doc = new Document().append("age", 20);
        Bson bsonTrue = Filters.exists("name", false);
        Bson bsonFalse = Filters.exists("age", false);
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(1.0, distanceNotMatch);
    }

    @Test
    public void testTypeExplicitVersion() {
        Document doc = new Document().append("age", 20);
        Bson bsonTrue = Filters.type("age", BsonType.INT32);
        Bson bsonFalse = Filters.type("age", BsonType.DOUBLE);
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(65551.0, distanceNotMatch);
    }

    @Test
    public void testTypeAliasVersion() {
        // This is not exactly the alias. Should be?
        Document doc = new Document().append("age", 20);
        Bson bsonTrue = Filters.type("age", BsonType.INT32.name());
        Bson bsonFalse = Filters.type("age", BsonType.DOUBLE.name());
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(65551.0, distanceNotMatch);
    }

    @Test
    public void testElemMatch() {
        Document doc = new Document().append("years", new ArrayList<>(Arrays.asList(2002, 2010)));
        Bson bsonTrue = Filters.elemMatch("years", Filters.gt("years", 2009));
        Bson bsonFalse = Filters.elemMatch("years", Filters.lt("years", 2001));
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(2.0, distanceNotMatch);
    }

    @Test
    public void testNearSphere() {
        Document doc = new Document().append("location", new Document().append("type", "Point").append("coordinates", Arrays.asList(-74.044502, 40.689247)));
        BsonDocument point = new BsonDocument().append("type", new BsonString("Point")).append("coordinates", new BsonArray(Arrays.asList(new BsonDouble(2.29441692356368), new BsonDouble(48.858504187164684))));
        Bson bsonTrue = Filters.nearSphere("location", point, 6000000.0, 0.0);
        Bson bsonFalse = Filters.nearSphere("location", point, 5000000.0, 0.0);
        Double distanceMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonTrue), doc);
        Double distanceNotMatch = new MongoHeuristicsCalculator().computeExpression(convertToDocument(bsonFalse), doc);
        assertEquals(0.0, distanceMatch);
        assertEquals(837402.9310023151, distanceNotMatch);
    }

    private static Document convertToDocument(Bson filter) {
        BsonDocument bsonDocument = filter.toBsonDocument();
        DocumentCodec documentCodec = new DocumentCodec();
        return documentCodec.decode(bsonDocument.asBsonReader(), DecoderContext.builder().build());
    }
}