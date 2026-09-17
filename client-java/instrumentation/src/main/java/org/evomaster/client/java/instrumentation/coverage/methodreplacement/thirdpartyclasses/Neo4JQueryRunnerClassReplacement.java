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
 * Replacement class for instrumenting {@code org.neo4j.driver.QueryRunner.run()} methods.
 * Covers queries run through the {@code QueryRunner} interface, which {@code Session} and {@code Transaction}
 * both implement. Libraries such as Spring Data Neo4j hold the runner by this type, so their call sites name it
 * rather than the concrete one.
 */
public class Neo4JQueryRunnerClassReplacement extends Neo4JOperationClassReplacement {
    private static final Neo4JQueryRunnerClassReplacement singleton = new Neo4JQueryRunnerClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.neo4j.driver.QueryRunner";
    }

    /**
     * Replacement for {@code QueryRunner.run(String query)}.
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = ID_RUN_STRING, usageFilter = UsageFilter.ANY, category = ReplacementCategory.NEO4J, castTo = "org.neo4j.driver.Result")
    public static Object run(Object runner, String query) {
        return handleRun(singleton, ID_RUN_STRING, runner, query, null, Collections.singletonList(query));
    }

    /**
     * Replacement for {@code QueryRunner.run(String query, Map<String, Object> parameters)}.
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = ID_RUN_STRING_MAP, usageFilter = UsageFilter.ANY, category = ReplacementCategory.NEO4J, castTo = "org.neo4j.driver.Result")
    public static Object run(Object runner, String query, Map<String, Object> parameters) {
        return handleRun(singleton, ID_RUN_STRING_MAP, runner, query, parameters, Arrays.asList(query, parameters));
    }

    /**
     * Replacement for {@code QueryRunner.run(String query, Value parameters)}.
     * Uses _EM_0 suffix to avoid signature conflicts.
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = ID_RUN_STRING_VALUE, usageFilter = UsageFilter.ANY, category = ReplacementCategory.NEO4J, castTo = "org.neo4j.driver.Result")
    public static Object run_EM_0(Object runner, String query, @ThirdPartyCast(actualType = "org.neo4j.driver.Value") Object parameters) {
        return handleRun(singleton, ID_RUN_STRING_VALUE, runner, query, parameters, Arrays.asList(query, parameters));
    }

    /**
     * Replacement for {@code QueryRunner.run(String query, Record parameters)}.
     * Uses _EM_1 suffix to avoid signature conflicts.
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = ID_RUN_STRING_RECORD, usageFilter = UsageFilter.ANY, category = ReplacementCategory.NEO4J, castTo = "org.neo4j.driver.Result")
    public static Object run_EM_1(Object runner, String query, @ThirdPartyCast(actualType = "org.neo4j.driver.Record") Object parameters) {
        return handleRun(singleton, ID_RUN_STRING_RECORD, runner, query, parameters, Arrays.asList(query, parameters));
    }

    /**
     * Replacement for {@code QueryRunner.run(Query query)}.
     */
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = ID_RUN_QUERY, usageFilter = UsageFilter.ANY, category = ReplacementCategory.NEO4J, castTo = "org.neo4j.driver.Result")
    public static Object run(Object runner, @ThirdPartyCast(actualType = "org.neo4j.driver.Query") Object query) {
        String queryText = extractQueryText(query);
        Object parameters = extractQueryParameters(query);
        return handleRun(singleton, ID_RUN_QUERY, runner, queryText, parameters, Collections.singletonList(query));
    }
}
