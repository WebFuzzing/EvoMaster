package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.Neo4JRunCommand;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

public abstract class Neo4JOperationClassReplacement extends ThirdPartyMethodReplacementClass {

    protected static void handleNeo4J(String query, Object parameters, boolean successfullyExecuted, long executionTime) {
        Neo4JRunCommand info = new Neo4JRunCommand(query, parameters, successfullyExecuted, executionTime);
        ExecutionTracer.addNeo4JInfo(info);
    }

}
