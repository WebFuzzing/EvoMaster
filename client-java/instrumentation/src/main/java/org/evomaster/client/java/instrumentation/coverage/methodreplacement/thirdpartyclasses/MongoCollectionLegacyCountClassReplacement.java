package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.util.Arrays;

/**
 * Replacements for MongoCollection.count(...), which was deprecated in driver 3.8 in favor of
 * countDocuments(...), and removed in 4.x.
 * <p>
 * This cannot be in {@link MongoCollectionClassReplacement}: all the original methods of a
 * replacement class are resolved together, and they all must exist in the driver version
 * in use. As count(...) does not exist in recent drivers, having it there would break all the other
 * replacements for MongoCollection with those drivers. Here, instead, the methods are resolved
 * only if a call to count(...) is replaced, which can only happen with drivers having it.
 * <p>
 * Old versions of Spring Data (2.1 and 2.2) still use count(...) to implement exists and count.
 */
public class MongoCollectionLegacyCountClassReplacement extends MongoOperationClassReplacement {
    private static final MongoCollectionLegacyCountClassReplacement singleton = new MongoCollectionLegacyCountClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "com.mongodb.client.MongoCollection";
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "countBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static long count(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter) {
        return (long) handleEagerQuery(singleton, "countBson", mongoCollection, Arrays.asList(filter), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "countBsonCountOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static long count(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "com.mongodb.client.model.CountOptions") Object options) {
        return (long) handleEagerQuery(singleton, "countBsonCountOptions", mongoCollection, Arrays.asList(filter, options), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "countClientSessionBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static long count_EM_0(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter) {
        return (long) handleEagerQuery(singleton, "countClientSessionBson", mongoCollection, Arrays.asList(clientSession, filter), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "countClientSessionBsonCountOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static long count(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "com.mongodb.client.model.CountOptions") Object options) {
        return (long) handleEagerQuery(singleton, "countClientSessionBsonCountOptions", mongoCollection, Arrays.asList(clientSession, filter, options), filter);
    }
}
