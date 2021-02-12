package org.evomaster.e2etests.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.evomaster.core.Main;
import org.evomaster.core.problem.graphql.GraphQLAction;
import org.evomaster.core.problem.graphql.GraphQLIndividual;
import org.evomaster.core.problem.graphql.GraphQlCallResult;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestCallAction;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.problem.rest.RestPath;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Solution;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class GraphQLTestBase extends WsTestBase{

    protected Solution<GraphQLIndividual> initAndRun(List<String> args){
        return (Solution<GraphQLIndividual>) Main.initAndRun(args.toArray(new String[0]));
    }

    protected boolean atLeastOneResponseWithData(EvaluatedIndividual<GraphQLIndividual> ind){

        List<GraphQLAction> actions = ind.getIndividual().seeActions();

        boolean stopped = false;

        for (int i = 0; i < actions.size() && !stopped; i++) {

            GraphQlCallResult res = (GraphQlCallResult) ind.getResults().get(i);
            stopped = res.getStopping();

            Integer statusCode = res.getStatusCode();
            if (!statusCode.equals(200)) {
                continue;
            }

            String body = res.getBody();
            ObjectMapper jackson = new ObjectMapper();

            JsonNode node;
            try {
                node = jackson.readTree(body);
            } catch (JsonProcessingException e) {
                continue;
            }

            JsonNode data = node.findPath("data");
            JsonNode errors = node.findPath("errors");

            if(!data.isNull() && !data.isMissingNode()
                    && (errors.isNull() || errors.isMissingNode() || !errors.elements().hasNext())){
                return true;
            }
        }

        return false;
    }

    protected void assertHasAtLeastOneResponseWithData(Solution<GraphQLIndividual> solution) {
        boolean ok = solution.getIndividuals().stream().anyMatch(ind -> atLeastOneResponseWithData(ind));;
        assertTrue(ok);
    }
}
