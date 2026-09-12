package org.evomaster.client.java.controller.mongo;

import com.mongodb.client.model.Filters;
import org.bson.*;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.evomaster.client.java.controller.internal.db.mongo.MongoDistanceWithMetrics;
import org.evomaster.client.java.controller.internal.TaintHandlerExecutionTracer;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintType;
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
    public void testEqualsScalarToList() {
        Document doc = new Document().append("tags", Arrays.asList("red","blue"));
        Bson bsonTrue = Filters.eq("tags", "red");
        Bson bsonFalse = Filters.eq("tags", "green");
        Truthness distanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonTrue), doc);
        Truthness distanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(bsonFalse), doc);
        assertTrue(distanceMatch.isTrue());
        assertTrue(distanceNotMatch.isFalse());
    }


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

        Bson negBsonTrue = Filters.nin("age", new ArrayList<>(Arrays.asList(1, 10, 8)));
        Bson negBsonFalse = Filters.nin("age", new ArrayList<>(Arrays.asList(1, 15)));

        Truthness negDistanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(negBsonTrue), doc);
        Truthness negDistanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(negBsonFalse), doc);

        assertTrue(negDistanceMatch.isFalse());
        assertTrue(negDistanceNotMatch.isTrue());

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

        Bson negBsonTrue = Filters.nin("age", new ArrayList<>(Arrays.asList(null, 10, 8)));
        Bson negBsonFalse = Filters.nin("age", new ArrayList<>(Arrays.asList(1, 15)));

        Truthness negDistanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(negBsonTrue), doc);
        Truthness negDistanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(negBsonFalse), doc);

        assertTrue(negDistanceMatch.isFalse());
        assertTrue(negDistanceNotMatch.isTrue());

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

        Bson negBsonTrue = Filters.nin("age", new ArrayList<>(Arrays.asList(null, 10, 8)));
        Bson negBsonFalse = Filters.nin("age", new ArrayList<>(Arrays.asList(1, 15)));

        Truthness negDistanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(negBsonTrue), doc);
        Truthness negDistanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(negBsonFalse), doc);

        assertTrue(negDistanceMatch.isFalse());
        assertTrue(negDistanceNotMatch.isTrue());

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

        Bson negBsonTrue = Filters.nin("tags", new ArrayList<>(Arrays.asList("b", "z")));
        Bson negBsonFalse = Filters.nin("tags", new ArrayList<>(Arrays.asList("x", "y")));

        Truthness negDistanceMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(negBsonTrue), doc);
        Truthness negDistanceNotMatch = new MongoHeuristicsCalculator().computeHeuristicDocument(convertToDocument(negBsonFalse), doc);

        assertTrue(negDistanceMatch.isFalse(), "Should not match because 'b' is in the list");
        assertTrue(negDistanceNotMatch.isTrue(), "Should match because no element is in the list");

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
    public void testAllMissingFieldWithSingleNullExpectedValue() {
        Document docUndefined = new Document();
        Bson allQuery = Filters.all("employees", Collections.singletonList(null));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(allQuery), docUndefined).isTrue());
    }

    @Test
    public void testAllNullFieldWithSingleNullExpectedValue() {
        Document docNull = new Document().append("employees", null);
        Bson allQuery = Filters.all("employees", Collections.singletonList(null));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(allQuery), docNull).isTrue());
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
    public void testAllOnScalarFieldWithSingleDifferentExpectedValue() {
        Document document = new Document().append("tag", "a");
        Bson allQuery = Filters.all("tag", Collections.singletonList("b"));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(allQuery), document).isFalse());
    }

    @Test
    public void testAllListFieldWithNullExpectedValue() {
        Document document = new Document().append("employees", Arrays.asList("Bob", null, "Alice"));
        Bson matchingAllQuery = Filters.all("employees", Collections.singletonList(null));
        Bson nonMatchingAllQuery = Filters.all("employees", Arrays.asList(null, "Carol"));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(matchingAllQuery), document).isTrue());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(nonMatchingAllQuery), document).isFalse());
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
    public void testElemMatchScalarConditionsMustMatchSameElement() {
        Document query = new Document("values",
                new Document("$elemMatch", new Document("$gt", 2).append("$lt", 5)));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(query,
                new Document("values", Arrays.asList(1, 3, 6))).isTrue());
        assertTrue(calculator.computeHeuristicDocument(query,
                new Document("values", Arrays.asList(1, 6))).isFalse());
    }

    @Test
    public void testNotInsideElemMatchNegatesEachElement() {
        Document condition = new Document("$not", new Document("$gt", 2));
        Document query = new Document("values", new Document("$elemMatch", condition));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();
        Document mixed = new Document("values", Arrays.asList(1, 6));

        assertTrue(calculator.computeHeuristicDocument(query, mixed).isTrue());
        assertTrue(calculator.computeHeuristicDocument(query,
                new Document("values", Arrays.asList(3, 6))).isFalse());
        assertTrue(calculator.computeHeuristicDocument(new Document("values", condition), mixed).isFalse());
        assertTrue(calculator.computeHeuristicDocument(new Document("values", condition),
                new Document("values", Arrays.asList(1, 2))).isTrue());
    }

    @Test
    public void testElemMatchDocumentConditionOnMixedArray() {
        Document query = new Document("values", new Document("$elemMatch", new Document("x", null)));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(query,
                new Document("values", Arrays.asList(null, 1, "abc", new Document("x", 2)))).isFalse());
        assertTrue(calculator.computeHeuristicDocument(query,
                new Document("values", Arrays.asList(null, 1, new Document("x", null)))).isTrue());
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
    public void testNotExistsPreservesMissingAndNullFields() {
        Document query = convertToDocument(Filters.not(Filters.exists("age")));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(query, new Document()).isTrue());
        assertTrue(calculator.computeHeuristicDocument(query, new Document("age", null)).isFalse());
        assertTrue(calculator.computeHeuristicDocument(query, new Document("age", 20)).isFalse());
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
    public void testRegexMatchesDocumentField() {
        Document query = new Document("name", new Document("$regex", "hospital"));
        Document matchingDocument = new Document("name", "nearest hospital");
        Document nonMatchingDocument = new Document("name", "medical clinic");
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        Truthness matchingResult = calculator.computeHeuristicDocument(query, matchingDocument);
        Truthness nonMatchingResult = calculator.computeHeuristicDocument(query, nonMatchingDocument);

        assertTrue(matchingResult.isTrue());
        assertTrue(nonMatchingResult.isFalse());
    }

    @Test
    public void testRegexAnchorsAreConsidered() {
        Document query = new Document("name", new Document("$regex", "^hospital$"));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        Truthness exactMatch = calculator.computeHeuristicDocument(query, new Document("name", "hospital"));
        Truthness substringMatch = calculator.computeHeuristicDocument(query, new Document("name", "nearest hospital"));

        assertTrue(exactMatch.isTrue());
        assertTrue(substringMatch.isFalse());
    }

    @Test
    public void testRegexHeuristicRewardsCloserNonMatchingValue() {
        Document query = new Document("name", new Document("$regex", "hospital"));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        Truthness closerResult = calculator.computeHeuristicDocument(query, new Document("name", "hospitel"));
        Truthness fartherResult = calculator.computeHeuristicDocument(query, new Document("name", "clinic"));

        assertTrue(closerResult.isFalse());
        assertTrue(fartherResult.isFalse());
        assertTrue(closerResult.getOfTrue() > fartherResult.getOfTrue());
    }

    @Test
    public void testRegexHeuristicIsPartialByDefault() {
        Document query = new Document("name", new Document("$regex", "hospital"));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        Truthness substringMatch = calculator.computeHeuristicDocument(query, new Document("name", "nearest hospital"));
        assertTrue(substringMatch.isTrue());
    }

    @Test
    public void testRegexHeuristicWholeWordMatch() {
        Document query = new Document("name", new Document("$regex", "^hospital$"));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        Truthness wholeWordMatch = calculator.computeHeuristicDocument(query, new Document("name", "hospital"));
        Truthness substringDoesNotMatch = calculator.computeHeuristicDocument(query, new Document("name", "nearest hospital"));

        assertTrue(wholeWordMatch.isTrue());
        assertTrue(substringDoesNotMatch.isFalse());
    }


    @Test
    public void testRegexMatchesDocumentFieldWithCaseInsensitiveOption() {
        Document query = new Document("name",
                new Document("$regex", "^general hospital$")
                        .append("$options", "i"));
        Document document = new Document("name", "GENERAL HOSPITAL");

        Truthness result = new MongoHeuristicsCalculator().computeHeuristicDocument(query, document);

        assertTrue(result.isTrue());
    }

    @Test
    public void testRegexMultilineOptionIsConsidered() {
        Document document = new Document("description", "clinic\nhospital\npharmacy");
        Document withoutMultiline = new Document("description", new Document("$regex", "^hospital$"));
        Document withMultiline = new Document("description",
                new Document("$regex", "^hospital$").append("$options", "m"));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(withoutMultiline, document).isFalse());
        assertTrue(calculator.computeHeuristicDocument(withMultiline, document).isTrue());
    }

    @Test
    public void testRegexDotAllOptionIsConsidered() {
        Document document = new Document("description", "hospital\nis near");
        Document withoutDotAll = new Document("description", new Document("$regex", "hospital.*near"));
        Document withDotAll = new Document("description",
                new Document("$regex", "hospital.*near").append("$options", "s"));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(withoutDotAll, document).isFalse());
        assertTrue(calculator.computeHeuristicDocument(withDotAll, document).isTrue());
    }

    @Test
    public void testRegexExtendedOptionIsConsidered() {
        Document document = new Document("name", "generalhospital");
        Document withoutExtended = new Document("name", new Document("$regex", "general hospital"));
        Document withExtended = new Document("name",
                new Document("$regex", "general hospital").append("$options", "x"));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(withoutExtended, document).isFalse());
        assertTrue(calculator.computeHeuristicDocument(withExtended, document).isTrue());
    }

    @Test
    public void testRegexUnicodeOptionIsConsidered() {
        Document document = new Document("name", "ÅLAND HOSPITAL");
        Document asciiCaseInsensitive = new Document("name",
                new Document("$regex", "^åland hospital$").append("$options", "i"));
        Document unicodeCaseInsensitive = new Document("name",
                new Document("$regex", "^åland hospital$").append("$options", "iu"));

        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();
        final Truthness asciiCaseInsensitiveTruthness = calculator.computeHeuristicDocument(asciiCaseInsensitive, document);
        final Truthness unicodeCaseInsensitiveTruthness = calculator.computeHeuristicDocument(unicodeCaseInsensitive, document);

        assertTrue(asciiCaseInsensitiveTruthness.isTrue());
        assertTrue(unicodeCaseInsensitiveTruthness.isTrue());
    }

    @Test
    public void testRegexDoesNotMatchMissingNullOrNonStringField() {
        Document query = new Document("name", new Document("$regex", "hospital"));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(query, new Document()).isFalse());
        assertTrue(calculator.computeHeuristicDocument(query, new Document("name", null)).isFalse());
        assertTrue(calculator.computeHeuristicDocument(query, new Document("name", 42)).isFalse());
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

    /*
        ================================================================================
        Cases about $geoIntersects with GeoJSON geometries (Point, LineString, Polygon,
        and combinations of them). The distance heuristic is an approximate planar
        distance between the query geometry and the document's geometry (see
        GeoJsonGeometryIntersection); 0 iff they would satisfy $geoIntersects.
        ================================================================================
     */

    private static Document geoIntersectsQuery(String fieldName, Document geometry) {
        return new Document(fieldName,
                new Document("$geoIntersects", new Document("$geometry", geometry)));
    }

    @Test
    public void testGeoIntersectsPointInsidePolygon() {
        Document square = new Document("type", "Polygon")
                .append("coordinates", Collections.singletonList(Arrays.asList(
                        Arrays.asList(0, 0), Arrays.asList(10, 0),
                        Arrays.asList(10, 10), Arrays.asList(0, 10), Arrays.asList(0, 0))));
        Document query = geoIntersectsQuery("area", square);
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        Document pointInside = new Document("area",
                new Document("type", "Point").append("coordinates", Arrays.asList(5, 5)));
        Document pointOnBoundary = new Document("area",
                new Document("type", "Point").append("coordinates", Arrays.asList(0, 5)));
        Document pointOutside = new Document("area",
                new Document("type", "Point").append("coordinates", Arrays.asList(20, 20)));

        assertTrue(calculator.computeHeuristicDocument(query, pointInside).isTrue());
        assertTrue(calculator.computeHeuristicDocument(query, pointOnBoundary).isTrue());
        assertTrue(calculator.computeHeuristicDocument(query, pointOutside).isFalse());
    }

    @Test
    public void testGeoIntersectsHeuristicIsGradedByDistanceToPolygon() {
        Document square = new Document("type", "Polygon")
                .append("coordinates", Collections.singletonList(Arrays.asList(
                        Arrays.asList(0, 0), Arrays.asList(10, 0),
                        Arrays.asList(10, 10), Arrays.asList(0, 10), Arrays.asList(0, 0))));
        Document query = geoIntersectsQuery("area", square);
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        Document closer = new Document("area",
                new Document("type", "Point").append("coordinates", Arrays.asList(11, 5)));
        Document fartherAway = new Document("area",
                new Document("type", "Point").append("coordinates", Arrays.asList(100, 5)));

        Truthness closerTruthness = calculator.computeHeuristicDocument(query, closer);
        Truthness fartherTruthness = calculator.computeHeuristicDocument(query, fartherAway);

        assertTrue(closerTruthness.isFalse());
        assertTrue(fartherTruthness.isFalse());
        assertTrue(closerTruthness.getOfTrue() > fartherTruthness.getOfTrue());
    }

    @Test
    public void testGeoIntersectsPolygonWithHoleExcludesHoleInterior() {
        Document squareWithHole = new Document("type", "Polygon")
                .append("coordinates", Arrays.asList(
                        Arrays.asList(Arrays.asList(0, 0), Arrays.asList(10, 0),
                                Arrays.asList(10, 10), Arrays.asList(0, 10), Arrays.asList(0, 0)),
                        Arrays.asList(Arrays.asList(2, 2), Arrays.asList(2, 4),
                                Arrays.asList(4, 4), Arrays.asList(4, 2), Arrays.asList(2, 2))));
        Document query = geoIntersectsQuery("area", squareWithHole);
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        Document pointInHole = new Document("area",
                new Document("type", "Point").append("coordinates", Arrays.asList(3, 3)));
        Document pointInFilledArea = new Document("area",
                new Document("type", "Point").append("coordinates", Arrays.asList(8, 8)));

        assertTrue(calculator.computeHeuristicDocument(query, pointInHole).isFalse());
        assertTrue(calculator.computeHeuristicDocument(query, pointInFilledArea).isTrue());
    }

    @Test
    public void testGeoIntersectsLineStringCrossingPolygon() {
        Document square = new Document("type", "Polygon")
                .append("coordinates", Collections.singletonList(Arrays.asList(
                        Arrays.asList(0, 0), Arrays.asList(10, 0),
                        Arrays.asList(10, 10), Arrays.asList(0, 10), Arrays.asList(0, 0))));
        Document query = geoIntersectsQuery("path", square);
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        Document crossingLine = new Document("path", new Document("type", "LineString")
                .append("coordinates", Arrays.asList(Arrays.asList(-5, 5), Arrays.asList(15, 5))));
        Document disjointLine = new Document("path", new Document("type", "LineString")
                .append("coordinates", Arrays.asList(Arrays.asList(20, 20), Arrays.asList(30, 30))));

        assertTrue(calculator.computeHeuristicDocument(query, crossingLine).isTrue());
        assertTrue(calculator.computeHeuristicDocument(query, disjointLine).isFalse());
    }

    @Test
    public void testGeoIntersectsGeometryCollectionMatchesIfAnyMemberIntersects() {
        Document point = new Document("type", "Point").append("coordinates", Arrays.asList(100, 80));
        Document square = new Document("type", "Polygon")
                .append("coordinates", Collections.singletonList(Arrays.asList(
                        Arrays.asList(0, 0), Arrays.asList(10, 0),
                        Arrays.asList(10, 10), Arrays.asList(0, 10), Arrays.asList(0, 0))));
        Document collection = new Document("type", "GeometryCollection")
                .append("geometries", Arrays.asList(point, square));
        Document query = geoIntersectsQuery("area", collection);
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        Document pointInsideSquare = new Document("area",
                new Document("type", "Point").append("coordinates", Arrays.asList(5, 5)));
        Document pointFarFromBoth = new Document("area",
                new Document("type", "Point").append("coordinates", Arrays.asList(-100, -80)));

        assertTrue(calculator.computeHeuristicDocument(query, pointInsideSquare).isTrue());
        assertTrue(calculator.computeHeuristicDocument(query, pointFarFromBoth).isFalse());
    }

    @Test
    public void testGeoIntersectsWithNonGeometryActualValueIsFalse() {
        Document square = new Document("type", "Polygon")
                .append("coordinates", Collections.singletonList(Arrays.asList(
                        Arrays.asList(0, 0), Arrays.asList(10, 0),
                        Arrays.asList(10, 10), Arrays.asList(0, 10), Arrays.asList(0, 0))));
        Document query = geoIntersectsQuery("area", square);
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(query, new Document()).isFalse());
        assertTrue(calculator.computeHeuristicDocument(query, new Document("area", 42)).isFalse());
        assertTrue(calculator.computeHeuristicDocument(query,
                new Document("area", new Document("type", "NotAGeometry"))).isFalse());
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
    public void testTaintHandlingForRegexOperation() {
        ExecutionTracer.reset();
        try {
            TaintHandler taintHandler = new TaintHandlerExecutionTracer();
            MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator(taintHandler);
            Document document = new Document("name", "_EM_1111_XYZ_");
            Document query = new Document("name",
                    new Document("$regex", "hospital").append("$options", "i"));

            calculator.computeHeuristicDocument(query, document);

            List<AdditionalInfo> additionalInfos = ExecutionTracer.exposeAdditionalInfoList();
            assertEquals(1, additionalInfos.size());
            Map<String, Set<StringSpecializationInfo>> specializations =
                    additionalInfos.get(0).getStringSpecializationsView();
            assertTrue(specializations.containsKey("_EM_1111_XYZ_"));
            assertEquals(1, specializations.get("_EM_1111_XYZ_").size());

            StringSpecializationInfo specialization =
                    specializations.get("_EM_1111_XYZ_").iterator().next();
            assertEquals("hospital", specialization.getValue());
            // TODO: Regex external flags are not monitored by the taintHandler
            assertEquals(0, specialization.getExternalRegexFlagsBitmask());
            // TODO: The regex specialization type should be REGEX_PARTIAL, but currently it is REGEX_WHOLE.
            assertEquals(StringSpecialization.REGEX_WHOLE, specialization.getStringSpecialization());
            assertEquals(TaintType.FULL_MATCH, specialization.getType());


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


    /*
        ================================================================================
        MongoDB semantics: behaviour that is currently not reproduced by the calculator.

        Every expected value below was obtained by running the same query and the same
        document against a real MongoDB 7.0.40 server, so the assertions state what the
        database actually does rather than an interpretation of the documentation.

        The @Disabled tests fail today. Removing the annotation is all that is needed
        once the corresponding behaviour is implemented, and they are deliberately one
        per defect so they can be enabled independently, in any order.

        First, a few tests of behaviour that is already correct, as a guard while the
        heuristic is being changed.
        ================================================================================
     */

    @Test
    public void testOrderingComparisonsDoNotMatchAcrossIncomparableTypes() {
        // mongo: {value:42} is matched by none of these, a number and a string do not compare
        Document doc = new Document().append("value", 42);
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.gt("value", "abc")), doc).isFalse());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.gte("value", "abc")), doc).isFalse());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.lt("value", "abc")), doc).isFalse());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.lte("value", "abc")), doc).isFalse());
    }

    @Test
    public void testEqualsMatchingAnArrayFieldAsAWhole() {
        // mongo: matches, an array is equal to another one with the same elements in order
        Document doc = new Document().append("tags", new ArrayList<>(Arrays.asList("a", "b")));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.eq("tags", Arrays.asList("a", "b"))), doc).isTrue());
        // mongo: no match, the order of the elements differs
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.eq("tags", Arrays.asList("b", "a"))), doc).isFalse());
    }

    @Test
    public void testTypeWithANumericCodeAndBitsWithALongBitmask() {
        // the two forms of these operators that are handled today, see the disabled tests below
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        Document stringField = new Document().append("a", "abc");
        Document typeQuery = new Document().append("a", new Document().append("$type", 2));
        assertTrue(calculator.computeHeuristicDocument(typeQuery, stringField).isTrue());

        Document flags = new Document().append("a", 5);
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.bitsAllSet("a", 1L)), flags).isTrue());
    }

    @Test
    public void testNotEqualsAgainstAnArrayField() {
        /*
            mongo: no match, the array holds 1.

            This one already answers correctly, but only because two of the defects below
            cancel each other out: $ne does not look at the elements of the array, which on
            its own would answer true, and the comparison of two values of incomparable BSON
            types answers false regardless of the operator, which brings it back to false.
            Fixing either one alone turns this into a false positive, so it is worth keeping
            as it is while both are addressed.
         */
        Document doc = new Document().append("a", new ArrayList<>(Arrays.asList(1, 2, 3)));
        assertTrue(new MongoHeuristicsCalculator()
                .computeHeuristicDocument(convertToDocument(Filters.ne("a", 1)), doc).isFalse());
    }

    /*
        ================================================================================
        Queries that make the calculator throw. As MongoHandler does not catch anything,
        the exception escapes the whole heuristics computation for the action, so the
        ExtraHeuristicsDto is lost, including the SQL heuristics computed before it.
        ================================================================================
     */

    @Test
    @Disabled("$type with a string alias is not parsed, so the calculator throws a NullPointerException")
    public void testTypeWithAStringAlias() {
        /*
            mongo: {"a": {"$type": "string"}} matches {a:'abc'} and not {a:5}.
            The string aliases are the documented form of this operator. Only the numeric
            codes are parsed today, so the query parses to null and the calculator throws.
            Note that {"$type": "array"} needs the value of the field to be compared as an
            array, which is a second problem in the same operator: the check is made on the
            name of the Java class, so an ArrayList is never a java.util.List.
         */
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.type("a", "string")), new Document().append("a", "abc")).isTrue());
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.type("a", "string")), new Document().append("a", 5)).isFalse());
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.type("a", "array")),
                new Document().append("a", new ArrayList<>(Arrays.asList(1, 2)))).isTrue());
    }

    @Test
    public void testBitsWithAnIntegerBitmask() {
        /*
            mongo: matches, any integer bitmask is accepted. It arrives as an Integer
            whenever the value fits in one, so this is an ordinary query. The selectors
            accept only a Long, so the query parses to null and the calculator throws.
         */
        Document doc = new Document().append("a", 5);
        Document query = new Document().append("a", new Document().append("$bitsAllSet", 1));

        assertTrue(new MongoHeuristicsCalculator().computeHeuristicDocument(query, doc).isTrue());
    }

    @Test
    public void testInAndNotInWithAnEmptyListOfValues() {
        // mongo: {$in: []} matches nothing, {$nin: []} matches everything
        Document doc = new Document().append("tags", new ArrayList<>(Arrays.asList("a", "b")));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.in("tags", Collections.emptyList())), doc).isFalse());
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.nin("tags", Collections.emptyList())), doc).isTrue());
    }

    @Test
    public void testInAndNotInAgainstAnEmptyArrayField() {
        // mongo: an empty array holds no value, so $in does not match and $nin does
        Document doc = new Document().append("tags", Collections.emptyList());
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.in("tags", Arrays.asList("a"))), doc).isFalse());
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.nin("tags", Arrays.asList("a"))), doc).isTrue());
    }

    @Test
    public void testEqualsBetweenEmptyArrays() {
        /*
            mongo: matches, two empty arrays are equal. The two are of equal size, so the
            comparison aggregates over their elements, of which there are none.
         */
        Document doc = new Document().append("tags", Collections.emptyList());
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.eq("tags", Collections.emptyList())), doc).isTrue());
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.ne("tags", Collections.emptyList())), doc).isFalse());
    }

    @Test
    public void testFieldsHoldingASubDocument() {
        /*
            mongo: does not fail on any of these, it simply does not match. A sub-document
            reaches the "Unsupported type" branch instead, which throws. Arrays of
            sub-documents are the common case of this.
         */
        Document subDocument = new Document().append("a", new Document().append("x", 1));
        Document arrayOfSubDocuments = new Document().append("a",
                new ArrayList<>(Arrays.asList(new Document().append("x", 1))));
        Document binaryData = new Document().append("a", new Binary(new byte[]{1, 2}));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.eq("a", 5)), subDocument).isFalse());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.gt("a", 2)), subDocument).isFalse());
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.in("a", Arrays.asList(1, 5))), arrayOfSubDocuments).isFalse());
        // mongo: matches, a sub-document is different from a number
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.ne("a", 5)), subDocument).isTrue());

        // binary data is the other value this calculator has no comparison for
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.eq("a", 5)), binaryData).isFalse());
    }

    /*
        ================================================================================
        Queries that are answered, but not the way MongoDB answers them. The ones that
        report a match where MongoDB has none are the harmful direction: a condition no
        data can satisfy is recorded as covered, so the search stops working towards it.
        ================================================================================
     */

    @Test
    public void testAllQuantifiesOverTheExpectedValues() {
        /*
            $all holds when every expected value is present in the array, and is
            indifferent to any further element. The two readings coincide only when the
            two lists are equal, which is the case the existing testAll covers.
         */
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        // mongo: matches, "a" is present and the extra "b" is irrelevant
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.all("tags", Arrays.asList("a"))),
                new Document().append("tags", new ArrayList<>(Arrays.asList("a", "b")))).isTrue());

        // mongo: no match, "b" and "c" are missing. Reported as fully satisfied today
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.all("tags", Arrays.asList("a", "b", "c"))),
                new Document().append("tags", new ArrayList<>(Arrays.asList("a")))).isFalse());
    }

    @Test
    public void testNotEqualsBetweenIncomparableTypes() {
        // mongo: matches, a number and a string are different from each other
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.ne("value", "abc")), new Document().append("value", 42)).isTrue());
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.ne("value", 1)), new Document().append("value", true)).isTrue());
    }

    @Test
    public void testNotInAgainstAnArrayField() {
        Document doc = new Document().append("tags", new ArrayList<>(Arrays.asList("a", "b")));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        // mongo: no match, the array holds "a", which is excluded. Reported as satisfied today
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.nin("tags", Arrays.asList("a"))), doc).isFalse());
        // mongo: matches, the array holds neither of the excluded values
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.nin("tags", Arrays.asList("z"))), doc).isTrue());
    }

    @Test
    public void testNotOnAMissingFieldWithAnInnerOperatorThatMatchesIt() {
        /*
            mongo: no match for any of these. $ne, $nin and $exists:false all match a
            document in which the field is absent, so negating them must not.
         */
        Document doc = new Document().append("name", "Bob"); // "age" is absent
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.not(Filters.ne("age", 5))), doc).isFalse());
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.not(Filters.nin("age", Arrays.asList(1, 2)))), doc).isFalse());
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.not(Filters.exists("age", false))), doc).isFalse());
    }

    @Test
    public void testFieldsAreMatchedByAnElementOfTheArrayTheyHold() {
        /*
            mongo matches all of these. A field holding an array satisfies a condition when
            the array itself does, or when any one of its elements does, and this applies to
            every condition on a field rather than only to equality.
         */
        Document doc = new Document().append("a", new ArrayList<>(Arrays.asList(1, 2, 3)));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.eq("a", 1)), doc).isTrue());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.gt("a", 2)), doc).isTrue());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.gte("a", 3)), doc).isTrue());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.lt("a", 2)), doc).isTrue());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.lte("a", 1)), doc).isTrue());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.mod("a", 2L, 1L)), doc).isTrue());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.bitsAllSet("a", 1L)), doc).isTrue());

        // mongo: no match, and negating a comparison over an array is where this turns into
        // a false positive today
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.not(Filters.gt("a", 2))), doc).isFalse());
    }

    @Test
    public void testAllOnAScalarField() {
        // mongo: a scalar is matched by a $all listing only values equal to it
        Document doc = new Document().append("tag", "a");
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.all("tag", Arrays.asList("a"))), doc).isTrue());
        // mongo: no match, a scalar cannot be equal to both
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.all("tag", Arrays.asList("a", "b"))), doc).isFalse());
    }

    @Test
    public void testInMatchingAnArrayFieldAsAWhole() {
        Document doc = new Document().append("tags", new ArrayList<>(Arrays.asList("a", "b")));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        // mongo: matches, one of the listed values is the array itself
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.in("tags", Arrays.asList(Arrays.asList("a", "b")))), doc).isTrue());
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.nin("tags", Arrays.asList(Arrays.asList("a", "b")))), doc).isFalse());
    }

    @Test
    public void testInMatchingAnEmptyArrayFieldAsAWhole() {
        Document doc = new Document().append("tags", new ArrayList<>(Arrays.asList()));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        // mongo: matches, one of the listed values is the array itself
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.in("tags", Arrays.asList(Arrays.asList()))), doc).isTrue());
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.nin("tags", Arrays.asList(Arrays.asList()))), doc).isFalse());
    }

    @Test
    public void testBitsDoesNotMatchANonIntegralNumber() {
        // mongo: 3.0 is an integer and is matched, 3.5 is not truncated to 3
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();
        Bson query = Filters.bitsAllSet("a", 1L);

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(query),
                new Document().append("a", 3.0d)).isTrue());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(query),
                new Document().append("a", 3.5d)).isFalse());
    }

    @Test
    public void testElemMatchOnAnArrayOfScalars() {
        // mongo: no match, no element of the array is a document holding "x"
        Document doc = new Document().append("a", new ArrayList<>(Arrays.asList(1, 2, 3)));
        assertTrue(new MongoHeuristicsCalculator().computeHeuristicDocument(
                convertToDocument(Filters.elemMatch("a", Filters.eq("x", 1))), doc).isFalse());
    }

    @Test
    @Disabled("a dotted field path is never resolved into the sub-document it names")
    public void testDottedFieldPath() {
        // mongo: matches both, "a.x" names the field "x" of the sub-document held by "a"
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.eq("a.x", 1)),
                new Document().append("a", new Document().append("x", 1))).isTrue());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.eq("a.x", 1)),
                new Document().append("a", new ArrayList<>(Arrays.asList(
                        new Document().append("x", 1), new Document().append("x", 2))))).isTrue());
    }

    /*
        ================================================================================
        Cases about the BSON value types a field can hold, and about the operators that
        are not modelled at all. Same convention: the expected values come from a real
        MongoDB 7.0.40 server.
        ================================================================================
     */

    @Test
    public void testOrderingComparisonWithNaN() {
        /*
            mongo: no match for either, NaN does not order against anything.
            Both "less than" and "greater or equal" are false for NaN, so the Truthness
            built for the comparison has neither of its two values equal to 1 and its own
            constructor rejects it with "At least one value should be equal to 1".
            NaN is reachable in ordinary data, as any double field can hold it.
         */
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.gt("a", 5.0d)),
                new Document().append("a", Double.NaN)).isFalse());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.lt("a", Double.NaN)),
                new Document().append("a", 5)).isFalse());
    }

    @Test
    @Disabled("an ObjectId is compared against any other value as a string, which orders it wrongly")
    public void testOrderingComparisonBetweenAnObjectIdAndANumber() {
        /*
            mongo: no match, an ObjectId and a number are of different BSON types and do
            not order against each other. The comparison turns both into strings instead,
            so "507f1f77bcf86cd799439011" is greater than "5.0" and the answer is a match.
         */
        Document doc = new Document().append("a", new ObjectId("507f1f77bcf86cd799439011"));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.gt("a", 5.0d)), doc).isFalse());
        assertTrue(calculator.computeHeuristicDocument(convertToDocument(Filters.lt("a", 5.0d)), doc).isFalse());
    }

    @Test
    public void testNullInsideAnArrayField() {
        /*
            mongo: {"a": {"$eq": null}} matches, as one element of the array is null, and
            {"$ne": null} therefore does not. The second is the harmful direction, since it
            is answered as a match today.
         */
        Document doc = new Document().append("a", Arrays.asList(1, null, 3));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.eq("a", null)), doc).isTrue());
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.ne("a", null)), doc).isFalse());
    }

    @Test
    @Disabled("$type with a list of aliases is not parsed, so the calculator throws a NullPointerException")
    public void testTypeWithAListOfAliases() {
        // mongo: matches, the field holds one of the listed types
        Document query = new Document().append("a",
                new Document().append("$type", Arrays.asList("string", "int")));

        assertTrue(new MongoHeuristicsCalculator()
                .computeHeuristicDocument(query, new Document().append("a", "abc")).isTrue());
    }

    @Test
    public void testBitsWithBitPositions() {
        // mongo: matches, bits 0 and 2 are set in 5. The bitmask can be given as positions
        Document query = new Document().append("a",
                new Document().append("$bitsAllSet", Arrays.asList(0, 2)));

        assertTrue(new MongoHeuristicsCalculator()
                .computeHeuristicDocument(query, new Document().append("a", 5)).isTrue());
    }

    @Test
    @Disabled("an operator this calculator does not model makes it throw, instead of degrading")
    public void testOperatorsThatAreNotModelledDoNotThrow() {
        /*
            None of these is parsed, and the calculator then throws a NullPointerException
            on the resulting null operation. As MongoHandler does not catch anything, that
            takes the whole ExtraHeuristicsDto for the action with it, including the SQL
            heuristics computed before it.

            The point of this test is not that these operators should be supported. It is
            that a query using one of them should not cost the action its heuristics: what
            the score ought to be for an operator that is not modelled is a decision for
            the heuristic, so the assertion here is only that nothing is thrown.

            $comment is worth singling out: it attaches to an otherwise ordinary query, so
            {"a": 1, "$comment": "..."} is enough to lose the heuristics of the action.
         */
        Document doc = new Document().append("a", 1);
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        List<Document> queries = Arrays.asList(
                new Document().append("$expr",
                        new Document().append("$eq", Arrays.asList("$a", 1))),
                new Document().append("$jsonSchema",
                        new Document().append("required", Arrays.asList("a"))),
                new Document().append("$where", "function(){ return true; }"),
                new Document().append("$text", new Document().append("$search", "x")),
                new Document().append("a", new Document().append("$geoWithin",
                        new Document().append("$centerSphere",
                                Arrays.asList(Arrays.asList(1.0, 2.0), 0.1))))
        );

        for (Document query : queries) {
            assertDoesNotThrow(() -> calculator.computeHeuristicDocument(query, doc),
                    "should not throw for " + query.toJson());
        }
    }

    /*
        ================================================================================
        Cases about $regex, and about the shapes $not can take. Same convention: the
        expected values come from a real MongoDB 7.0.40 server.
        ================================================================================
     */

    @Test
    public void testRegexMatchingAnElementOfAnArrayField() {
        /*
            mongo: matches, one element of the array matches the expression. $regex follows
            the same rule as the other operators on a field, which it does not apply today.
         */
        Document doc = new Document().append("a", new ArrayList<>(Arrays.asList("abc", "x")));
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.regex("a", "^ab")), doc).isTrue());
        // mongo: no match, neither element matches
        assertTrue(calculator.computeHeuristicDocument(
                convertToDocument(Filters.regex("a", "^zz")), doc).isFalse());
    }

    @Test
    public void testNotWithMoreThanOneInnerOperator() {
        /*
            mongo: {"a": {"$not": {"$gt": 1, "$lt": 9}}} matches {a:1}, as the two inner
            conditions are combined with an implicit and, which is false here, and $not
            negates it.

            The selector gives up whenever the inner expression parses to more than one
            condition, and on a $not nested inside another $not, which MongoDB accepts as
            well. In both cases the query parses to null and the calculator throws on it.
         */
        Document doc = new Document().append("a", 1);
        MongoHeuristicsCalculator calculator = new MongoHeuristicsCalculator();

        Document twoOperators = new Document().append("a",
                new Document().append("$not", new Document().append("$gt", 1).append("$lt", 9)));
        assertTrue(calculator.computeHeuristicDocument(twoOperators, doc).isTrue());
        assertTrue(calculator.computeHeuristicDocument(twoOperators, new Document("a", 5)).isFalse());

        // mongo: no match, the two negations cancel and 1 is not greater than 1
        Document nested = new Document().append("a",
                new Document().append("$not", new Document().append("$not", new Document().append("$gt", 1))));
        assertTrue(calculator.computeHeuristicDocument(nested, doc).isFalse());
        assertTrue(calculator.computeHeuristicDocument(nested, new Document("a", 5)).isTrue());
    }
}
