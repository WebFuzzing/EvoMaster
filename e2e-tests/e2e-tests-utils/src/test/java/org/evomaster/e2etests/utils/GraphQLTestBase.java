package org.evomaster.e2etests.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.evomaster.core.Main;
import org.evomaster.core.problem.graphql.GQMethodType;
import org.evomaster.core.problem.graphql.GraphQLAction;
import org.evomaster.core.problem.graphql.GraphQLIndividual;
import org.evomaster.core.problem.graphql.GraphQlCallResult;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Solution;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class GraphQLTestBase extends EnterpriseTestBase {

    protected Solution<GraphQLIndividual> initAndRun(List<String> args) {
        return (Solution<GraphQLIndividual>) Main.initAndRun(args.toArray(new String[0]));
    }

    protected boolean atLeastOneResponseWithData(EvaluatedIndividual<GraphQLIndividual> ind) {

        List<GraphQLAction> actions = ind.getIndividual().seeMainExecutableActions();

        boolean stopped = false;

        for (int i = 0; i < actions.size() && !stopped; i++) {

            GraphQlCallResult res = (GraphQlCallResult) ind.seeResults(actions).get(i);
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

            if (!data.isNull() && !data.isMissingNode()
                    && (errors.isNull() || errors.isMissingNode() || !errors.elements().hasNext())) {
                return true;
            }
        }

        return false;
    }

    protected boolean noneWithErrors(EvaluatedIndividual<GraphQLIndividual> ind) {

        List<GraphQLAction> actions = ind.getIndividual().seeMainExecutableActions();

        boolean stopped = false;

        for (int i = 0; i < actions.size() && !stopped; i++) {

            GraphQlCallResult res = (GraphQlCallResult) ind.seeResults(actions).get(i);
            stopped = res.getStopping();

            Integer statusCode = res.getStatusCode();
            if (!statusCode.equals(200)) {
                return false;
            }

            String body = res.getBody();
            ObjectMapper jackson = new ObjectMapper();

            JsonNode node;
            try {
                node = jackson.readTree(body);
            } catch (JsonProcessingException e) {
                continue;
            }

            JsonNode errors = node.findPath("errors");

          /*  if (!errors.isNull() || !errors.isMissingNode()) {
                return false;
            }*/
            if (!errors.isEmpty() || !errors.isMissingNode()) {
                return false;
            }
        }

        return true;
    }


    protected void assertHasAtLeastOneResponseWithData(Solution<GraphQLIndividual> solution) {
        boolean ok = solution.getIndividuals().stream().anyMatch(ind -> atLeastOneResponseWithData(ind));
        assertTrue(ok);
    }

    protected void assertNoneWithErrors(Solution<GraphQLIndividual> solution) {
        boolean ok = solution.getIndividuals().stream().allMatch(ind -> noneWithErrors(ind));
        String errorMsg = "Seed " + (defaultSeed-1)+". There exist some errors\n";
        assertTrue(ok, errorMsg + graphActions(solution));
    }

    protected void assertAnyWithErrors(Solution<GraphQLIndividual> solution) {
        boolean ok = solution.getIndividuals().stream().anyMatch(ind -> !noneWithErrors(ind));
        assertTrue(ok);
    }

    protected boolean hasValueInData(EvaluatedIndividual<GraphQLIndividual> ind, String value) {
        List<GraphQLAction> actions = ind.getIndividual().seeMainExecutableActions();

        boolean stopped = false;

        for (int i = 0; i < actions.size() && !stopped; i++) {

            GraphQlCallResult res = (GraphQlCallResult) ind.seeResults(actions).get(i);
            stopped = res.getStopping();

            if (hasValueInData(res, value)){
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param result is the GraphQL action result to be checked
     * @param propertyName is a property of the results to be extracted
     * @return extracted content based on propertyName
     */
    private JsonNode getDataInGraphQLResults(GraphQlCallResult result, String propertyName){
        Integer statusCode = result.getStatusCode();

        if (!statusCode.equals(200)) {
            return null;
        }

        String body = result.getBody();
        ObjectMapper jackson = new ObjectMapper();

        JsonNode node;
        try {
            node = jackson.readTree(body);
        } catch (JsonProcessingException e) {
            return null;
        }

        return node.findPath(propertyName);
    }

    private boolean hasValueInData(GraphQlCallResult result, String value){

        JsonNode data = getDataInGraphQLResults(result, "data");
        if (data == null) return false;

        /*
        if (!data.isNull() && !data.isMissingNode() && data.asText().contains(value)) {
            return true;
        }*/

        if (!data.isNull() && !data.isMissingNode() && data.toString().contains(value)) {
            return true;
        }
        return false;
    }


    protected void assertValueInDataAtLeastOnce(Solution<GraphQLIndividual> solution, String value) {
        boolean ok = solution.getIndividuals().stream().anyMatch(ind -> hasValueInData(ind, value));
        assertTrue(ok);
    }

    protected void assertHasAtLeastOne(Solution<GraphQLIndividual> solution, String methodName, GQMethodType type, int expectedStatusCode, String inResponse){

        boolean ok = solution.getIndividuals().stream().anyMatch(
                ind -> hasAtLeastOne(ind, methodName, type, expectedStatusCode, inResponse));

        String errorMsg = "Seed " + (defaultSeed-1)+". ";
        errorMsg += "Missing " + expectedStatusCode + " " + type + " " + methodName + " " + inResponse + "\n";

        assertTrue(ok, errorMsg + graphActions(solution));
    }

    protected void assertHasAtLeastOne(Solution<GraphQLIndividual> solution,
                                       String methodName, GQMethodType type, int expectedStatusCode,
                                       List<String> inResponse, boolean and){
        boolean ok;
        if (and){
            ok = inResponse.stream().allMatch(s-> solution.getIndividuals().stream().anyMatch(ind ->
                    hasAtLeastOne(ind, methodName, type, expectedStatusCode, s)));
        }else{
            ok = inResponse.stream().anyMatch(s-> solution.getIndividuals().stream().anyMatch(ind ->
                    hasAtLeastOne(ind, methodName, type, expectedStatusCode, s)));
        }

        String errorMsg = "Seed " + (defaultSeed-1)+". ";
        errorMsg += "Missing " + expectedStatusCode + " " + type + " " + methodName + " " + (and?" all of " : " any of ") +String.join(",", inResponse) + "\n";

        assertTrue(ok, errorMsg + graphActions(solution));
    }



    private boolean hasAtLeastOne(EvaluatedIndividual<GraphQLIndividual> ind, String methodName, GQMethodType type, int expectedStatusCode, String inResponse){

        if (ind.getIndividual().seeAllActions().size() != ind.seeResults(null).size()){
            throw new IllegalStateException(String.format("mismatched size of results (%d) with calls (%d) for GraphQLIndividual",
                    ind.seeResults(null).size(), ind.getIndividual().seeAllActions().size()));
        }
        List<GraphQLAction> actions = ind.getIndividual().seeMainExecutableActions();

        boolean stopped = false;

        for (int i = 0; i < actions.size() && !stopped; i++) {

            GraphQlCallResult res = (GraphQlCallResult) ind.seeResults(actions).get(i);
            stopped = res.getStopping();

            boolean matched = actions.get(i).getMethodName().equals(methodName) &&
                    actions.get(i).getMethodType().equals(type) && res.getStatusCode() == expectedStatusCode;

            if(!matched) continue;

            if (inResponse == null) continue;

            if (hasValueInData(res, inResponse)) return true;
        }

        return false;

    }

    protected String graphActions(Solution<GraphQLIndividual> solution) {
        StringBuffer msg = new StringBuffer("Graph calls:\n");

        solution.getIndividuals().stream().flatMap(ind -> ind.evaluatedMainActions().stream())
                .filter(ea -> ea.getAction() instanceof GraphQLAction)
                .map(ea -> {
                    GraphQlCallResult res = (GraphQlCallResult)ea.getResult();
                    String s = res.getStatusCode() + " ";
                    JsonNode node = getDataInGraphQLResults(res, "data");
                    if (node != null) s += " data:"+node.toString();
                    node = getDataInGraphQLResults(res, "errors");
                    if (node != null && (!node.isEmpty() || !node.isMissingNode())) s += " errors:"+node.toString();
                    s += ea.getAction().toString() + "\n";
                    return s;
                })
                .sorted()
                .forEach(s -> msg.append(s));
        ;

        return msg.toString();
    }


}
