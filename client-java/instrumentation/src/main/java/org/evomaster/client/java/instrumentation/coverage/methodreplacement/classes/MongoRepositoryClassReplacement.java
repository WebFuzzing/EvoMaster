package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class MongoRepositoryClassReplacement extends ThirdPartyMethodReplacementClass {
    private static final MongoRepositoryClassReplacement singleton = new MongoRepositoryClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.springframework.data.mongodb.repository.support.SimpleMongoRepository";
    }

    // This is not working. It may be related to the fact that this constructor seems to be called using reflection.
    // When a MongoRepository is created in Spring it uses a MongoCollection under the hood.
    // The type of the collection is the default (Document) despite probably the repository was created of some custom type like Person.
    // The fact that elements of the collection should be Person is stored in Spring (SimpleMongoRepository).
    // The idea is to instrument the constructor of SimpleMongoRepository to obtain the actual type of the collection.

    // Change category to MONGO
    @Replacement(replacingStatic = false,
            replacingConstructor = true,
            type = ReplacementType.TRACKER, id = "SimpleMongoRepository",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.SQL)
    public static void SimpleMongoRepository(
            Object mongoRepository,
            @ThirdPartyCast(actualType = "org.springframework.data.mongodb.repository.query.MongoEntityInformation") Object metadata,
            @ThirdPartyCast(actualType = "org.springframework.data.mongodb.repository.core.MongoOperations")  Object mongoOperations) {
        try {
            Method method = getOriginal(singleton, "SimpleMongoRepository", mongoRepository);
            method.invoke(mongoRepository, metadata, mongoOperations);
            // Store info
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);
        } catch (InvocationTargetException e){
            throw (RuntimeException) e.getCause();
        }
    }
}
