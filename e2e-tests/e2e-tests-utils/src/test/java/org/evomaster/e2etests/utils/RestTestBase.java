package org.evomaster.e2etests.utils;

import org.evomaster.clientJava.controller.EmbeddedStarter;
import org.evomaster.clientJava.controller.SutController;
import org.evomaster.clientJava.controllerApi.dto.SutInfoDto;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestCallAction;
import org.evomaster.core.problem.rest.RestCallResult;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.problem.rest.service.RemoteController;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class RestTestBase {

    protected static EmbeddedStarter embeddedStarter;
    protected static String baseUrlOfSut;
    protected static RemoteController remoteController;
    protected static int controllerPort;

    protected static void initClass(SutController controller) throws Exception {

        embeddedStarter = new EmbeddedStarter(controller);
        embeddedStarter.start();

        controllerPort = embeddedStarter.getControllerServerJettyPort();

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


    protected boolean hasAtLeastOne(EvaluatedIndividual<RestIndividual> ind,
                                    HttpVerb verb,
                                    int expectedStatusCode) {

        List<Integer> index = ind.getIndividual().getIndexOfHttpCalls(verb);
        for (int i : index) {
            String statusCode = ind.getResults().get(i).getResultValue(
                    RestCallResult.Companion.getSTATUS_CODE());
            if (statusCode.equals("" + expectedStatusCode)) {
                return true;
            }
        }
        return false;
    }


    protected void assertHasAtLeastOne(Solution<RestIndividual> solution,
                                       HttpVerb verb,
                                       int expectedStatusCode) {

        boolean ok = solution.getIndividuals().stream().anyMatch(
                ind -> hasAtLeastOne(ind, verb, expectedStatusCode));

        StringBuffer msg = new StringBuffer("REST calls:\n");
        if(!ok){
            solution.getIndividuals().stream().flatMap(ind -> ind.evaluatedActions().stream())
                    .map(ea -> ea.getAction())
                    .filter(a -> a instanceof RestCallAction)
                    .forEach(a -> msg.append(a.toString() + "\n"));
        }

        assertTrue(ok, msg.toString());
    }

    protected void assertNone(Solution<RestIndividual> solution,
                              HttpVerb verb,
                              int expectedStatusCode) {

        boolean ok = solution.getIndividuals().stream().noneMatch(
                ind -> hasAtLeastOne(ind, verb, expectedStatusCode));

        StringBuffer msg = new StringBuffer("REST calls:\n");
        if(!ok){
            solution.getIndividuals().stream().flatMap(ind -> ind.evaluatedActions().stream())
                    .map(ea -> ea.getAction())
                    .filter(a -> a instanceof RestCallAction)
                    .forEach(a -> msg.append(a.toString() + "\n"));
        }

        assertTrue(ok, msg.toString());
    }
}
