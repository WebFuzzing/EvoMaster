package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.util.Arrays;
import java.util.List;

/**
 * Replacements for the overloads of updateOne, updateMany and findOneAndUpdate that take an
 * update pipeline (a list of stages) instead of a single update document.
 * Those overloads were added to MongoCollection in driver 3.11.
 * <p>
 * This cannot be in {@link MongoCollectionClassReplacement}: all the original methods of a
 * replacement class are resolved together, and they all must exist in the driver version
 * in use. With older drivers, having these methods there would break all the other
 * replacements for MongoCollection. Here, instead, the methods are resolved
 * only if a call with a pipeline is replaced, which can only happen with drivers having it.
 */
public class MongoCollectionPipelineUpdateClassReplacement extends MongoOperationClassReplacement {
    private static final MongoCollectionPipelineUpdateClassReplacement singleton = new MongoCollectionPipelineUpdateClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "com.mongodb.client.MongoCollection";
    }

    /**
     * Replacement for {@code MongoCollection.updateOne(Bson, List<? extends Bson>)}.
     * <p>
     * Present in the MongoDB Java driver from 3.11.0 (not present in 3.10.2).
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "updateOneBsonPipeline", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.UpdateResult")
    public static Object updateOne(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, List<?> pipeline) {
        return handleEagerQuery(singleton, "updateOneBsonPipeline", mongoCollection, Arrays.asList(filter, pipeline), filter);
    }

    /**
     * Replacement for {@code MongoCollection.updateOne(Bson, List<? extends Bson>, UpdateOptions)}.
     * <p>
     * Present in the MongoDB Java driver from 3.11.0 (not present in 3.10.2).
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "updateOneBsonPipelineUpdateOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.UpdateResult")
    public static Object updateOne(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, List<?> pipeline, @ThirdPartyCast(actualType = "com.mongodb.client.model.UpdateOptions") Object options) {
        return handleEagerQuery(singleton, "updateOneBsonPipelineUpdateOptions", mongoCollection, Arrays.asList(filter, pipeline, options), filter);
    }

    /**
     * Replacement for {@code MongoCollection.updateOne(ClientSession, Bson, List<? extends Bson>)}.
     * <p>
     * Present in the MongoDB Java driver from 3.11.0 (not present in 3.10.2).
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "updateOneClientSessionBsonPipeline", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.UpdateResult")
    public static Object updateOne(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, List<?> pipeline) {
        return handleEagerQuery(singleton, "updateOneClientSessionBsonPipeline", mongoCollection, Arrays.asList(clientSession, filter, pipeline), filter);
    }

    /**
     * Replacement for {@code MongoCollection.updateOne(ClientSession, Bson, List<? extends Bson>, UpdateOptions)}.
     * <p>
     * Present in the MongoDB Java driver from 3.11.0 (not present in 3.10.2).
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "updateOneClientSessionBsonPipelineUpdateOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.UpdateResult")
    public static Object updateOne(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, List<?> pipeline, @ThirdPartyCast(actualType = "com.mongodb.client.model.UpdateOptions") Object options) {
        return handleEagerQuery(singleton, "updateOneClientSessionBsonPipelineUpdateOptions", mongoCollection, Arrays.asList(clientSession, filter, pipeline, options), filter);
    }

    /**
     * Replacement for {@code MongoCollection.updateMany(Bson, List<? extends Bson>)}.
     * <p>
     * Present in the MongoDB Java driver from 3.11.0 (not present in 3.10.2).
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "updateManyBsonPipeline", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.UpdateResult")
    public static Object updateMany(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, List<?> pipeline) {
        return handleEagerQuery(singleton, "updateManyBsonPipeline", mongoCollection, Arrays.asList(filter, pipeline), filter);
    }

    /**
     * Replacement for {@code MongoCollection.updateMany(Bson, List<? extends Bson>, UpdateOptions)}.
     * <p>
     * Present in the MongoDB Java driver from 3.11.0 (not present in 3.10.2).
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "updateManyBsonPipelineUpdateOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.UpdateResult")
    public static Object updateMany(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, List<?> pipeline, @ThirdPartyCast(actualType = "com.mongodb.client.model.UpdateOptions") Object options) {
        return handleEagerQuery(singleton, "updateManyBsonPipelineUpdateOptions", mongoCollection, Arrays.asList(filter, pipeline, options), filter);
    }

    /**
     * Replacement for {@code MongoCollection.updateMany(ClientSession, Bson, List<? extends Bson>)}.
     * <p>
     * Present in the MongoDB Java driver from 3.11.0 (not present in 3.10.2).
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "updateManyClientSessionBsonPipeline", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.UpdateResult")
    public static Object updateMany(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, List<?> pipeline) {
        return handleEagerQuery(singleton, "updateManyClientSessionBsonPipeline", mongoCollection, Arrays.asList(clientSession, filter, pipeline), filter);
    }

    /**
     * Replacement for {@code MongoCollection.updateMany(ClientSession, Bson, List<? extends Bson>, UpdateOptions)}.
     * <p>
     * Present in the MongoDB Java driver from 3.11.0 (not present in 3.10.2).
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "updateManyClientSessionBsonPipelineUpdateOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.UpdateResult")
    public static Object updateMany(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, List<?> pipeline, @ThirdPartyCast(actualType = "com.mongodb.client.model.UpdateOptions") Object options) {
        return handleEagerQuery(singleton, "updateManyClientSessionBsonPipelineUpdateOptions", mongoCollection, Arrays.asList(clientSession, filter, pipeline, options), filter);
    }

    /**
     * Replacement for {@code MongoCollection.findOneAndUpdate(Bson, List<? extends Bson>)}.
     * <p>
     * Present in the MongoDB Java driver from 3.11.0 (not present in 3.10.2).
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findOneAndUpdateBsonPipeline", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static Object findOneAndUpdate(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, List<?> pipeline) {
        return handleEagerQuery(singleton, "findOneAndUpdateBsonPipeline", mongoCollection, Arrays.asList(filter, pipeline), filter);
    }

    /**
     * Replacement for {@code MongoCollection.findOneAndUpdate(Bson, List<? extends Bson>, FindOneAndUpdateOptions)}.
     * <p>
     * Present in the MongoDB Java driver from 3.11.0 (not present in 3.10.2).
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findOneAndUpdateBsonPipelineFindOneAndUpdateOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static Object findOneAndUpdate(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, List<?> pipeline, @ThirdPartyCast(actualType = "com.mongodb.client.model.FindOneAndUpdateOptions") Object options) {
        return handleEagerQuery(singleton, "findOneAndUpdateBsonPipelineFindOneAndUpdateOptions", mongoCollection, Arrays.asList(filter, pipeline, options), filter);
    }

    /**
     * Replacement for {@code MongoCollection.findOneAndUpdate(ClientSession, Bson, List<? extends Bson>)}.
     * <p>
     * Present in the MongoDB Java driver from 3.11.0 (not present in 3.10.2).
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findOneAndUpdateClientSessionBsonPipeline", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static Object findOneAndUpdate(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, List<?> pipeline) {
        return handleEagerQuery(singleton, "findOneAndUpdateClientSessionBsonPipeline", mongoCollection, Arrays.asList(clientSession, filter, pipeline), filter);
    }

    /**
     * Replacement for {@code MongoCollection.findOneAndUpdate(ClientSession, Bson, List<? extends Bson>, FindOneAndUpdateOptions)}.
     * <p>
     * Present in the MongoDB Java driver from 3.11.0 (not present in 3.10.2).
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findOneAndUpdateClientSessionBsonPipelineFindOneAndUpdateOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static Object findOneAndUpdate(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, List<?> pipeline, @ThirdPartyCast(actualType = "com.mongodb.client.model.FindOneAndUpdateOptions") Object options) {
        return handleEagerQuery(singleton, "findOneAndUpdateClientSessionBsonPipelineFindOneAndUpdateOptions", mongoCollection, Arrays.asList(clientSession, filter, pipeline, options), filter);
    }
}
