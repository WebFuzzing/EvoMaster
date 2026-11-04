package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        return handleFind(preparer, mongoCollection, find);
    }

    private static Object handleFind(Object preparer, Object mongoCollection, Function<Object, Object> find) {
        long startTime = System.currentTimeMillis();

        Object argument = getField(find, "arg$1");
        Object query = getField(argument, "query");
        // the original also applies the read preference and prepare() (skip, limit, sort...), find.apply alone drops them
        Object result = invokeOriginal(preparer, mongoCollection, find);

        long endTime = System.currentTimeMillis();

        handleMongo(mongoCollection, query, true, endTime - startTime);

        return result;
    }

    private static Object invokeOriginal(Object preparer, Object mongoCollection, Function<Object, Object> find) {
        try {
            ClassLoader loader = preparer.getClass().getClassLoader();
            Method initiateFind = loader.loadClass("org.springframework.data.mongodb.core.CursorPreparer")
                    .getMethod("initiateFind", loader.loadClass("com.mongodb.client.MongoCollection"), Function.class);
            return initiateFind.invoke(preparer, mongoCollection, find);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
