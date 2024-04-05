package org.evomaster.e2etests.utils;

import kotlin.Unit;
import org.evomaster.client.java.utils.SimpleLogger;
import org.evomaster.core.Main;
import org.evomaster.core.StaticCounter;
import org.evomaster.core.search.action.Action;
import org.evomaster.core.search.action.ActionResult;
import org.evomaster.core.logging.TestLoggingUtil;
import org.evomaster.core.problem.rest.*;
import org.evomaster.core.search.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public abstract class RestTestBase  extends EnterpriseTestBase {


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
                "--useTimeInFeedbackSampling" , "false",
                "--createConfigPathIfMissing", "false"
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
        List<Action> actions = ind.seeAllActions();

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
            if (statusCode!=null && statusCode.equals("" + expectedStatusCode)) {
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

        List<RestCallAction> actions = ind.getIndividual().seeMainExecutableActions();
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


    /*
     Helper method to check if two given paths match based on focus or prefix
     path - Path we are analyzing
     pathFocusOrPrefix - Focus or prefix we are trying to check
     focusMode - true for checking focus, false for checking prefix
     */
    private boolean pathsMatchFocusOrPrefix(RestPath pathToAnalyze, RestPath pathFocusOrPrefix, boolean focusMode)
    {
        // check that pathToAnalyze and pathFocusOrPrefix are both not NULL
        if(pathToAnalyze == null || pathFocusOrPrefix == null) {
            throw new IllegalArgumentException("Invalid parameter(s), check that the given path parameters are" +
                                               "both not NUL");
        }
        // if mode is focus, path and pathFocusOrPrefix match only if they are the same
        if (focusMode) {
            return pathToAnalyze.isEquivalent(pathFocusOrPrefix);
        }
        // focusMode is false, which means prefix match
        else {
            // if the path we are analyzing starts with pathFocusOrPrefix
            return pathToAnalyze.toString().startsWith(pathFocusOrPrefix.toString());
        }
    }

    /* Check if a given individual has either of the given paths as the focus or
       as the prefix
     */
    protected boolean hasFocusOrPrefixInPath(EvaluatedIndividual<RestIndividual> ind,
                                             List<RestPath> paths, boolean focusMode) {
        // if no paths are provided, none of the paths are focus of emoty path
        // every path contains empty path as a prefix
        if (paths == null || ind == null) {
            throw new IllegalArgumentException("Invalid parameters provided to method, one or both of them is " +
                    "NULL: ind or paths");
        }
        // if no paths are provided, none of the paths are focus of empty path
        // every path contains empty path as a prefix
        else if (paths.isEmpty()) {
            // in focusMode, none of the paths match with the empty path
            // in prefix mode, every path matches with the empty path
            if (focusMode == true) {
                return false;
            }
            else {
                return true;
            }
        }
        else {
            // actions and noMatchFlag
            List<RestCallAction> actions = ind.getIndividual().seeMainExecutableActions();
            boolean noMatchFlag = false;

            for (int i = 0; i < actions.size() && !noMatchFlag; i++) {

                RestCallAction action = actions.get(i);

                // if none of them match in one solution, noMatchFlag is true
                if (paths.stream().noneMatch(currentPath -> pathsMatchFocusOrPrefix(action.getPath(),
                        currentPath, focusMode))) {
                    noMatchFlag = true;
                }
            }
            // if a patch which does not match has been encountered, return false
            if (noMatchFlag == true) {
                return false;
            }
            else {
                return true;
            }
        }
    }

    /*
    All solutions should have one of the provided paths as the focus or the prefix
     */
    protected void assertAllSolutionsHavePathFocusOrPrefixList(Solution<RestIndividual> solution,
                                                               List<String> paths, boolean focusMode) {

        // convert String list of paths to list of RestPaths
        List<RestPath> listOfRestPaths = new ArrayList<>();

        // convert list of Strings to a list of RestPaths
        for (String path : paths) {
            listOfRestPaths.add(new RestPath(path));
        }

        // check that all paths have any of given paths as the focus or prefix.
        boolean ok = solution.getIndividuals().stream().allMatch(
                ind -> hasFocusOrPrefixInPath(ind, listOfRestPaths, focusMode) );

        // error message
        String errorMsg = "Not all the provided paths are contained in the solution as" +
                           " the focus or prefix\n";
        errorMsg = errorMsg + "List of paths given to check:\n";

        // display paths in the error message
        for (String path : paths) {
            errorMsg = MessageFormat.format("{0}{1}\n", errorMsg, path);
        }

        // display list of paths included in the solution in the error message
        errorMsg = errorMsg + "List of paths included in the solution\n";
        errorMsg = errorMsg + restActions(solution);

        // assertion
        assertTrue(ok, errorMsg + restActions(solution));
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

        solution.getIndividuals().stream().flatMap(ind -> ind.evaluatedMainActions().stream())
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

        assertOK(solution, ok);
    }

    private static void assertOK(Solution<RestIndividual> solution, boolean ok) {
        StringBuffer msg = new StringBuffer("REST calls:\n");
        if (!ok) {
            solution.getIndividuals().stream().flatMap(ind -> ind.evaluatedMainActions().stream())
                    .map(ea -> ea.getAction())
                    .filter(a -> a instanceof RestCallAction)
                    .forEach(a -> msg.append(a.toString() + "\n"));
        }

        assertTrue(ok, msg.toString());
    }

    protected void assertNone(Solution<RestIndividual> solution,
                              HttpVerb verb,
                              int expectedStatusCode,
                              String path,
                              String inResponse){

        boolean ok = solution.getIndividuals().stream().noneMatch(
                ind -> hasAtLeastOne(ind, verb, expectedStatusCode, path, inResponse));

        assertOK(solution, ok);
    }

}
