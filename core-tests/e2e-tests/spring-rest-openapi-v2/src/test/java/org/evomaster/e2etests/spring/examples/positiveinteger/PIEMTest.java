package org.evomaster.e2etests.spring.examples.positiveinteger;

import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PIEMTest extends PITestBase {


    @ParameterizedTest
    @EnumSource(EMConfig.Algorithm.class)
    public void testAlgorithms(EMConfig.Algorithm alg)  throws Throwable {
        testRunEM(alg, 1000);// high value, just to check if no crash
    }



    private void testRunEM(EMConfig.Algorithm alg, int iterations) throws Throwable {

        String outputFolderName = "PIEM_" + alg.toString();
        ClassName className = new ClassName("org.PIEM_Run_" + alg);
        clearGeneratedFiles(outputFolderName, className);

        handleFlaky(() -> {
            List<String> args = getArgsWithCompilation(iterations, outputFolderName, className);

            args.add("--algorithm");
            args.add(alg.toString());
            defaultSeed++;

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
