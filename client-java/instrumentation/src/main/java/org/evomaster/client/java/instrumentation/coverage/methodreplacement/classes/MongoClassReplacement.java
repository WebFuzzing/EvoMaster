package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.MongoInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class MongoClassReplacement extends ThirdPartyMethodReplacementClass {
    private static final MongoClassReplacement singleton = new MongoClassReplacement();

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
    public static Object find(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object bson) {
        return handleFind("findBson", mongoCollection, Arrays.asList(bson), bson);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findBsonResultClass", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.FindIterable")
    public static <TResult> Object find(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object bson, Class<TResult> resultClass) {
        return handleFind("findBsonResultClass", mongoCollection, Arrays.asList(bson, resultClass), bson);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findClientSessionBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.FindIterable")
    public static Object find(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object bson) {
        return handleFind("findClientSessionBson", mongoCollection, Arrays.asList(clientSession, bson), bson);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findClientSessionBsonResultClass", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.FindIterable")
    public static <TResult> Object find(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object bson, Class<TResult> resultClass) {
        return handleFind("findClientSessionBsonResultClass", mongoCollection, Arrays.asList(clientSession, bson, resultClass), bson);
    }

    private static Object handleFind(String id, Object mongoCollection, List<Object> args, Object query) {
        long start = System.currentTimeMillis();
        try {
            Method findMethod = retrieveFindMethod(id, mongoCollection);
            Object result = findMethod.invoke(mongoCollection, args.toArray());
            long end = System.currentTimeMillis();
            handleMongo(mongoCollection, query, true, end - start);
            return result;
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);
        } catch (InvocationTargetException e){
            throw (RuntimeException) e.getCause();
        }
    }

    private static Method retrieveFindMethod(String id, Object mongoCollection) {
        return getOriginal(singleton, id, mongoCollection);
    }

    private static void handleMongo(Object mongoCollection, Object bson, boolean successfullyExecuted, long executionTime) {
        MongoInfo info = new MongoInfo(mongoCollection, bson, successfullyExecuted, executionTime);
        ExecutionTracer.addMongoInfo(info);
    }
}
