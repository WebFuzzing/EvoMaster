package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.neo4j.driver.*;

import java.util.Map;
import java.util.function.Consumer;

/** Queries run inside a managed transaction, where the receiver only exists within the callback. */
public class Neo4JTransactionContextClassReplacementTest extends Neo4JRunReplacementTestBase {

    @Override
    protected void withRunner(Consumer<Object> body) {
        try (Session session = driver.session()) {
            session.executeRead(tx -> {
                body.accept(tx);
                return null;
            });
        }
    }

    @Override
    protected Object run(Object runner, String query) {
        return Neo4JTransactionContextClassReplacement.run(runner, query);
    }

    @Override
    protected Object run(Object runner, String query, Map<String, Object> parameters) {
        return Neo4JTransactionContextClassReplacement.run(runner, query, parameters);
    }

    @Override
    protected Object runWithValue(Object runner, String query, Value parameters) {
        return Neo4JTransactionContextClassReplacement.run_EM_0(runner, query, parameters);
    }

    @Override
    protected Object runWithRecord(Object runner, String query, Record parameters) {
        return Neo4JTransactionContextClassReplacement.run_EM_1(runner, query, parameters);
    }

    @Override
    protected Object run(Object runner, Query query) {
        return Neo4JTransactionContextClassReplacement.run(runner, query);
    }
}
