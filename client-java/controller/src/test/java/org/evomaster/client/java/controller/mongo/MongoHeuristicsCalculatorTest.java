package org.evomaster.client.java.controller.mongo;

import com.mongodb.client.model.Filters;
import org.bson.*;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.evomaster.client.java.controller.internal.db.mongo.MongoDistanceWithMetrics;
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
    public void testElemMatch() {
        Document doc = new Document().append("results", Arrays.asList(
                new Document("product", "xyz").append("quantity", 5),
                new Document("product", "abc").append("quantity", 15)
        ));

        Bson bsonTrue = Filters.elemMatch("results", Filters.eq("product", "abc"));
        Bson bsonFalse = Filters.elemMatch("results", Filters.eq("product", "def"));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testElemMatchNested() {
        Document doc = new Document("groups", Arrays.asList(
                new Document("members", Arrays.asList(
                        new Document("name", "Bob"),
                        new Document("name", "Alice"))),
                new Document("members", Collections.singletonList(
                        new Document("name", "Eve")))
        ));
        Document bsonTrue = new Document("groups",
                new Document("$elemMatch",
                        new Document("members",
                                new Document("$elemMatch", new Document("name", "Alice")))));
        Document bsonFalse = new Document("groups",
                new Document("$elemMatch",
                        new Document("members",
                                new Document("$elemMatch", new Document("name", "Carol")))));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(bsonTrue, doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(bsonFalse, doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testElemMatchWithEqOperatorDocument() {
        Document doc = new Document().append("tags", Arrays.asList("a", "b", "c"));
        Document bsonTrue = new Document("tags", new Document("$elemMatch", new Document("$eq", "b")));
        Document bsonFalse = new Document("tags", new Document("$elemMatch", new Document("$eq", "z")));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(bsonTrue, doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(bsonFalse, doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testElemMatchWithMultipleConditions() {
        Document doc = new Document().append("results", Arrays.asList(
                new Document("product", "abc").append("quantity", 5),
                new Document("product", "abc").append("quantity", 15)
        ));

        Bson bsonTrue = Filters.elemMatch("results", Filters.and(Filters.eq("product", "abc"), Filters.gt("quantity", 10)));
        Bson bsonFalse = Filters.elemMatch("results", Filters.and(Filters.eq("product", "abc"), Filters.gt("quantity", 20)));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testElemMatchMissingField() {
        Document doc = new Document().append("name", "Bob");
        Bson query = Filters.elemMatch("results", Filters.eq("product", "abc"));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(query), doc);
        assertTrue(distanceMatch.isFalse());
    }

    @Test
    public void testElemMatchNotAList() {
        Document doc = new Document().append("results", new Document("product", "abc"));
        Bson query = Filters.elemMatch("results", Filters.eq("product", "abc"));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(query), doc);
        assertTrue(distanceMatch.isFalse());
    }

    @Test
    public void testElemMatchEmptyList() {
        Document doc = new Document().append("results", Collections.emptyList());
        Bson query = Filters.elemMatch("results", Filters.eq("product", "abc"));

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
    public void testBitsAllClear() {
        Document doc = new Document().append("flags", 0b1010L);
        Bson bsonTrue = Filters.bitsAllClear("flags", 0b0101L);
        Bson bsonFalse = Filters.bitsAllClear("flags", 0b0010L);
        Bson bsonFurtherFromMatch = Filters.bitsAllClear("flags", 0b1010L);

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        Truthness distanceFurtherFromMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFurtherFromMatch), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
        assertTrue(distanceFurtherFromMatch.isFalse());
        assertTrue(distanceNotMatch.getOfTrue() > distanceFurtherFromMatch.getOfTrue());
    }

    @Test
    public void testBitsAllSet() {
        Document doc = new Document().append("flags", 0b1010L);
        Bson bsonTrue = Filters.bitsAllSet("flags", 0b1010L);
        Bson bsonFalse = Filters.bitsAllSet("flags", 0b1110L);
        Bson bsonFurtherFromMatch = Filters.bitsAllSet("flags", 0b1111L);

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        Truthness distanceFurtherFromMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFurtherFromMatch), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
        assertTrue(distanceFurtherFromMatch.isFalse());
        assertTrue(distanceNotMatch.getOfTrue() > distanceFurtherFromMatch.getOfTrue());
    }

    @Test
    public void testBitsAnyClear() {
        Document doc = new Document().append("flags", 0b1010L);
        Bson bsonTrue = Filters.bitsAnyClear("flags", 0b1110L);
        Bson bsonFalse = Filters.bitsAnyClear("flags", 0b1000L);
        Bson bsonFurtherFromTrue = Filters.bitsAnyClear("flags", 0b1010L);

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        Truthness distanceFurtherFromTrue = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFurtherFromTrue), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
        assertTrue(distanceFurtherFromTrue.isFalse());
        assertTrue(distanceMatch.getOfTrue() > distanceFurtherFromTrue.getOfTrue());
    }

    @Test
    public void testBitsAnySet() {
        Document doc = new Document().append("flags", 0b1010L);
        Bson bsonTrue = Filters.bitsAnySet("flags", 0b0010L);
        Bson bsonFalse = Filters.bitsAnySet("flags", 0b0001L);
        Bson bsonFalseFurtherFromTrue = Filters.bitsAnySet("flags", 0b0101L);

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        Truthness distanceNotMatchFurtherFromMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalseFurtherFromTrue), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
        assertTrue(distanceNotMatchFurtherFromMatch.isFalse());
        assertTrue(distanceMatch.getOfTrue() > distanceNotMatchFurtherFromMatch.getOfTrue());
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
    public void testTypeTimestamp() {
        Document doc = new Document().append("timestamp", new BsonTimestamp(1, 1));
        Bson bsonTrue = Filters.type("timestamp", BsonType.TIMESTAMP);
        Bson bsonFalse = Filters.type("timestamp", BsonType.DATE_TIME);
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
    public void testEqualsStringOnIntegerField() {
        Document doc = new Document().append("value", 42);
        Bson bson = Filters.eq("value", "42");

        Truthness distance = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bson), doc);

        assertTrue(distance.isFalse());
    }

    @Test
    public void testEqualsStringOnBooleanField() {
        Document doc = new Document().append("value", true);
        Bson bson = Filters.eq("value", "bar");

        Truthness distance = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bson), doc);

        assertTrue(distance.isFalse());
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
    public void testEqualsTimestamp() {
        BsonTimestamp value = new BsonTimestamp(1, 1);
        BsonTimestamp otherValue = new BsonTimestamp(1, 2);

        Document doc = new Document().append("timestamp", value);
        Bson bsonTrue = Filters.eq("timestamp", value);
        Bson bsonFalse = Filters.eq("timestamp", otherValue);
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testEqualsDateTimeWithTimestamp() {
        Document doc = new Document().append("value", new Date(1_000L));
        Bson bson = Filters.eq("value", new BsonTimestamp(1, 1));

        Truthness distance = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bson), doc);

        assertTrue(distance.isFalse());
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

    @Test
    public void testLessThanDateTime() {
        Document doc = new Document().append("date", new Date(2_000L));
        Bson bsonTrue = Filters.lt("date", new Date(3_000L));
        Bson bsonFalse = Filters.lt("date", new Date(2_000L));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testLessThanEqualsDateTime() {
        Document doc = new Document().append("date", new Date(2_000L));
        Bson bsonTrue = Filters.lte("date", new Date(2_000L));
        Bson bsonFalse = Filters.lte("date", new Date(1_000L));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testGreaterThanDateTime() {
        Document doc = new Document().append("date", new Date(2_000L));
        Bson bsonTrue = Filters.gt("date", new Date(1_000L));
        Bson bsonFalse = Filters.gt("date", new Date(2_000L));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testGreaterThanEqualsDateTime() {
        Document doc = new Document().append("date", new Date(2_000L));
        Bson bsonTrue = Filters.gte("date", new Date(2_000L));
        Bson bsonFalse = Filters.gte("date", new Date(3_000L));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testLessThanTimestamp() {
        Document doc = new Document().append("timestamp", new BsonTimestamp(2, 0));
        Bson bsonTrue = Filters.lt("timestamp", new BsonTimestamp(3, 0));
        Bson bsonFalse = Filters.lt("timestamp", new BsonTimestamp(2, 0));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testLessThanEqualsTimestamp() {
        Document doc = new Document().append("timestamp", new BsonTimestamp(2, 0));
        Bson bsonTrue = Filters.lte("timestamp", new BsonTimestamp(2, 0));
        Bson bsonFalse = Filters.lte("timestamp", new BsonTimestamp(1, 0));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testGreaterThanTimestamp() {
        Document doc = new Document().append("timestamp", new BsonTimestamp(2, 0));
        Bson bsonTrue = Filters.gt("timestamp", new BsonTimestamp(1, 0));
        Bson bsonFalse = Filters.gt("timestamp", new BsonTimestamp(2, 0));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testGreaterThanEqualsTimestamp() {
        Document doc = new Document().append("timestamp", new BsonTimestamp(2, 0));
        Bson bsonTrue = Filters.gte("timestamp", new BsonTimestamp(2, 0));
        Bson bsonFalse = Filters.gte("timestamp", new BsonTimestamp(3, 0));

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testLessThanBoolean() {
        Document doc = new Document().append("active", false);
        Bson bsonTrue = Filters.lt("active", true);
        Bson bsonFalse = Filters.lt("active", false);

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testLessThanEqualsBoolean() {
        Document doc = new Document().append("active", true);
        Bson bsonTrue = Filters.lte("active", true);
        Bson bsonFalse = Filters.lte("active", false);

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testGreaterThanBoolean() {
        Document doc = new Document().append("active", true);
        Bson bsonTrue = Filters.gt("active", false);
        Bson bsonFalse = Filters.gt("active", true);

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testGreaterThanEqualsBoolean() {
        Document doc = new Document().append("active", false);
        Bson bsonTrue = Filters.gte("active", false);
        Bson bsonFalse = Filters.gte("active", true);

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testLessThanString() {
        Document doc = new Document().append("name", "banana");
        Bson bsonTrue = Filters.lt("name", "cherry");
        Bson bsonFalse = Filters.lt("name", "banana");

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testLessThanEqualsString() {
        Document doc = new Document().append("name", "banana");
        Bson bsonTrue = Filters.lte("name", "banana");
        Bson bsonFalse = Filters.lte("name", "apple");

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testGreaterThanString() {
        Document doc = new Document().append("name", "banana");
        Bson bsonTrue = Filters.gt("name", "apple");
        Bson bsonFalse = Filters.gt("name", "banana");

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

    @Test
    public void testGreaterThanEqualsString() {
        Document doc = new Document().append("name", "banana");
        Bson bsonTrue = Filters.gte("name", "banana");
        Bson bsonFalse = Filters.gte("name", "cherry");

        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);

        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }

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
    public void testNotEqualsLists() {
        Document doc = new Document().append("employees", Arrays.asList("Alice"));
        Bson bsonTrue = Filters.ne("employees", Arrays.asList("Alice", "Bob"));
        Bson bsonFalse = Filters.ne ("employees", Arrays.asList("Alice"));
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
    public void testNearSphereRejectsCloserPointInsideMinimumDistance() {
        Document closerPoint = new Document("location", new Document("type", "Point")
                .append("coordinates", Arrays.asList(0.0, 0.001)));
        Document fartherPoint = new Document("location", new Document("type", "Point")
                .append("coordinates", Arrays.asList(0.0, 0.02)));
        BsonDocument queryPoint = new BsonDocument("type", new BsonString("Point"))
                .append("coordinates", new BsonArray(Arrays.asList(new BsonDouble(0.0), new BsonDouble(0.0))));

        // The closer point is about 111 m away; the farther point is about 2.2 km away.
        Bson nearSphere = Filters.nearSphere("location", queryPoint, 3000.0, 1000.0);
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        Truthness closerResult = calculator.computeHeuristicDocument(convertToDocument(nearSphere), closerPoint);
        Truthness fartherResult = calculator.computeHeuristicDocument(convertToDocument(nearSphere), fartherPoint);

        assertTrue(closerResult.isFalse());
        assertTrue(fartherResult.isTrue());
    }

    @Test
    public void testNear() {
        Document doc = new Document().append("location", new Document().append("type", "Point").append("coordinates", Arrays.asList(-74.044502, 40.689247)));
        BsonDocument point = new BsonDocument().append("type", new BsonString("Point")).append("coordinates", new BsonArray(Arrays.asList(new BsonDouble(2.29441692356368), new BsonDouble(48.858504187164684))));
        Bson bsonTrue = Filters.near("location", point, 6000000.0, 0.0);
        Bson bsonFalse = Filters.near("location", point, 5000000.0, 0.0);
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
    public void testComputeDistanceDocumentsEmptyCollection() {
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();
        Bson filter = Filters.eq("age", 10);

        MongoDistanceWithMetrics result = calculator.computeDistanceDocuments(convertToDocument(filter), Collections.emptyList());

        assertEquals(0, result.numberOfEvaluatedDocuments);
        assertTrue(result.mongoDistance > 0d);
    }

    @Test
    public void testComputeDistanceDocumentsWithMatchingDocument() {
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();
        Bson filter = Filters.eq("age", 10);
        List<Document> documents = Arrays.asList(
                new Document().append("age", 1),
                new Document().append("age", 10)
        );

        MongoDistanceWithMetrics result = calculator.computeDistanceDocuments(convertToDocument(filter), documents);

        assertEquals(2, result.numberOfEvaluatedDocuments);
        assertEquals(0d, result.mongoDistance, 0.000001d);
    }

    @Test
    public void testComputeDistanceDocumentsWithNonMatchingDocuments() {
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();
        Bson filter = Filters.eq("age", 10);
        List<Document> documents = Arrays.asList(
                new Document().append("age", 1),
                new Document().append("age", 2)
        );

        MongoDistanceWithMetrics result = calculator.computeDistanceDocuments(convertToDocument(filter), documents);

        assertEquals(2, result.numberOfEvaluatedDocuments);
        assertTrue(result.mongoDistance > 0d);
    }

    @Test
    public void testTaintHandlerCalledForStringEquals() {
        ExecutionTracer.reset();
        try {
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
        } finally {
            ExecutionTracer.reset();
        }
    }

    @Test
    public void testTaintHandlerCalledForObjectIdEquals() {
        ExecutionTracer.reset();
        try {
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
        } finally {
            ExecutionTracer.reset();
        }
    }

    @Test
    public void testEqualsDateToString() throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date startDate = dateFormat.parse("2025-01-14");

        Document doc = new Document().append("startDate", startDate);
        Bson query = Filters.eq("startDate", "2025-01-14");
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(query), doc);

        assertTrue(distanceNotMatch.isFalse());
    }

    public static Document convertToDocument(Bson filter) {
        BsonDocument bsonDocument = filter.toBsonDocument();
        DocumentCodec documentCodec = new DocumentCodec();
        return documentCodec.decode(bsonDocument.asBsonReader(), DecoderContext.builder().build());
    }

}
