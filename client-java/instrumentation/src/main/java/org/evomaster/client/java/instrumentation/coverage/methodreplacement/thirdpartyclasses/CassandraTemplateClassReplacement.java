package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.CassandraTableSchema;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * The intention of this replacement is the same as {@link MappingCassandraEntityInformationClassReplacement}:
 * retrieve the entity-type-to-table mapping. But that constructor replacement only fires when Spring Data
 * instantiates a {@code CassandraRepository}; a SUT calling {@code CassandraTemplate} directly (bypassing
 * the repository layer) needs this replacement to have that mapping recorded too.
 */
public class CassandraTemplateClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final CassandraTemplateClassReplacement singleton = new CassandraTemplateClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.springframework.data.cassandra.core.CassandraTemplate";
    }

    private static final String INSERT_ID = "insert";

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = INSERT_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.CASSANDRA)
    public static <T> T insert(Object cassandraTemplate, T entity) {
        try {
            addCassandraTableType(cassandraTemplate, entity.getClass());

            Method insertMethod = getOriginal(singleton, INSERT_ID, cassandraTemplate);
            Object result = insertMethod.invoke(cassandraTemplate, entity);
            return (T) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static final String SELECT_ONE_ID = "selectOneString";

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = SELECT_ONE_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.CASSANDRA)
    public static <T> T selectOne(Object cassandraTemplate, String cql, Class<T> entityClass) {
        try {
            addCassandraTableType(cassandraTemplate, entityClass);

            Method selectOneMethod = getOriginal(singleton, SELECT_ONE_ID, cassandraTemplate);
            Object result = selectOneMethod.invoke(cassandraTemplate, cql, entityClass);
            return (T) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static final String SELECT_ID = "selectString";

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = SELECT_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.CASSANDRA)
    public static <T> List<T> select(Object cassandraTemplate, String cql, Class<T> entityClass) {
        try {
            addCassandraTableType(cassandraTemplate, entityClass);

            Method selectMethod = getOriginal(singleton, SELECT_ID, cassandraTemplate);
            Object result = selectMethod.invoke(cassandraTemplate, cql, entityClass);
            return (List<T>) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static void addCassandraTableType(Object cassandraTemplate, Class<?> entityClass) {
        try {
            Object tableNameId = cassandraTemplate.getClass().getMethod("getTableName", Class.class)
                    .invoke(cassandraTemplate, entityClass);
            String tableName = (String) tableNameId.getClass().getMethod("asInternal").invoke(tableNameId);

            String schema = ClassToSchema.getOrDeriveSchemaWithItsRef(entityClass, true, Collections.emptyList());
            ExecutionTracer.addCassandraTableType(new CassandraTableSchema(tableName, schema));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}