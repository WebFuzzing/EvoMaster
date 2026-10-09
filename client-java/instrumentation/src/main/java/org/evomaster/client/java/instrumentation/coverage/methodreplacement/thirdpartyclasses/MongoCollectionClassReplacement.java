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

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "deleteOneBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.DeleteResult")
    public static Object deleteOne(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter) {
        return handleDelete("deleteOneBson", mongoCollection, Arrays.asList(filter), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "deleteOneBsonDeleteOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.DeleteResult")
    public static Object deleteOne(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "com.mongodb.client.model.DeleteOptions") Object options) {
        return handleDelete("deleteOneBsonDeleteOptions", mongoCollection, Arrays.asList(filter, options), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "deleteOneClientSessionBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.DeleteResult")
    public static Object deleteOne_EM_0(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter) {
        return handleDelete("deleteOneClientSessionBson", mongoCollection, Arrays.asList(clientSession, filter), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "deleteOneClientSessionBsonDeleteOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.DeleteResult")
    public static Object deleteOne(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "com.mongodb.client.model.DeleteOptions") Object options) {
        return handleDelete("deleteOneClientSessionBsonDeleteOptions", mongoCollection, Arrays.asList(clientSession, filter, options), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "deleteManyBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.DeleteResult")
    public static Object deleteMany(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter) {
        return handleDelete("deleteManyBson", mongoCollection, Arrays.asList(filter), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "deleteManyBsonDeleteOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.DeleteResult")
    public static Object deleteMany(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "com.mongodb.client.model.DeleteOptions") Object options) {
        return handleDelete("deleteManyBsonDeleteOptions", mongoCollection, Arrays.asList(filter, options), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "deleteManyClientSessionBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.DeleteResult")
    public static Object deleteMany_EM_0(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter) {
        return handleDelete("deleteManyClientSessionBson", mongoCollection, Arrays.asList(clientSession, filter), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "deleteManyClientSessionBsonDeleteOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.DeleteResult")
    public static Object deleteMany(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "com.mongodb.client.model.DeleteOptions") Object options) {
        return handleDelete("deleteManyClientSessionBsonDeleteOptions", mongoCollection, Arrays.asList(clientSession, filter, options), filter);
    }


    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "updateOneBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.UpdateResult")
    public static Object updateOne(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object update) {
        return handleUpdate("updateOneBson", mongoCollection, Arrays.asList(filter, update), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "updateOneBsonUpdateOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.UpdateResult")
    public static Object updateOne(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object update, @ThirdPartyCast(actualType = "com.mongodb.client.model.UpdateOptions") Object options) {
        return handleUpdate("updateOneBsonUpdateOptions", mongoCollection, Arrays.asList(filter, update, options), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "updateOneClientSessionBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.UpdateResult")
    public static Object updateOne_EM_0(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object update) {
        return handleUpdate("updateOneClientSessionBson", mongoCollection, Arrays.asList(clientSession, filter, update), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "updateOneClientSessionBsonUpdateOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.UpdateResult")
    public static Object updateOne(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object update, @ThirdPartyCast(actualType = "com.mongodb.client.model.UpdateOptions") Object options) {
        return handleUpdate("updateOneClientSessionBsonUpdateOptions", mongoCollection, Arrays.asList(clientSession, filter, update, options), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "updateManyBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.UpdateResult")
    public static Object updateMany(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object update) {
        return handleUpdate("updateManyBson", mongoCollection, Arrays.asList(filter, update), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "updateManyBsonUpdateOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.UpdateResult")
    public static Object updateMany(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object update, @ThirdPartyCast(actualType = "com.mongodb.client.model.UpdateOptions") Object options) {
        return handleUpdate("updateManyBsonUpdateOptions", mongoCollection, Arrays.asList(filter, update, options), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "updateManyClientSessionBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.UpdateResult")
    public static Object updateMany_EM_0(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object update) {
        return handleUpdate("updateManyClientSessionBson", mongoCollection, Arrays.asList(clientSession, filter, update), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "updateManyClientSessionBsonUpdateOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO, castTo = "com.mongodb.client.result.UpdateResult")
    public static Object updateMany(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object update, @ThirdPartyCast(actualType = "com.mongodb.client.model.UpdateOptions") Object options) {
        return handleUpdate("updateManyClientSessionBsonUpdateOptions", mongoCollection, Arrays.asList(clientSession, filter, update, options), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findOneAndDeleteBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static Object findOneAndDelete(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter) {
        return handleDelete("findOneAndDeleteBson", mongoCollection, Arrays.asList(filter), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findOneAndDeleteBsonFindOneAndDeleteOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static Object findOneAndDelete(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "com.mongodb.client.model.FindOneAndDeleteOptions") Object options) {
        return handleDelete("findOneAndDeleteBsonFindOneAndDeleteOptions", mongoCollection, Arrays.asList(filter, options), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findOneAndDeleteClientSessionBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static Object findOneAndDelete_EM_0(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter) {
        return handleDelete("findOneAndDeleteClientSessionBson", mongoCollection, Arrays.asList(clientSession, filter), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findOneAndDeleteClientSessionBsonFindOneAndDeleteOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static Object findOneAndDelete(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "com.mongodb.client.model.FindOneAndDeleteOptions") Object options) {
        return handleDelete("findOneAndDeleteClientSessionBsonFindOneAndDeleteOptions", mongoCollection, Arrays.asList(clientSession, filter, options), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findOneAndUpdateBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static Object findOneAndUpdate(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object update) {
        return handleUpdate("findOneAndUpdateBson", mongoCollection, Arrays.asList(filter, update), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findOneAndUpdateBsonFindOneAndUpdateOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static Object findOneAndUpdate(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object update, @ThirdPartyCast(actualType = "com.mongodb.client.model.FindOneAndUpdateOptions") Object options) {
        return handleUpdate("findOneAndUpdateBsonFindOneAndUpdateOptions", mongoCollection, Arrays.asList(filter, update, options), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findOneAndUpdateClientSessionBson", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static Object findOneAndUpdate_EM_0(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object update) {
        return handleUpdate("findOneAndUpdateClientSessionBson", mongoCollection, Arrays.asList(clientSession, filter, update), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findOneAndUpdateClientSessionBsonFindOneAndUpdateOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static Object findOneAndUpdate(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object update, @ThirdPartyCast(actualType = "com.mongodb.client.model.FindOneAndUpdateOptions") Object options) {
        return handleUpdate("findOneAndUpdateClientSessionBsonFindOneAndUpdateOptions", mongoCollection, Arrays.asList(clientSession, filter, update, options), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findOneAndReplaceBsonTDocument", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static Object findOneAndReplace(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, Object replacement) {
        return handleUpdate("findOneAndReplaceBsonTDocument", mongoCollection, Arrays.asList(filter, replacement), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findOneAndReplaceBsonTDocumentFindOneAndReplaceOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static Object findOneAndReplace_EM_0(Object mongoCollection, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, Object replacement, @ThirdPartyCast(actualType = "com.mongodb.client.model.FindOneAndReplaceOptions") Object options) {
        return handleUpdate("findOneAndReplaceBsonTDocumentFindOneAndReplaceOptions", mongoCollection, Arrays.asList(filter, replacement, options), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findOneAndReplaceClientSessionBsonTDocument", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static Object findOneAndReplace(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, Object replacement) {
        return handleUpdate("findOneAndReplaceClientSessionBsonTDocument", mongoCollection, Arrays.asList(clientSession, filter, replacement), filter);
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findOneAndReplaceClientSessionBsonTDocumentFindOneAndReplaceOptions", usageFilter = UsageFilter.ANY, category = ReplacementCategory.MONGO)
    public static Object findOneAndReplace(Object mongoCollection, @ThirdPartyCast(actualType = "com.mongodb.client.ClientSession") Object clientSession, @ThirdPartyCast(actualType = "org.bson.conversions.Bson") Object filter, Object replacement, @ThirdPartyCast(actualType = "com.mongodb.client.model.FindOneAndReplaceOptions") Object options) {
        return handleUpdate("findOneAndReplaceClientSessionBsonTDocumentFindOneAndReplaceOptions", mongoCollection, Arrays.asList(clientSession, filter, replacement, options), filter);
    }

    /**
     * Processes the count handling based on the provided parameters.
     *
     * @param id The identifier used to locate the relevant resource.
     * @param mongoCollection The collection object to interact with the database.
     * @param args A list of additional arguments required for the operation.
     * @param filter The filter criteria applied during the handling process.
     * @return The calculated count as a long value.
     */
    private static long handleCount(String id, Object mongoCollection, List<Object> args, Object filter) {
        return (long) handleEagerQuery(id, mongoCollection, args, filter);
    }

    /**
     * Handles the deletion operation for a specific document in the provided collection.
     *
     * @param id The identifier of the document to be deleted.
     * @param mongoCollection The collection from which the document will be deleted.
     * @param args A list of additional arguments required for the delete operation.
     * @param filter The filter conditions to locate the document for deletion.
     * @return The result of the delete operation, which can vary based on the implementation.
     */
    private static Object handleDelete(String id, Object mongoCollection, List<Object> args, Object filter) {
        return handleEagerQuery(id, mongoCollection, args, filter);
    }

    /**
     * Handles the update of documents (updateOne/updateMany) in the provided collection.
     *
     * @param id The identifier of the replacement, used to retrieve the original update method.
     * @param mongoCollection The collection in which the documents will be updated.
     * @param args The arguments of the original update call.
     * @param filter The filter conditions to locate the documents to update.
     * @return The result of the update operation (an UpdateResult).
     */
    private static Object handleUpdate(String id, Object mongoCollection, List<Object> args, Object filter) {
        return handleEagerQuery(id, mongoCollection, args, filter);
    }

    /**
     * Unlike find, operations like count, delete and update are executed eagerly,
     * so we know right away whether the filter was valid
     */
    private static Object handleEagerQuery(String id, Object mongoCollection, List<Object> args, Object filter) {
        return handleEagerQuery(singleton, id, mongoCollection, args, filter);
    }

    /**
     * Handles a lazy query execution on a MongoDB collection and ensures the query is valid
     * by invoking the relevant "find" method and explicitly verifying the results.
     *
     * @param id              The identifier used to retrieve the appropriate find method for the query.
     * @param mongoCollection The MongoDB collection object on which the query is executed.
     * @param args            The list of arguments passed to the find method for the query execution.
     * @param query           The query object representing the query being executed.
     * @return The result of the query execution, typically an iterable containing the query results.
     * @throws RuntimeException If an error occurs during method invocation or if an exception is thrown
     *                          by the query's hasNext() method.
     */
    private static Object handleLazyQuery(String id, Object mongoCollection, List<Object> args, Object query) {
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


    /**
     * Handles the invocation of a "find" operation on the given MongoDB collection.
     * This method retrieves the appropriate find method for the specified identifier
     * and invokes it with the provided arguments. It also ensures that the query execution
     * is valid by verifying the result through its iterator.
     *
     * @param id              The identifier used to retrieve the appropriate "find" method.
     * @param mongoCollection The MongoDB collection object on which the "find" operation is to be executed.
     * @param args            A list of arguments to be passed to the "find" method.
     * @param query           The query object associated with the "find" operation, used for logging and analysis.
     * @return The result of the "find" operation, typically an iterable or cursor representing the query result.
     * @throws RuntimeException If an IllegalAccessException or InvocationTargetException occurs,
     *                          or if the root cause of the exception during invocation is a runtime exception.
     */
    private static Object handleFind(String id, Object mongoCollection, List<Object> args, Object query) {
        return handleLazyQuery(id, mongoCollection, args, query);
    }


    private static Method retrieveFindMethod(String id, Object mongoCollection) {
        return getOriginal(singleton, id, mongoCollection);
    }
}
