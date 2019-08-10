package org.evomaster.e2etests.graphql.examples.petshop;

import org.evomaster.client.java.instrumentation.ClassName;
import org.evomaster.core.problem.graphql.GraphqlIndividual;
import org.junit.jupiter.api.Test;
import org.evomaster.core.EMConfig;
import java.util.List;
import org.evomaster.core.search.Solution;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PetShopEMTest extends PetShopTestBase {
    @Test
    public void testMIO() throws Throwable {
        testRunEM(EMConfig.Algorithm.MIO, 1000);
    }

    private void testRunEM(EMConfig.Algorithm alg, int iterations) throws Throwable {

        String outputFolderName = "PIEM_" + alg.toString();
        ClassName className = new ClassName("org.PIEM_Run_" + alg.toString());
        clearGeneratedFiles(outputFolderName, className);

        handleFlaky(() -> {
            List<String> args = getArgsWithCompilation(iterations, outputFolderName, className);
            args.add("--algorithm");
            args.add(alg.toString());

            Solution<GraphqlIndividual> solution = initAndRun(args);

            assertTrue(solution.getIndividuals().size() >= 1);

        });

        compileRunAndVerifyTests(outputFolderName, className);
    }
}
