package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.CassandraTableSchema;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

/**
 * When a Cassandra repository is created in Spring, a CqlSession is used under the hood.
 * But information about the type of the repository's rows is not transferred to the session.
 * That info is retained on Spring side.
 * So the intention of this replacement is to retrieve that type info.
 * This will allow us to create and insert rows of the correct type in the table (and the repository).
 */
public class MappingCassandraEntityInformationClassReplacement extends ThirdPartyMethodReplacementClass {
    private static final MappingCassandraEntityInformationClassReplacement singleton = new MappingCassandraEntityInformationClassReplacement();
    private static ThreadLocal<Object> instance = new ThreadLocal<>();

    public static final String CONSTRUCTOR_ENTITY_CONVERTER_ID = "constructorEntityConverter";

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.springframework.data.cassandra.repository.support.MappingCassandraEntityInformation";
    }

    public static Object consumeInstance() {
        Object mappingCassandraEntityInformation = instance.get();
        if (mappingCassandraEntityInformation == null) {
            throw new IllegalStateException("No instance to consume");
        }
        instance.set(null);
        return mappingCassandraEntityInformation;
    }

    private static void addInstance(Object x) {
        Object mappingCassandraEntityInformation = instance.get();
        if (mappingCassandraEntityInformation != null) {
            throw new IllegalStateException("Previous instance was not consumed");
        }
        instance.set(x);
    }

    @Replacement(
            replacingConstructor = true,
            type = ReplacementType.TRACKER,
            category = ReplacementCategory.CASSANDRA,
            id = CONSTRUCTOR_ENTITY_CONVERTER_ID,
            castTo = "org.springframework.data.cassandra.repository.support.MappingCassandraEntityInformation"
    )
    public static void MappingCassandraEntityInformation(
            @ThirdPartyCast(actualType = "org.springframework.data.cassandra.core.mapping.CassandraPersistentEntity") Object entity,
            @ThirdPartyCast(actualType = "org.springframework.data.cassandra.core.convert.CassandraConverter") Object converter) {
        Constructor original = getOriginalConstructor(singleton, CONSTRUCTOR_ENTITY_CONVERTER_ID);

        try {
            Object mappingCassandraEntityInformation = original.newInstance(entity, converter);
            addInstance(mappingCassandraEntityInformation);

            Object tableNameId = mappingCassandraEntityInformation.getClass().getMethod("getTableName").invoke(mappingCassandraEntityInformation);
            String tableName = (String) tableNameId.getClass().getMethod("asInternal").invoke(tableNameId);

            Class<?> rowType = (Class<?>) mappingCassandraEntityInformation.getClass().getMethod("getJavaType").invoke(mappingCassandraEntityInformation);
            String schema = ClassToSchema.getOrDeriveSchemaWithItsRef(rowType, true, Collections.emptyList());

            ExecutionTracer.addCassandraTableType(new CassandraTableSchema(tableName, schema));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}