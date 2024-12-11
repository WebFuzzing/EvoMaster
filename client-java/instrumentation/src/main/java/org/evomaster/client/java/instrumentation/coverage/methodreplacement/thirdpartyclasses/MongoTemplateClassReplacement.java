package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.MongoCollectionSchema;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.object.CustomTypeToOasConverter;
import org.evomaster.client.java.instrumentation.object.GeoJsonPointToOasConverter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;


public class MongoTemplateClassReplacement extends MongoOperationClassReplacement {

    private static final MongoTemplateClassReplacement singleton = new MongoTemplateClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.springframework.data.mongodb.core.MongoTemplate";
    }

    private static final String SAVE_ID = "save";

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = SAVE_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.MONGO)
    public static <T> T save(Object mongoTemplate,
                             T objectToSave,
                             String collectionName) {
        try {
            // Get the save method with two parameters: Object and String
            Method saveMethod = getOriginal(singleton, SAVE_ID, mongoTemplate);
            // Invoke the save method with the provided parameters
            Object result = saveMethod.invoke(mongoTemplate, objectToSave, collectionName);
            // Cast the retrieved instance
            return (T) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static final String FIND_ONE_ID = "findOne";


    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = FIND_ONE_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.MONGO)
    public static <T> T findOne(Object mongoTemplate,
                                @ThirdPartyCast(actualType = "org.springframework.data.mongodb.core.query.Query") Object query,
                                Class<T> entityClass,
                                String collectionName) {
        try {
            addMongoCollectionType(entityClass, collectionName);

            Method findOneMethod = getOriginal(singleton, FIND_ONE_ID, mongoTemplate);
            Object result = findOneMethod.invoke(mongoTemplate, query, entityClass, collectionName);
            return (T) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static <T> void addMongoCollectionType(Class<T> entityClass, String collectionName) {
        List<CustomTypeToOasConverter> converters = Collections.singletonList(new GeoJsonPointToOasConverter());
        String schema = ClassToSchema.getOrDeriveSchemaWithItsRef(entityClass, true, converters);
        ExecutionTracer.addMongoCollectionType(new MongoCollectionSchema(collectionName, schema));
    }

    private static final String FIND_ID = "find";


    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = FIND_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.MONGO)
    public static <T> List<T> find(Object mongoTemplate,
                                   @ThirdPartyCast(actualType = "org.springframework.data.mongodb.core.query.Query") Object query,
                                   Class<T> entityClass,
                                   String collectionName) {
        try {
            addMongoCollectionType(entityClass, collectionName);

            Method findOneMethod = getOriginal(singleton, FIND_ID, mongoTemplate);
            Object result = findOneMethod.invoke(mongoTemplate, query, entityClass, collectionName);
            return (List<T>) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }


}
