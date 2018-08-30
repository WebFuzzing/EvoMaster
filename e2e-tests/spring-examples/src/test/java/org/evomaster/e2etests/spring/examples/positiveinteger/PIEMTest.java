package org.evomaster.e2etests.spring.examples.positiveinteger;

import org.evomaster.core.EMConfig;
import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PIEMTest extends PITestBase {



    @Test
    public void testMIO() throws Throwable {
        testRunEM(EMConfig.Algorithm.MIO, 1000);
    }

    @Test
    public void testRand() throws Throwable {
        testRunEM(EMConfig.Algorithm.RANDOM, 20);
    }

    @Test
    public void testWTS() throws Throwable {
        testRunEM(EMConfig.Algorithm.WTS, 2_000); // high value, just to check if no crash
    }

    @Test
    public void testMOSA() throws Throwable {
        testRunEM(EMConfig.Algorithm.MOSA, 2_000); // high value, just to check if no crash
    }


    private void testRunEM(EMConfig.Algorithm alg, int iterations) throws Throwable {

        handleFlaky(() -> {
            String[] args = new String[]{
                    "--createTests", "false",
                    "--seed", "42",
                    "--sutControllerPort", "" + controllerPort,
                    "--maxActionEvaluations", "" + iterations,
                    "--stoppingCriterion", "FITNESS_EVALUATIONS",
                    "--algorithm", alg.toString()
            };

            Solution<RestIndividual> solution = (Solution<RestIndividual>) Main.initAndRun(args);

            assertTrue(solution.getIndividuals().size() >= 1);

            assertHasAtLeastOne(solution, HttpVerb.GET, 200);
            assertHasAtLeastOne(solution, HttpVerb.POST, 200);
        });
    }


    @Test
    public void testCreateTest() {

        String[] args = new String[]{
                "--createTests", "true",
                "--seed", "42",
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "20",
                "--stoppingCriterion", "FITNESS_EVALUATIONS"
        };

       Main.initAndRun(args);

       //TODO check file
    }
}
