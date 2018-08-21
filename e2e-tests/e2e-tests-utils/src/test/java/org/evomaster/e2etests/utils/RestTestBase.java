package org.evomaster.e2etests.utils;

import org.evomaster.clientJava.controller.EmbeddedSutController;
import org.evomaster.clientJava.controller.InstrumentedSutStarter;
import org.evomaster.clientJava.controller.internal.SutController;
import org.evomaster.clientJava.controllerApi.dto.SutInfoDto;
import org.evomaster.core.problem.rest.*;
import org.evomaster.core.remote.service.RemoteController;
import org.evomaster.core.search.Action;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Individual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class RestTestBase {

    protected static InstrumentedSutStarter embeddedStarter;
    protected static String baseUrlOfSut;
    protected static SutController controller;
    protected static RemoteController remoteController;
    protected static int controllerPort;

    protected static void initClass(EmbeddedSutController controller) throws Exception {

        RestTestBase.controller = controller;

        embeddedStarter = new InstrumentedSutStarter(controller);
        embeddedStarter.start();

        controllerPort = embeddedStarter.getControllerServerPort();

        remoteController = new RemoteController("localhost", controllerPort);
        boolean started = remoteController.startSUT();
        assertTrue(started);

        SutInfoDto dto = remoteController.getSutInfo();
        assertNotNull(dto);

        baseUrlOfSut = dto.baseUrlOfSUT;
        assertNotNull(baseUrlOfSut);

        System.out.println("Remote controller running on port " + controllerPort);
        System.out.println("SUT listening on " + baseUrlOfSut);
    }

    @AfterAll
    public static void tearDown() {

        boolean stopped = remoteController.stopSUT();
        stopped = embeddedStarter.stop() && stopped;

        assertTrue(stopped);
    }


    @BeforeEach
    public void initTest() {

        boolean reset = remoteController.resetSUT();
        assertTrue(reset);
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
                    RestCallResult.Companion.getSTATUS_CODE());
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

        List<Action> actions = ind.getIndividual().seeActions();

        for (int i = 0; i < actions.size(); i++) {

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

            RestCallResult res = (RestCallResult) ind.getResults().get(i);
            Integer statusCode = res.getStatusCode();

            if (!statusCode.equals(expectedStatusCode)) {
                continue;
            }

            String body = res.getBody();
            if (inResponse != null && !body.contains(inResponse)) {
                continue;
            }

            return true;
        }

        return false;
    }

    protected void assertHasAtLeastOne(Solution<RestIndividual> solution,
                                       HttpVerb verb,
                                       int expectedStatusCode,
                                       String path,
                                       String inResponse) {

        boolean ok = solution.getIndividuals().stream().anyMatch(
                ind -> hasAtLeastOne(ind, verb, expectedStatusCode, path, inResponse));

        assertTrue(ok, restActions(solution));
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

    private String restActions(Solution<RestIndividual> solution) {
        StringBuffer msg = new StringBuffer("REST calls:\n");

        solution.getIndividuals().stream().flatMap(ind -> ind.evaluatedActions().stream())
                .map(ea -> ea.getAction())
                .filter(a -> a instanceof RestCallAction)
                .forEach(a -> msg.append(a.toString()).append("\n"));

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

    /**
     * Unfortunately JUnit 5 does not handle flaky tests, and Maven is not upgraded yet.
     * See https://github.com/junit-team/junit5/issues/1558#issuecomment-414701182
     *
     * TODO: once that issue is fixed (if it will ever be fixed), then this method
     * will no longer be needed
     *
     * @param lambda
     * @throws Throwable
     */
    protected void handleFlaky(Runnable lambda) throws Throwable{

        int attempts = 3;
        Throwable error = null;

        for(int i=0; i<attempts; i++){

            try{
                lambda.run();
                return;
            }catch (OutOfMemoryError e){
                throw e;
            }catch (Throwable t){
                error = t;
            }
        }

        throw error;
    }
}
