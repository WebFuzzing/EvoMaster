package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.MongoCollectionInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


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
            category = ReplacementCategory.SQL,
            id = "constructorEntity"
    )
    public static void MappingMongoEntityInformation(@ThirdPartyCast(actualType = "org.springframework.data.mongodb.core.mapping.MongoPersistentEntity") Object entity) {
        handleMappingMongoEntityInformationConstructor("constructorEntity", Collections.singletonList(entity));
    }

    @Replacement(
            replacingConstructor = true,
            type = ReplacementType.TRACKER,
            category = ReplacementCategory.SQL,
            id = "constructorEntityCustomCollectionName"
    )
    public static void MappingMongoEntityInformation(@ThirdPartyCast(actualType = "org.springframework.data.mongodb.core.mapping.MongoPersistentEntity") Object entity, String customCollectionName) {
        handleMappingMongoEntityInformationConstructor("constructorEntityCustomCollectionName", Arrays.asList(entity, customCollectionName));
    }

    @Replacement(
            replacingConstructor = true,
            type = ReplacementType.TRACKER,
            category = ReplacementCategory.SQL,
            id = "constructorEntityFallbackIdType"
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
            ExecutionTracer.addMongoCollectionInfo(new MongoCollectionInfo(collectionName, repositoryType));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
