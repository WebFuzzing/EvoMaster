package org.evomaster.e2etests.utils;

import com.google.inject.Injector;
import kotlin.Unit;
import org.apache.commons.io.FileUtils;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;
import org.evomaster.core.Main;
import org.evomaster.core.StaticCounter;
import org.evomaster.core.logging.LoggingUtil;
import org.evomaster.core.output.OutputFormat;
import org.evomaster.core.output.compiler.CompilerForTestGenerated;
import org.evomaster.core.problem.rest.*;
import org.evomaster.core.remote.service.RemoteController;
import org.evomaster.core.search.Action;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Individual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        return LoggingUtil.Companion.runWithDeterministicLogger(
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
        for (int i : index) {
            String statusCode = ind.getResults().get(i).getResultValue(
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

        List<RestAction> actions = ind.getIndividual().seeActions();

        boolean stopped = false;

        for (int i = 0; i < actions.size() && !stopped; i++) {

            RestCallResult res = (RestCallResult) ind.getResults().get(i);
            stopped = res.getStopping();

            if (!(actions.get(i) instanceof RestCallAction)) {
                continue;
            }

            RestCallAction action = (RestCallAction) actions.get(i);

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

    protected void assertInsertionIntoTable(Solution<RestIndividual> solution, String tableName) {

        boolean ok = solution.getIndividuals().stream().anyMatch(
                ind -> ind.getIndividual().getDbInitialization().stream().anyMatch(
                        da -> da.getTable().getName().equalsIgnoreCase(tableName))
        );

        assertTrue(ok);
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
