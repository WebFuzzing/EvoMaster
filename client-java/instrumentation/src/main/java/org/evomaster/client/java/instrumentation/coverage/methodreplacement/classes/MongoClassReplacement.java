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


public class MongoClassReplacement extends ThirdPartyMethodReplacementClass {


    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "com.mongodb.client.MongoCollection";
    }

    private static void handleMongo(Object mongoCollection, Object bson, boolean successfullyExecuted, long executionTime) {
        MongoInfo info = new MongoInfo(mongoCollection, bson, successfullyExecuted, executionTime);
        ExecutionTracer.addMongoInfo(info);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "find", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.FindIterable")
    public static Object find(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object bson) {
        long start = System.currentTimeMillis();
        try {
            Method findMethod = getOriginal(new MongoClassReplacement(), "find", mongoCollection);
            Object findIterable = findMethod.invoke(mongoCollection, bson);
            long end = System.currentTimeMillis();
            handleMongo(mongoCollection, bson, true, end - start);
            return findIterable;
        } catch (IllegalAccessException | InvocationTargetException e) {
            //handleMongo(mongoCollection, bson, false, 0);
            throw new RuntimeException(e);
        }
    }
}
