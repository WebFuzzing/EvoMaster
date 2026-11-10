package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.CursorPreparer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CursorPreparerClassReplacementTest extends MongoCollectionClassReplacementTestBase {

    /**
     * Mimics the object captured by the lambda that Spring creates for a find,
     * as the replacement reads its "query" field reflectively
     */
    public static class FindCallback {
        private final Document query;

        FindCallback(Document query) {
            this.query = query;
        }

        FindIterable<Document> find(MongoCollection<Document> collection) {
            return collection.find(query);
        }
    }

    @BeforeEach
    public void clearDatabase() {
        getMongoCollection().deleteMany(new Document());
        ExecutionTracer.reset();
    }

    private static Function<Object, Object> findFunction(Document query) {
        // the lambda captures a single value, so it is stored in "arg$1", as in Spring's
        return wrapCapturingOnlyCallback(new FindCallback(query));
    }

    @SuppressWarnings("unchecked")
    private static Function<Object, Object> wrapCapturingOnlyCallback(FindCallback callback) {
        return c -> callback.find((MongoCollection<Document>) c);
    }

    private static CursorPreparer paginatingPreparer(int skip, int limit, Document sort) {
        return new CursorPreparer() {
            @Override
            public FindIterable<Document> prepare(FindIterable<Document> iterable) {
                return iterable.skip(skip).limit(limit).sort(sort);
            }
        };
    }

    @Test
    public void testSkipLimitAndSortArePreserved() {
        for (int age = 1; age <= 10; age++) {
            insertPerson("person" + age, age);
        }
        ExecutionTracer.setExecutingInitMongo(false);

        // ascending order, skip first 2, then take 3 => ages 3,4,5
        CursorPreparer preparer = paginatingPreparer(2, 3, new Document("age", 1));

        FindIterable<Document> result = (FindIterable<Document>) CursorPreparerClassReplacement.initiateFind(
                preparer, getMongoCollection(), findFunction(new Document()));

        List<Integer> ages = new ArrayList<>();
        for (Document d : result) {
            ages.add(d.getInteger("age"));
        }
        assertEquals(java.util.Arrays.asList(3, 4, 5), ages);

        assertSingleRecordedCommand(true);
    }

    @Test
    public void testDescendingSortIsPreserved() {
        for (int age = 1; age <= 10; age++) {
            insertPerson("person" + age, age);
        }
        ExecutionTracer.setExecutingInitMongo(false);

        //  descending order, skip the first, then take 2 => ages 9,8
        CursorPreparer preparer = paginatingPreparer(1, 2, new Document("age", -1));

        FindIterable<Document> result = (FindIterable<Document>) CursorPreparerClassReplacement.initiateFind(
                preparer, getMongoCollection(), findFunction(new Document()));

        List<Integer> ages = new ArrayList<>();
        for (Document d : result) {
            ages.add(d.getInteger("age"));
        }
        assertEquals(java.util.Arrays.asList(9, 8), ages);
    }
}
