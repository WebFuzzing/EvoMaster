package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * Replacement class for instrumenting {@code org.neo4j.driver.TransactionContext.run()} methods.
 * Covers queries run inside a managed transaction, i.e. the callback of {@code session.executeRead(...)} and
 * {@code session.executeWrite(...)}. The type exists since driver 5.
 */
public class Neo4JTransactionContextClassReplacement extends Neo4JOperationClassReplacement {
    private static final Neo4JTransactionContextClassReplacement singleton = new Neo4JTransactionContextClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.neo4j.driver.TransactionContext";
    }

    /**
     * Replacement for {@code TransactionContext.run(String query)}.
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = ID_RUN_STRING, usageFilter = UsageFilter.ANY, category = ReplacementCategory.NEO4J, castTo = "org.neo4j.driver.Result")
    public static Object run(Object context, String query) {
        return handleRun(singleton, ID_RUN_STRING, context, query, null, Collections.singletonList(query));
    }

    /**
     * Replacement for {@code TransactionContext.run(String query, Map<String, Object> parameters)}.
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = ID_RUN_STRING_MAP, usageFilter = UsageFilter.ANY, category = ReplacementCategory.NEO4J, castTo = "org.neo4j.driver.Result")
    public static Object run(Object context, String query, Map<String, Object> parameters) {
        return handleRun(singleton, ID_RUN_STRING_MAP, context, query, parameters, Arrays.asList(query, parameters));
    }

    /**
     * Replacement for {@code TransactionContext.run(String query, Value parameters)}.
     * Uses _EM_0 suffix to avoid signature conflicts.
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = ID_RUN_STRING_VALUE, usageFilter = UsageFilter.ANY, category = ReplacementCategory.NEO4J, castTo = "org.neo4j.driver.Result")
    public static Object run_EM_0(Object context, String query, @ThirdPartyCast(actualType = "org.neo4j.driver.Value") Object parameters) {
        return handleRun(singleton, ID_RUN_STRING_VALUE, context, query, parameters, Arrays.asList(query, parameters));
    }

    /**
     * Replacement for {@code TransactionContext.run(String query, Record parameters)}.
     * Uses _EM_1 suffix to avoid signature conflicts.
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = ID_RUN_STRING_RECORD, usageFilter = UsageFilter.ANY, category = ReplacementCategory.NEO4J, castTo = "org.neo4j.driver.Result")
    public static Object run_EM_1(Object context, String query, @ThirdPartyCast(actualType = "org.neo4j.driver.Record") Object parameters) {
        return handleRun(singleton, ID_RUN_STRING_RECORD, context, query, parameters, Arrays.asList(query, parameters));
    }

    /**
     * Replacement for {@code TransactionContext.run(Query query)}.
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = ID_RUN_QUERY, usageFilter = UsageFilter.ANY, category = ReplacementCategory.NEO4J, castTo = "org.neo4j.driver.Result")
    public static Object run(Object context, @ThirdPartyCast(actualType = "org.neo4j.driver.Query") Object query) {
        String queryText = extractQueryText(query);
        Object parameters = extractQueryParameters(query);
        return handleRun(singleton, ID_RUN_QUERY, context, queryText, parameters, Collections.singletonList(query));
    }
}
