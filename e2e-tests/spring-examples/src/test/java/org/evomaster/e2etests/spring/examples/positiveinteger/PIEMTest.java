package org.evomaster.e2etests.spring.examples.positiveinteger;

import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

import java.util.List;

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

        String outputFolderName = "PIEM_" + alg.toString();
        ClassName className = new ClassName("org.PIEM_Run_" + alg.toString());
        clearGeneratedFiles(outputFolderName, className);

        handleFlaky(() -> {
            List<String> args = getArgsWithCompilation(iterations, outputFolderName, className);
            args.add("--algorithm");
            args.add(alg.toString());

            Solution<RestIndividual> solution = initAndRun(args);

            assertTrue(solution.getIndividuals().size() >= 1);

            assertHasAtLeastOne(solution, HttpVerb.GET, 200);
            assertHasAtLeastOne(solution, HttpVerb.POST, 200);
        });

        compileRunAndVerifyTests(outputFolderName, className);
    }


    @Test
    public void testCreateTest() {

        String outputFolderName = "PIEM";
        ClassName className = new ClassName("org.PIEM_Create");
        clearGeneratedFiles(outputFolderName, className);


        List<String> args = getArgsWithCompilation(20, outputFolderName, className);
        Solution<RestIndividual> solution = initAndRun(args);

        assertHasAtLeastOne(solution, HttpVerb.GET, 200);

        compileRunAndVerifyTests(outputFolderName, className);
    }
}
