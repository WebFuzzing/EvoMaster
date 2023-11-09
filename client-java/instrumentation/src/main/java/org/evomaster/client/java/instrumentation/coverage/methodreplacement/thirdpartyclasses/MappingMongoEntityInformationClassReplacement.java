package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.MongoCollectionInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.object.CustomTypeToOasConverter;
import org.evomaster.client.java.instrumentation.object.GeoJsonPointToOasConverter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * When a MongoRepository is created in Spring, a MongoCollection is used under the hood.
 * But information about the type of the repository's documents is not transferred to the collection.
 * That info is retained on Spring side.
 * So the intention of this replacement is to retrieve that type info.
 * This will allow us to create and insert documents of the correct type in the collection (and the repository).
 */
public class MappingMongoEntityInformationClassReplacement extends ThirdPartyMethodReplacementClass {
    private static final MappingMongoEntityInformationClassReplacement singleton = new MappingMongoEntityInformationClassReplacement();
    private static ThreadLocal<Object> instance = new ThreadLocal<>();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.springframework.data.mongodb.repository.support.MappingMongoEntityInformation";
    }

    public static Object consumeInstance() {
        Object mappingMongoEntityInformation = instance.get();
        if (mappingMongoEntityInformation == null) {
            throw new IllegalStateException("No instance to consume");
        }
        instance.set(null);
        return mappingMongoEntityInformation;
    }

    private static void addInstance(Object x) {
        Object mappingMongoEntityInformation = instance.get();
        if (mappingMongoEntityInformation != null) {
            throw new IllegalStateException("Previous instance was not consumed");
        }
        instance.set(x);
    }

    @Replacement(
            replacingConstructor = true,
            type = ReplacementType.TRACKER,
            category = ReplacementCategory.MONGO,
            id = "constructorEntity",
            castTo = "org.springframework.data.mongodb.repository.support.MappingMongoEntityInformation"
    )
    public static void MappingMongoEntityInformation(@ThirdPartyCast(actualType = "org.springframework.data.mongodb.core.mapping.MongoPersistentEntity") Object entity) {
        handleMappingMongoEntityInformationConstructor("constructorEntity", Collections.singletonList(entity));
    }

    @Replacement(
            replacingConstructor = true,
            type = ReplacementType.TRACKER,
            category = ReplacementCategory.MONGO,
            id = "constructorEntityCustomCollectionName",
            castTo = "org.springframework.data.mongodb.repository.support.MappingMongoEntityInformation"
    )
    public static void MappingMongoEntityInformation(@ThirdPartyCast(actualType = "org.springframework.data.mongodb.core.mapping.MongoPersistentEntity") Object entity, String customCollectionName) {
        handleMappingMongoEntityInformationConstructor("constructorEntityCustomCollectionName", Arrays.asList(entity, customCollectionName));
    }

    @Replacement(
            replacingConstructor = true,
            type = ReplacementType.TRACKER,
            category = ReplacementCategory.MONGO,
            id = "constructorEntityFallbackIdType",
            castTo = "org.springframework.data.mongodb.repository.support.MappingMongoEntityInformation"
    )
    public static void MappingMongoEntityInformation(@ThirdPartyCast(actualType = "org.springframework.data.mongodb.core.mapping.MongoPersistentEntity") Object entity, Class<?> fallbackIdType) {
        handleMappingMongoEntityInformationConstructor("constructorEntityFallbackIdType", Arrays.asList(entity, fallbackIdType));
    }

    private static void handleMappingMongoEntityInformationConstructor(String id, List<Object> args) {
        Constructor original = getOriginalConstructor(singleton, id);

        try {
            Object mappingMongoEntityInformation = original.newInstance(args.toArray());
            addInstance(mappingMongoEntityInformation);
            String collectionName = (String) mappingMongoEntityInformation.getClass().getMethod("getCollectionName").invoke(mappingMongoEntityInformation);
            Class<?> repositoryType = (Class<?>) mappingMongoEntityInformation.getClass().getMethod("getJavaType").invoke(mappingMongoEntityInformation);
            List<CustomTypeToOasConverter> converters = Collections.singletonList(new GeoJsonPointToOasConverter());
            String schema = ClassToSchema.getOrDeriveSchemaWithItsRef(repositoryType, true, converters);
            ExecutionTracer.addMongoCollectionInfo(new MongoCollectionInfo(collectionName, schema));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
