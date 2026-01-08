package org.evomaster.e2etests.spring.examples.resource.db;

import com.foo.rest.examples.spring.resource.ResourceRestController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.evomaster.e2etests.spring.examples.resource.ResourceTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * created by manzh on 2019-08-12
 */
public class ResourceDependencyDBEMTest extends ResourceTestBase {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testRunEM(boolean heuristicsForSQLAdvanced) throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "ResourceDBEM",
                "org.db.resource.ResourceEM" + (heuristicsForSQLAdvanced ? "Complete" : "Partial"),
                1_000,
                true,
                (args) -> {
                    // disable taint analysis
                    setOption(args,"baseTaintAnalysisProbability","0.0");

                    //disable hypermutation
                    setOption(args, "enableTrackEvaluatedIndividual", "false");
                    setOption(args, "weightBasedMutationRate", "false");
                    setOption(args, "adaptiveGeneSelectionMethod", "NONE");
                    setOption(args, "archiveGeneMutation", "NONE");
                    setOption(args, "probOfArchiveMutation", "0.0");

                    //enable SQL
                    setOption(args, "heuristicsForSQL", "true");
                    setOption(args, "generateSqlDataWithSearch", "true");
                    setOption(args,"extractSqlExecutionInfo","true");
                    setOption(args, "heuristicsForSQLAdvanced", heuristicsForSQLAdvanced ? "true" : "false");

                    setOption(args,"maxTestSize", "4");


                    setOption(args,"exportDependencies","true");

                    String dependencies = "target/dependencyInfo/dependencies.csv";

                    setOption(args,"dependencyFile", dependencies);

                    setOption(args,"probOfSmartSampling","1.0");

                    setOption(args,"doesApplyNameMatching","true");

                    setOption(args, "probOfEnablingResourceDependencyHeuristics", "1.0");

                    setOption(args, "structureMutationProbability","1.0");

                    setOption(args,"probOfApplySQLActionToCreateResources", "0.8");

                    setOption(args, "skipFailureSQLInTestFile", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assert(Files.exists(Paths.get(dependencies)));

                    boolean anyDBExecution = solution.getIndividuals().stream().anyMatch(
                            s -> s.getFitness().isAnyDatabaseExecutionInfo()
                    );
                    assertTrue(anyDBExecution);

                    boolean ok = solution.getIndividuals().stream().anyMatch(
                            s -> hasAtLeastOneSequence(s, new HttpVerb[]{HttpVerb.POST}, new int[]{201}, new String[]{"/api/rpR"}) ||
                                    hasAtLeastOneSequence(s, new HttpVerb[]{HttpVerb.GET, HttpVerb.POST}, new int[]{200, 201}, new String[]{"/api/rd/{rdId}","/api/rpR"})
                    );

                    assertTrue(ok);
                }, 3);
    }

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ResourceRestController(Arrays.asList("/api/rd")));
    }

}
