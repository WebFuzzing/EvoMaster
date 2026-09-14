package org.evomaster.client.java.controller.mongo;

import com.mongodb.client.model.Filters;
import org.bson.BsonRegularExpression;
import org.bson.Document;
import org.bson.types.Binary;
import org.evomaster.client.java.controller.internal.db.mongo.MongoDistanceWithMetrics;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.evomaster.client.java.controller.mongo.MongoHeuristicsCalculatorTest.convertToDocument;
import static org.junit.jupiter.api.Assertions.*;

/*
    ================================================================================
    More MongoDB semantics that the heuristics calculator does not reproduce, in the
    same spirit as the section at the end of MongoHeuristicsCalculatorTest.

    Every expected value below was obtained by running the same query and the same
    document against a MongoDB 7.0.41 server, so the assertions state what the
    database actually does rather than an interpretation of the documentation.

    The @Disabled tests fail today. Removing the annotation is all that is needed
    once the corresponding behaviour is implemented, and they are one per defect so
    they can be enabled independently, in any order.

    The tests that are not disabled pass. They cover the neighbouring case of each
    defect, the one that is already handled, so that a fix can be told apart from a
    regression in the path next to it.
    ================================================================================
 */
public class MongoHeuristicsCalculatorMongoSemanticsTest {

    private static Truthness heuristic(Document query, Document document) {
        return new MongoHeuristicsCalculator().computeHeuristicDocument(query, document);
    }

    /*
        --------------------------------------------------------------------------
        Queries that make the calculator throw. MongoHandler does not catch
        anything around computeDistanceDocuments, so the exception escapes the
        whole heuristics computation for the action, including the SQL heuristics
        computed before it.
        --------------------------------------------------------------------------
     */

    @Test
    @Disabled("$regex against an array holding no strings throws IllegalArgumentException")
    public void testRegexAgainstAnArrayWithoutStringElements() {
        /*
            mongo: no match, and no error. The elements are compared one by one and a
            number never matches a regex.
            The array branch keeps only the strings and then aggregates what is left,
            and the aggregation rejects an empty array. Note that the scalar branch of
            the same operator answers C_FALSE for a number, as this one used to.
         */
        Document query = new Document("a", new Document("$regex", new BsonRegularExpression("x")));
        Document document = new Document("a", new ArrayList<>(Arrays.asList(1, 2, 3)));

        Truthness truthness = assertDoesNotThrow(() -> heuristic(query, document));
        assertTrue(truthness.isFalse());
    }

    @Test
    @Disabled("a bitmask held as org.bson.types.Binary is not parsed, so the calculator throws")
    public void testBitmaskGivenAsBinaryData() {
        /*
            mongo: matches, bit 1 of the mask is set and so is bit 1 of the value.
            A bitmask given as binary data is only recognised when it is a byte[].
            Any query that has been through a BSON decode holds a Binary instead, and
            that one leaves the mask unparsed, so the whole query parses to null.
         */
        Document query = new Document("a", new Document("$bitsAllSet", new Binary(new byte[]{(byte) 0x02})));
        Document document = new Document("a", 2);

        Truthness truthness = assertDoesNotThrow(() -> heuristic(query, document));
        assertTrue(truthness.isTrue());
    }

    @Test
    public void testBitmaskGivenAsAByteArray() {
        // the shape of binary bitmask that is handled today, next to the one above
        Document query = new Document("a", new Document("$bitsAllSet", new byte[]{(byte) 0x02}));
        assertTrue(heuristic(query, new Document("a", 2)).isTrue());
    }

    @Test
    @Disabled("$mod with a divisor of 0 throws ArithmeticException")
    public void testModByZero() {
        // mongo: the query is rejected, "divisor cannot be 0"
        Document query = new Document("a", new Document("$mod", Arrays.asList(0, 0)));
        assertDoesNotThrow(() -> heuristic(query, new Document("a", 5)));
    }

    @Test
    @Disabled("$not holding a bare regex is not parsed, so the calculator throws a NullPointerException")
    public void testNotWithABareRegex() {
        /*
            mongo: matches, {"a": {"$not": /x/}} is a documented form of the operator.
            Only a document is accepted as the value of $not today, so this parses to
            null. Unlike the other two in this group, this is a query mongo answers
            normally rather than one it rejects.
         */
        Document query = new Document("a", new Document("$not", new BsonRegularExpression("x")));

        Truthness truthness = assertDoesNotThrow(() -> heuristic(query, new Document("a", "aa")));
        assertTrue(truthness.isTrue());
    }

    @Test
    @Disabled("$size with a negative value is not parsed, so the calculator throws a NullPointerException")
    public void testNegativeSize() {
        // mongo: the query is rejected, "Expected a non-negative number in: $size: -1"
        Document query = new Document("a", new Document("$size", -1));
        assertDoesNotThrow(() -> heuristic(query, new Document("a", Arrays.asList(1, 2))));
    }

    /*
        --------------------------------------------------------------------------
        Queries that are answered, but not the way the database answers them.
        --------------------------------------------------------------------------
     */

    @Test
    @Disabled("a negative bit position is read as bit 63 instead of being rejected")
    public void testBitmaskWithANegativeBitPosition() {
        /*
            mongo: the query is rejected, "Failed to parse bit position. Expected a
            non-negative number in: 0: -1".
            A shift count is masked to its low six bits in Java, so 1L << -1 sets bit
            63 and the mask becomes Long.MIN_VALUE. Answering false for a query the
            database will not run is fine. Answering that it matches is not.
         */
        Document query = new Document("a", new Document("$bitsAllSet", Collections.singletonList(-1)));
        Document document = new Document("a", Long.MIN_VALUE);

        assertFalse(heuristic(query, document).isTrue());
    }

    @Test
    @Disabled("a bitmask that is not an integer is truncated instead of being rejected")
    public void testBitmaskThatIsNotAnInteger() {
        // mongo: the query is rejected, "Expected an integer: $bitsAllSet: 3.9"
        Document query = new Document("a", new Document("$bitsAllSet", 3.9d));
        assertFalse(heuristic(query, new Document("a", 3)).isTrue());
    }

    @Test
    @Disabled("a negative bitmask is read as all ones instead of being rejected")
    public void testBitmaskThatIsNegative() {
        // mongo: the query is rejected, "Expected a non-negative number in: $bitsAllSet: -1"
        Document query = new Document("a", new Document("$bitsAllSet", -1));
        assertFalse(heuristic(query, new Document("a", -1)).isTrue());
    }

    @Test
    @Disabled("$comment is removed from documents that are values rather than operators")
    public void testCommentInsideALiteralValue() {
        /*
            mongo: matches. A field name that starts with "$" is storable since 5.0,
            and under an explicit $eq the document is compared as a literal.
            The comment operator is removed everywhere it appears in the query, so the
            value being compared loses the key as well and no longer equals the stored
            document.
         */
        Document literal = new Document("$comment", "note").append("x", 1);
        Document query = new Document("a", new Document("$eq", literal));
        Document document = new Document("a", new Document("$comment", "note").append("x", 1));

        assertTrue(heuristic(query, document).isTrue());
    }

    @Test
    public void testCommentAtTopLevel() {
        // the placement of $comment that the query is actually allowed to use
        Document query = new Document("a", 1).append("$comment", "note");
        assertTrue(heuristic(query, new Document("a", 1)).isTrue());
    }

    @Test
    @Disabled("$all with a repeated element does not match a field that is not an array")
    public void testAllWithARepeatedElement() {
        // mongo: matches. $all is an $and of $eq, so a repeat holds against a scalar.
        Document query = new Document("a", new Document("$all", Arrays.asList(1, 1)));
        assertTrue(heuristic(query, new Document("a", 1)).isTrue());
    }

    @Test
    @Disabled("$all holding more than one null does not match a missing field")
    public void testAllWithRepeatedNullsAgainstAMissingField() {
        // mongo: matches, null matches a missing field however many times it is asked for
        Document query = new Document("a", new Document("$all", Arrays.asList(null, null)));
        assertTrue(heuristic(query, new Document("b", 1)).isTrue());
    }

    @Test
    public void testAllWithASingleNullAgainstAMissingField() {
        // the single element case, which is special-cased today
        Document query = new Document("a", new Document("$all", Collections.singletonList(null)));
        assertTrue(heuristic(query, new Document("b", 1)).isTrue());
    }

    @Test
    @Disabled("$in does not match an element of the list that is a regex")
    public void testInWithARegexElement() {
        // mongo: matches, $in accepts regexes among its elements
        Document query = new Document("a",
                new Document("$in", Collections.singletonList(new BsonRegularExpression("x"))));
        assertTrue(heuristic(query, new Document("a", "xy")).isTrue());
    }

    /*
        --------------------------------------------------------------------------
        Not a question of MongoDB semantics, but of the contract of the calculator.
        --------------------------------------------------------------------------
     */

    @Test
    @Disabled("the documents are traversed more than once, so a one-shot Iterable is scored wrongly")
    public void testDocumentsThatCanOnlyBeTraversedOnce() {
        /*
            computeDistanceDocuments counts the documents, computeHeuristicOnDocuments
            counts them again, and the loop then walks them a third time. What is passed
            in today is a FindIterable, which replays, so this costs a round trip to the
            database per traversal rather than a wrong answer. The signature accepts any
            Iterable though, and one that cannot be replayed is scored as if the
            collection were empty while still reporting the documents it counted first.
         */
        List<Document> documents = Arrays.asList(new Document("a", 1), new Document("a", 2));
        Document query = convertToDocument(Filters.eq("a", 2));

        MongoDistanceWithMetrics fromList = new MongoHeuristicsCalculator()
                .computeDistanceDocuments(query, documents);
        MongoDistanceWithMetrics fromOneShot = new MongoHeuristicsCalculator()
                .computeDistanceDocuments(query, new OneShotIterable<>(documents));

        assertEquals(fromList.numberOfEvaluatedDocuments, fromOneShot.numberOfEvaluatedDocuments);
        assertEquals(fromList.mongoDistance, fromOneShot.mongoDistance);
    }

    /**
     * An Iterable that can be traversed only once.
     */
    private static final class OneShotIterable<T> implements Iterable<T> {

        private final Iterable<T> delegate;
        private boolean consumed = false;

        private OneShotIterable(Iterable<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Iterator<T> iterator() {
            if (consumed) {
                return Collections.emptyIterator();
            }
            consumed = true;
            return delegate.iterator();
        }
    }
}
