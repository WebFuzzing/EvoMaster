package org.evomaster.client.java.controller.mongo;

import com.mongodb.client.model.Filters;
import org.bson.*;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.evomaster.client.java.controller.internal.TaintHandlerExecutionTracer;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.sql.internal.TaintHandler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class MongoHeuristicsCalculatorTest {

    @Test
    public void testEquals() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.eq("age", 10);
        Bson bsonFalse = Filters.eq("age", 26);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testNotEquals() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue1 = Filters.ne("age", 26);
        Bson bsonTrue2 = Filters.ne("some-field", 26);
        Bson bsonFalse = Filters.ne("age", 10);
        Truthness distanceMatch1 = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue1), doc);
        Truthness distanceMatch2 = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue2), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch1.isTrue());
        assertTrue(distanceMatch2.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testGreaterThan() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.gt("age", 5);
        Bson bsonFalse = Filters.gt("age", 13);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testGreaterThanEquals() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.gte("age", 5);
        Bson bsonFalse = Filters.gte("age", 13);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testLessThan() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.lt("age", 11);
        Bson bsonFalse = Filters.lt("age", 7);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testLessThanEquals() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.lte("age", 11);
        Bson bsonFalse = Filters.lte("age", 7);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testOr() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.or(Filters.gt("age", 9), Filters.lt("age", 20));
        Bson bsonFalse = Filters.or(Filters.gt("age", 17), Filters.lt("age", 8));
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testAnd() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.and(Filters.gt("age", 9), Filters.lt("age", 20));
        Bson bsonFalse = Filters.and(Filters.gt("age", 10), Filters.lt("age", 8));
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testNorReturnsTrue() {
        Document doc = new Document().append("age", 25);
        Bson bsonTrue = Filters.nor(Filters.gt("age", 30), Filters.lt("age", 18));
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        assertTrue(distanceMatch.isTrue());
    }

    @Test
    public void testNorReturnsFalse() {
        Document doc = new Document().append("age", 35);
        Bson bsonFalse = Filters.nor(Filters.gt("age", 30), Filters.lt("age", 18));
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testImplicitAnd() {
        Document doc = new Document().append("age", 10).append("kg", 50);
        Bson bsonTrue = BsonDocument.parse("{age: 10, kg: {$gt: 40}}");
        Bson bsonFalse = BsonDocument.parse("{age: 9, kg: {$gt: 40}}");
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testIn() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.in("age", new ArrayList<>(Arrays.asList(1, 10, 8)));
        Bson bsonFalse = Filters.in("age", new ArrayList<>(Arrays.asList(1, 15)));
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testInFieldMissingField() {
        Document doc = new Document(); // field "age" is undefined
        Bson bsonTrue = Filters.in("age", new ArrayList<>(Arrays.asList(null, 10, 8)));
        Bson bsonFalse = Filters.in("age", new ArrayList<>(Arrays.asList(1, 15)));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue(), "Should match because null is in the list and undefined is treated as null");
        assertTrue(distanceNotMatch.isFalse(), "Should not match because null is NOT in the list");
    }

    @Test
    public void testInFieldNull() {
        Document doc = new Document().append("age", null);
        Bson bsonTrue = Filters.in("age", new ArrayList<>(Arrays.asList(null, 10, 8)));
        Bson bsonFalse = Filters.in("age", new ArrayList<>(Arrays.asList(1, 15)));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue(), "Should match because null is in the list");
        assertTrue(distanceNotMatch.isFalse(), "Should not match because null is NOT in the list");
    }

    @Test
    public void testInFieldList() {
        Document doc = new Document().append("tags", new ArrayList<>(Arrays.asList("a", "b", "c")));

        // Match if any of "tags" elements is in ["b", "z"]
        Bson bsonTrue = Filters.in("tags", new ArrayList<>(Arrays.asList("b", "z")));
        Bson bsonFalse = Filters.in("tags", new ArrayList<>(Arrays.asList("x", "y")));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue(), "Should match because 'b' is in both lists");
        assertTrue(distanceNotMatch.isFalse(), "Should not match because no element is in both lists");
    }

    @Test
    public void testNotIn() {
        Document doc = new Document().append("age", 10);
        Bson bsonTrue = Filters.nin("age", new ArrayList<>(Arrays.asList(1, 8)));
        Bson bsonFalse = Filters.nin("age", new ArrayList<>(Arrays.asList(1, 10)));
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testNotInMissingField() {
        Document doc = new Document().append("name", "Bob"); // "age" field is missing
        Bson bsonTrue = Filters.nin("age", new ArrayList<>(Arrays.asList(1, 8)));
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        assertTrue(distanceMatch.isTrue());
    }

    @Test
    public void testAll() {
        Document doc = new Document().append("employees", new ArrayList<>(Arrays.asList(1, 5, 6)));
        Bson bsonTrue = Filters.all("employees", new ArrayList<>(Arrays.asList(1, 5, 6)));
        Bson bsonFalse = Filters.all("employees", new ArrayList<>(Arrays.asList(1, 7, 8)));
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testAllNull() {
        Document docNull = new Document().append("employees", null);
        Bson all = Filters.all("employees", Arrays.asList(1, 2));

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(all), docNull).isFalse());
    }


    @Test
    public void testAllMissingField() {
        Document docUndefined = new Document();
        Bson allQuery = Filters.all("employees", Arrays.asList("Bob", "Alice"));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(allQuery), docUndefined).isFalse());
    }

    @Test
    public void testAllExpectedListIsEmpty() {
        Document document = new Document().append("employees", Arrays.asList("Bob", "Alice"));
        Bson allQuery = Filters.all("employees", Collections.emptyList());
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(allQuery), document).isFalse());
    }

    @Test
    public void testAllActualListIsEmpty() {
        Document document = new Document().append("employees", Collections.emptyList());
        Bson all = Filters.all("employees", Arrays.asList("Bob", "Alice"));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(all), document).isFalse());
    }

    @Test
    public void testAllBothActualAndExpectedListsAreEmpty() {
        Document document = new Document().append("employees", Collections.emptyList());
        Bson allQuery = Filters.all("employees", Collections.emptyList());
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(allQuery), document).isFalse());
    }


    @Test
    public void testSize() {
        Document doc = new Document().append("employees", new ArrayList<>(Arrays.asList(1, 5, 6)));
        Bson bsonTrue = Filters.size("employees", 3);
        Bson bsonFalse = Filters.size("employees", 5);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testSizeMissingField() {
        Document doc = new Document().append("name", "Bob"); // employees field is missing
        Bson query = Filters.size("employees", 3);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(query), doc);
        assertTrue(distanceMatch.isFalse());
    }

    @Test
    public void testSizeNull() {
        Document doc = new Document().append("employees", null);
        Bson query = Filters.size("employees", 3);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(query), doc);
        assertTrue(distanceMatch.isFalse());
    }

    @Test
    public void testSizeNotAList() {
        Document doc = new Document().append("employees", "Bob"); // employees field is not a list
        Bson query = Filters.size("employees", 3);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(query), doc);
        assertTrue(distanceMatch.isFalse());
    }

    @Test
    public void testMod() {
        Document doc = new Document().append("age", 20);
        Bson bsonTrue = Filters.mod("age", 3, 2);
        Bson bsonFalse = Filters.mod("age", 3, 0);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testModNull() {
        Document docNull = new Document().append("age", null);
        Bson mod = Filters.mod("age", 3, 2);

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(mod), docNull).isFalse());
    }

    @Test
    public void testModMissingField() {
        Document docUndefined = new Document();
        Bson mod = Filters.mod("age", 3, 2);

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(mod), docUndefined).isFalse());
    }


    @Test
    public void testNot() {
        Document doc = new Document().append("age", 20);
        Bson bsonTrue = Filters.not(Filters.gt("age", 30));
        Bson bsonFalse = Filters.not(Filters.gt("age", 10));
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testNotMissingField() {
        Document doc = new Document().append("name", "Bob"); // "age" field is undefined
        Bson bsonTrue = Filters.not(Filters.gt("age", 30));
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        assertTrue(distanceMatch.isTrue());
    }

    @Test
    public void testNotNullValue() {
        Document doc = new Document().append("age", null);
        Bson bsonTrue = Filters.not(Filters.eq("age", null));
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        assertTrue(distanceMatch.isFalse());
    }

    @Test
    public void testExistsTrueValue() {
        Document doc = new Document().append("age", 20);
        Bson bsonTrue = Filters.exists("age", true);
        Bson bsonFalse = Filters.exists("name", true);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testExistsFalseValue() {
        Document doc = new Document().append("age", 20);
        Bson bsonTrue = Filters.exists("name", false);
        Bson bsonFalse = Filters.exists("age", false);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testTypeExplicitVersion() {
        Document doc = new Document().append("age", 20);
        Bson bsonTrue = Filters.type("age", BsonType.INT32);
        Bson bsonFalse = Filters.type("age", BsonType.DOUBLE);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testTypeAliasVersion() {
        // This is not exactly the alias. Should be?
        Document doc = new Document().append("age", 20);
        Bson bsonTrue = Filters.type("age", BsonType.INT32.name());
        Bson bsonFalse = Filters.type("age", BsonType.DOUBLE.name());
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testTypeString() {
        Document doc = new Document().append("name", "John");
        Bson bsonTrue = Filters.type("name", BsonType.STRING);
        Bson bsonFalse = Filters.type("name", BsonType.BOOLEAN);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testTypeBoolean() {
        Document doc = new Document().append("active", true);
        Bson bsonTrue = Filters.type("active", BsonType.BOOLEAN);
        Bson bsonFalse = Filters.type("active", BsonType.STRING);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testEqualsBoolean() {
        Document doc = new Document().append("active", true);
        Bson bsonTrue = Filters.eq("active", true);
        Bson bsonFalse = Filters.eq("active", false);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testEqualsString() {
        Document doc = new Document().append("name", "Bob");
        Bson bsonTrue = Filters.eq("name", "Bob");
        Bson bsonFalse = Filters.eq("name", "Alice");
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testEqualsDouble() {
        Document doc = new Document().append("score", 10.5d);
        Bson bsonTrue = Filters.eq("score", 10.5d);
        Bson bsonFalse = Filters.eq("score", 20.5d);

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();
        Truthness distanceMatch = calculator.computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = calculator.computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testEqualsInt64() {
        Document doc = new Document().append("big", 10L);
        Bson bsonTrue = Filters.eq("big", 10L);
        Bson bsonFalse = Filters.eq("big", 11L);

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();
        Truthness distanceMatch = calculator.computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = calculator.computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testEqualsDecimal128() {
        Decimal128 value = new Decimal128(new BigDecimal("12.34"));
        Decimal128 otherValue = new Decimal128(new BigDecimal("56.78"));

        Document doc = new Document().append("amount", value);
        Bson bsonTrue = Filters.eq("amount", value);
        Bson bsonFalse = Filters.eq("amount", otherValue);

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();
        Truthness distanceMatch = calculator.computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = calculator.computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());

    }

    @Test
    public void testEqualsObjectId() {
        ObjectId value = new ObjectId("64b7f3b5e13823708a6a1234");
        ObjectId otherValue = new ObjectId("64b7f3b5e13823708a6a5678");

        Document doc = new Document().append("_id", value);
        Bson bsonTrue = Filters.eq("_id", value);
        Bson bsonFalse = Filters.eq("_id", otherValue);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testEqualsDate() throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        Date startDate = dateFormat.parse("2025-01-14");
        Date anotherStartDate = dateFormat.parse("2025-02-14");


        Document doc = new Document().append("startDate", startDate);
        Bson bsonTrue = Filters.eq("startDate", startDate);
        Bson bsonFalse = Filters.eq("startDate", anotherStartDate);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Disabled
    @Test
    public void testEqualsLists() {
        Document doc = new Document().append("employees", Arrays.asList("Alice", "Bob"));
        Bson bsonTrue = Filters.eq("employees", Arrays.asList("Alice", "Bob"));
        Bson bsonFalse = Filters.eq("employees", Arrays.asList("Alice"));
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testSizeStringList() {
        Document doc = new Document().append("tags", Arrays.asList("qa", "api", "db"));
        Bson bsonTrue = Filters.size("tags", 3);
        Bson bsonFalse = Filters.size("tags", 2);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Disabled
    @Test
    public void testNearSphere() {
        Document doc = new Document().append("location", new Document().append("type", "Point").append("coordinates", Arrays.asList(-74.044502, 40.689247)));
        BsonDocument point = new BsonDocument().append("type", new BsonString("Point")).append("coordinates", new BsonArray(Arrays.asList(new BsonDouble(2.29441692356368), new BsonDouble(48.858504187164684))));
        Bson bsonTrue = Filters.nearSphere("location", point, 6000000.0, 0.0);
        Bson bsonFalse = Filters.nearSphere("location", point, 5000000.0, 0.0);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testComparisonNull() {
        Document docNull = new Document().append("age", null);

        Bson gt = Filters.gt("age", 5);
        Bson gte = Filters.gte("age", 5);
        Bson lt = Filters.lt("age", 15);
        Bson lte = Filters.lte("age", 15);

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        // All should be false because null/undefined is not comparable to 5/15 using these operators
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(gt), docNull).isFalse());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(gte), docNull).isFalse());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(lt), docNull).isFalse());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(lte), docNull).isFalse());
    }

    @Test
    public void testComparisonMissingField() {
        Document docUndefined = new Document();

        Bson gt = Filters.gt("age", 5);
        Bson gte = Filters.gte("age", 5);
        Bson lt = Filters.lt("age", 15);
        Bson lte = Filters.lte("age", 15);

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        // All should be false because null/undefined is not comparable to 5/15 using these operators
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(gt), docUndefined).isFalse());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(gte), docUndefined).isFalse());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(lt), docUndefined).isFalse());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(lte), docUndefined).isFalse());
    }


    @Test
    public void testEqualsNull() {
        Document docNull = new Document().append("age", null);

        Bson eqNull = Filters.eq("age", null);
        Bson eqValue = Filters.eq("age", 10);

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(eqNull), docNull).isTrue());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(eqValue), docNull).isFalse());
    }

    @Test
    public void testEqualsMissingField() {
        Document docUndefined = new Document();

        Bson eqNull = Filters.eq("age", null);
        Bson eqValue = Filters.eq("age", 10);

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(eqNull), docUndefined).isTrue());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(eqValue), docUndefined).isFalse());
    }


    @Test
    public void testNotEqualsNull() {
        Document docNull = new Document().append("age", null);

        Bson neNull = Filters.ne("age", null);
        Bson neValue = Filters.ne("age", 10);

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(neNull), docNull).isFalse());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(neValue), docNull).isTrue());
    }

    @Test
    public void testNotEqualsMissingField() {
        Document docUndefined = new Document();

        Bson neNull = Filters.ne("age", null);
        Bson neValue = Filters.ne("age", 10);

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(neNull), docUndefined).isFalse());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(neValue), docUndefined).isTrue());
    }

    @Test
    public void testTypeNull() {
        Document docNull = new Document().append("field", null);

        Bson typeNull = Filters.type("field", BsonType.NULL);
        Bson typeInt = Filters.type("field", BsonType.INT32);

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(typeNull), docNull).isTrue());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(typeInt), docNull).isFalse());
    }

    @Test
    public void testTypeMissingField() {
        Document docUndefined = new Document();

        Bson typeNull = Filters.type("field", BsonType.NULL);
        Bson typeInt = Filters.type("field", BsonType.INT32);

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(typeNull), docUndefined).isFalse());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(typeInt), docUndefined).isFalse());
    }

    @Test
    public void testExistsMissingField() {
        Document docUndefined = new Document();
        Bson existsTrue = Filters.exists("age", true);
        Bson existsFalse = Filters.exists("age", false);

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        // When field is missing, exists:true should be false, exists:false should be true
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(existsTrue), docUndefined).isFalse());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(existsFalse), docUndefined).isTrue());
    }

    @Test
    public void testExistsNull() {
        Document docNull = new Document().append("age", null);
        Bson existsTrue = Filters.exists("age", true);
        Bson existsFalse = Filters.exists("age", false);

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        // When field is present but null, it still exists in MongoDB
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(existsTrue), docNull).isTrue());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(existsFalse), docNull).isFalse());
    }

    @Test
    public void testTrueOperation() {
        Document document = new Document().append("name", "Bob");
        Bson emptyFilter = Filters.empty();
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();
        Truthness result = calculator.computeHeuristicDocument(convertToDocument(emptyFilter), document);
        assertTrue(result.isTrue());
    }

    @Test
    public void testTaintHandlerCalledForStringEquals() {
        TaintHandler taintHandler = new TaintHandlerExecutionTracer();
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator(taintHandler);

        Document doc = new Document().append("name", "_EM_1111_XYZ_");
        Bson filter = Filters.eq("name", "bar");

        calculator.computeHeuristicDocument(convertToDocument(filter), doc);
        final List<AdditionalInfo> additionalInfos = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfos.size());
        final Map<String, Set<StringSpecializationInfo>> stringSpecializationsView = additionalInfos.get(0).getStringSpecializationsView();
        assertTrue(stringSpecializationsView.containsKey("_EM_1111_XYZ_"));
        assertEquals(1, stringSpecializationsView.get("_EM_1111_XYZ_").size());
        assertEquals("bar", stringSpecializationsView.get("_EM_1111_XYZ_").iterator().next().getValue());
    }

    @Test
    public void testTaintHandlerCalledForObjectIdEquals() {
        TaintHandler taintHandler = new TaintHandlerExecutionTracer();
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator(taintHandler);

        Document doc = new Document().append("name", "_EM_1111_XYZ_");
        ObjectId objectId = new ObjectId("64b7f3b5e13823708a6a1234");
        Bson filter = Filters.eq("name", objectId);

        calculator.computeHeuristicDocument(convertToDocument(filter), doc);
        final List<AdditionalInfo> additionalInfos = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfos.size());
        final Map<String, Set<StringSpecializationInfo>> stringSpecializationsView = additionalInfos.get(0).getStringSpecializationsView();
        assertTrue(stringSpecializationsView.containsKey("_EM_1111_XYZ_"));
        assertEquals(1, stringSpecializationsView.get("_EM_1111_XYZ_").size());
        assertEquals("64b7f3b5e13823708a6a1234", stringSpecializationsView.get("_EM_1111_XYZ_").iterator().next().getValue());
    }



    public static Document convertToDocument(Bson filter) {
        BsonDocument bsonDocument = filter.toBsonDocument();
        DocumentCodec documentCodec = new DocumentCodec();
        return documentCodec.decode(bsonDocument.asBsonReader(), DecoderContext.builder().build());
    }

}
