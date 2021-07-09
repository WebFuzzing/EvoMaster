package org.evomaster.e2etests.utils;

import kotlin.Unit;
import org.evomaster.client.java.utils.SimpleLogger;
import org.evomaster.core.Main;
import org.evomaster.core.StaticCounter;
import org.evomaster.core.logging.TestLoggingUtil;
import org.evomaster.core.problem.rest.*;
import org.evomaster.core.search.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public abstract class RestTestBase  extends WsTestBase{


    protected Solution<RestIndividual> initAndRun(List<String> args){
        return (Solution<RestIndividual>) Main.initAndRun(args.toArray(new String[0]));
    }

    protected void runAndCheckDeterminism(int iterations, Consumer<List<String>> lambda){
        runAndCheckDeterminism(iterations, lambda, 2, false);
    }

    protected String runAndCheckDeterminism(int iterations, Consumer<List<String>> lambda, int times, boolean notDeterminism){

        /*
            As some HTTP verbs are idempotent, they could be repeated... and we have no control whatsoever on it :(
            so, for these deterministic checks, we disable the loggers in the driver
         */
        SimpleLogger.setThreshold(SimpleLogger.Level.OFF);

        List<String> args =  new ArrayList<>(Arrays.asList(
                "--createTests", "false",
                "--seed", "42",
                "--showProgress", "false",
                "--avoidNonDeterministicLogs", "true",
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "" + iterations,
                "--stoppingCriterion", "FITNESS_EVALUATIONS",
                "--useTimeInFeedbackSampling" , "false"
        ));

        return isDeterminismConsumer(args, lambda, times, notDeterminism);
    }
    protected String isDeterminismConsumer(List<String> args, Consumer<List<String>> lambda) {
        return isDeterminismConsumer(args, lambda, 2, false);
    }

    protected String isDeterminismConsumer(List<String> args, Consumer<List<String>> lambda, int times, boolean notEqual) {
        assert(times >= 2);

        String firstRun = consumerToString(args, lambda);

        int c = 1;
        while (c < times){
            String secondRun = consumerToString(args, lambda);
            if (notEqual)
                assertNotEquals(firstRun, secondRun);
            else
                assertEquals(firstRun, secondRun);
            firstRun = secondRun;
            c++;
        }
        return firstRun;
    }

    protected String consumerToString(List<String> args, Consumer<List<String>> lambda){
        StaticCounter.Companion.reset();
        return TestLoggingUtil.Companion.runWithDeterministicLogger(
                () -> {lambda.accept(args); return Unit.INSTANCE;}
        );
    }

    protected List<Integer> getIndexOfHttpCalls(Individual ind, HttpVerb verb) {

        List<Integer> indices = new ArrayList<>();
        List<Action> actions = ind.seeActions();

        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i) instanceof RestCallAction) {
                RestCallAction action = (RestCallAction) actions.get(i);
                if (action.getVerb() == verb) {
                    indices.add(i);
                }
            }
        }

        return indices;
    }


    protected boolean hasAtLeastOne(EvaluatedIndividual<RestIndividual> ind,
                                    HttpVerb verb,
                                    int expectedStatusCode) {

        List<Integer> index = getIndexOfHttpCalls(ind.getIndividual(), verb);
        List<ActionResult> results = ind.seeResults(null);
        for (int i : index) {
            String statusCode = results.get(i).getResultValue(
                    RestCallResult.STATUS_CODE);
            if (statusCode.equals("" + expectedStatusCode)) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasAtLeastOne(EvaluatedIndividual<RestIndividual> ind,
                                    HttpVerb verb,
                                    int expectedStatusCode,
                                    String path,
                                    String inResponse) {

        List<RestCallAction> actions = ind.getIndividual().seeActions();
        List<ActionResult> results = ind.seeResults(actions);

        boolean stopped = false;

        for (int i = 0; i < actions.size() && !stopped; i++) {

            RestCallResult res = (RestCallResult) results.get(i);
            stopped = res.getStopping();

            RestCallAction action = actions.get(i);

            if (action.getVerb() != verb) {
                continue;
            }

            if (path != null) {
                RestPath target = new RestPath(path);
                if (!action.getPath().isEquivalent(target)) {
                    continue;
                }
            }



            Integer statusCode = res.getStatusCode();

            if (!statusCode.equals(expectedStatusCode)) {
                continue;
            }

            String body = res.getBody();
            if (inResponse != null && (body==null ||  !body.contains(inResponse))) {
                continue;
            }

            return true;
        }

        return false;
    }

    protected int countExpected(Solution<RestIndividual> solution,
                                       HttpVerb verb,
                                       int expectedStatusCode,
                                       String path,
                                       String inResponse, int count, List<String> msg) {

        boolean ok = solution.getIndividuals().stream().anyMatch(
                ind -> hasAtLeastOne(ind, verb, expectedStatusCode, path, inResponse));
        if (!ok){
            msg.add("Seed " + (defaultSeed-1)+". ");
            msg.add("Missing " + expectedStatusCode + " " + verb + " " + path + " " + inResponse + "\n");
        }

        return ok? count+1: count;
    }

    protected void assertHasAtLeastOne(Solution<RestIndividual> solution,
                                       HttpVerb verb,
                                       int expectedStatusCode,
                                       String path,
                                       String inResponse) {

        boolean ok = solution.getIndividuals().stream().anyMatch(
                ind -> hasAtLeastOne(ind, verb, expectedStatusCode, path, inResponse));

        String errorMsg = "Seed " + (defaultSeed-1)+". ";
        errorMsg += "Missing " + expectedStatusCode + " " + verb + " " + path + " " + inResponse + "\n";

        assertTrue(ok, errorMsg + restActions(solution));
    }

    protected void assertHasAtLeastOne(Solution<RestIndividual> solution,
                                       HttpVerb verb,
                                       int expectedStatusCode) {
        assertHasAtLeastOne(solution, verb, expectedStatusCode, null, null);
    }

    protected String restActions(Solution<RestIndividual> solution) {
        StringBuffer msg = new StringBuffer("REST calls:\n");

        solution.getIndividuals().stream().flatMap(ind -> ind.evaluatedActions().stream())
                .filter(ea -> ea.getAction() instanceof RestCallAction)
                .map(ea -> {
                    String s = ((RestCallResult)ea.getResult()).getStatusCode() + " ";
                    s += ea.getAction().toString() + "\n";
                    return s;
                })
                .sorted()
                .forEach(s -> msg.append(s));
        ;

        return msg.toString();
    }

    protected void assertNone(Solution<RestIndividual> solution,
                              HttpVerb verb,
                              int expectedStatusCode) {

        boolean ok = solution.getIndividuals().stream().noneMatch(
                ind -> hasAtLeastOne(ind, verb, expectedStatusCode));

        StringBuffer msg = new StringBuffer("REST calls:\n");
        if (!ok) {
            solution.getIndividuals().stream().flatMap(ind -> ind.evaluatedActions().stream())
                    .map(ea -> ea.getAction())
                    .filter(a -> a instanceof RestCallAction)
                    .forEach(a -> msg.append(a.toString() + "\n"));
        }

        assertTrue(ok, msg.toString());
    }


}
