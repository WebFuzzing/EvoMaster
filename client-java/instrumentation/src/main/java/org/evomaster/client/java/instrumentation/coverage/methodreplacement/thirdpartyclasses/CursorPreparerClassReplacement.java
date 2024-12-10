package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.Field;
import java.util.function.Function;

/**
 * This replacement should ideally be unnecessary due to the existence of MongoOperationClassReplacement.
 * However, there appears to be an issue as operations related to IDs are not being captured by this replacement,
 * and the root cause for this behavior is not immediately apparent.
 * Until this issue is resolved, this workaround should address the problem.
 */
public class CursorPreparerClassReplacement extends MongoOperationClassReplacement {
    private static final CursorPreparerClassReplacement singleton = new CursorPreparerClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.springframework.data.mongodb.core.CursorPreparer";
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "initiateFind", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.FindIterable")
    public static Object initiateFind(Object preparer, @ThirdPartyCast(actualType = "com.mongodb.client.MongoCollection") Object mongoCollection, Function<Object, Object> find) {
        return handleFind(mongoCollection, find);
    }

    private static Object handleFind(Object mongoCollection, Function<Object, Object> find) {
        long startTime = System.currentTimeMillis();

        Object argument = getField(find, "arg$1");
        Object query = getField(argument, "query");
        Object result = find.apply(mongoCollection);

        long endTime = System.currentTimeMillis();

        handleMongo(mongoCollection, query, true, endTime - startTime);

        return result;
    }
}
