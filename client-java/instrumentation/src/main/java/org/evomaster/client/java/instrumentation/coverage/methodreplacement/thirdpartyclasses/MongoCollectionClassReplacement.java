package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class MongoCollectionClassReplacement extends MongoOperationClassReplacement {
    private static final MongoCollectionClassReplacement singleton = new MongoCollectionClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "com.mongodb.client.MongoCollection";
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "find", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.FindIterable")
    public static Object find(Object mongoCollection) {
        return handleFind("find", mongoCollection, Collections.emptyList(), null);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findResultClass", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.FindIterable")
    public static <TResult> Object find(Object mongoCollection, Class<TResult> resultClass) {
        return handleFind("findResultClass", mongoCollection, Arrays.asList(resultClass), null);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.FindIterable")
    public static Object find(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter) {
        return handleFind("findBson", mongoCollection, Arrays.asList(filter), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findBsonResultClass", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.FindIterable")
    public static <TResult> Object find(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, Class<TResult> resultClass) {
        return handleFind("findBsonResultClass", mongoCollection, Arrays.asList(filter, resultClass), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findClientSessionBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.FindIterable")
    public static Object find(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter) {
        return handleFind("findClientSessionBson", mongoCollection, Arrays.asList(clientSession, filter), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findClientSessionBsonResultClass", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.FindIterable")
    public static <TResult> Object find(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, Class<TResult> resultClass) {
        return handleFind("findClientSessionBsonResultClass", mongoCollection, Arrays.asList(clientSession, filter, resultClass), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "countDocumentsBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static long countDocuments(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter) {
        return handleCount("countDocumentsBson", mongoCollection, Arrays.asList(filter), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "countDocumentsBsonCountOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static long countDocuments(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "com.mongodb.client.model.CountOptions") Object options) {
        return handleCount("countDocumentsBsonCountOptions", mongoCollection, Arrays.asList(filter, options), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "countDocumentsClientSessionBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static long countDocuments_EM_0(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter) {
        return handleCount("countDocumentsClientSessionBson", mongoCollection, Arrays.asList(clientSession, filter), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "countDocumentsClientSessionBsonCountOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static long countDocuments(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "com.mongodb.client.model.CountOptions") Object options) {
        return handleCount("countDocumentsClientSessionBsonCountOptions", mongoCollection, Arrays.asList(clientSession, filter, options), filter);
    }

    /**
     * Unlike find, count is executed eagerly, so we know right away whether the filter was valid
     */
    private static long handleCount(String id, Object mongoCollection, List<Object> args, Object filter) {
        long start = System.currentTimeMillis();
        Method countMethod = retrieveFindMethod(id, mongoCollection);
        try {
            long result = (long) countMethod.invoke(mongoCollection, args.toArray());
            long end = System.currentTimeMillis();
            handleMongo(mongoCollection, filter, true, end  - start);
            return result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            long end = System.currentTimeMillis();
            handleMongo(mongoCollection, filter, false, end - start);
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    private static Object handleFind(String id, Object mongoCollection, List<Object> args, Object query) {
        long start = System.currentTimeMillis();
        try {
            Method findMethod = retrieveFindMethod(id, mongoCollection);
            Object result = findMethod.invoke(mongoCollection, args.toArray());
            // As the result is usually a lazy cursor, we need to explicitly call hasNext()
            // to ensure that the query is valid.
            Iterable<?> iterableResult = (Iterable<?>) result;
            boolean successfullyExecuted;
            try {
                iterableResult.iterator().hasNext();
                successfullyExecuted = true;
            } catch (RuntimeException e) {
                // If the hasNext() method throws an exception, it means the query was not valid.
                successfullyExecuted = false;
            }
            long end = System.currentTimeMillis();
            handleMongo(mongoCollection, query, successfullyExecuted, end - start);
            return result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static Method retrieveFindMethod(String id, Object mongoCollection) {
        return getOriginal(singleton, id, mongoCollection);
    }
}
