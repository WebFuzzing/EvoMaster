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

    /**
     * Replacement for {@code MongoCollection.count(Bson)}.
     * <p>
     * Present in the MongoDB Java driver from 3.0.0 (the first version with {@code MongoCollection}) until 3.12.14 (the last 3.x version checked); removed in 4.0.0.
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "countBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static long count(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter) {
        return (long) handleEagerQuery(singleton, "countBson", mongoCollection, Arrays.asList(filter), filter);
    }

    /**
     * Replacement for {@code MongoCollection.count(Bson, CountOptions)}.
     * <p>
     * Present in the MongoDB Java driver from 3.0.0 (the first version with {@code MongoCollection}) until 3.12.14 (the last 3.x version checked); removed in 4.0.0.
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "countBsonCountOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static long count(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "com.mongodb.client.model.CountOptions") Object options) {
        return (long) handleEagerQuery(singleton, "countBsonCountOptions", mongoCollection, Arrays.asList(filter, options), filter);
    }

    /**
     * Replacement for {@code MongoCollection.count(ClientSession, Bson)}.
     * <p>
     * Present in the MongoDB Java driver from 3.8.0 (not present in 3.7.1) until 3.12.14 (the last 3.x version checked); removed in 4.0.0.
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "countClientSessionBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static long count_EM_0(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter) {
        return (long) handleEagerQuery(singleton, "countClientSessionBson", mongoCollection, Arrays.asList(clientSession, filter), filter);
    }

    /**
     * Replacement for {@code MongoCollection.count(ClientSession, Bson, CountOptions)}.
     * <p>
     * Present in the MongoDB Java driver from 3.8.0 (not present in 3.7.1) until 3.12.14 (the last 3.x version checked); removed in 4.0.0.
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "countClientSessionBsonCountOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static long count(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "com.mongodb.client.model.CountOptions") Object options) {
        return (long) handleEagerQuery(singleton, "countClientSessionBsonCountOptions", mongoCollection, Arrays.asList(clientSession, filter, options), filter);
    }
}
