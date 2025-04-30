package org.evomaster.e2etests.spring.examples.resource.db;

import com.foo.rest.examples.spring.resource.ResourceRestController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.evomaster.e2etests.spring.examples.resource.ResourceTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * created by manzh on 2019-08-12
 */
public class ResourceDependencyDBEMTest extends ResourceTestBase {

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "ResourceEM",
                "org.db.resource.ResourceEM",
                1_000,
                true,
                (args) -> {
                    // disable taint analysis
                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.0");

                    //disable hypermutation
                    args.add("--enableTrackEvaluatedIndividual");
                    args.add("false");
                    args.add("--weightBasedMutationRate");
                    args.add("false");
                    args.add("--adaptiveGeneSelectionMethod");
                    args.add("NONE");
                    args.add("--archiveGeneMutation");
                    args.add("NONE");
                    args.add("--probOfArchiveMutation");
                    args.add("0.0");

                    //disable SQL
                    args.add("--heuristicsForSQL");
                    args.add("true");
                    args.add("--generateSqlDataWithSearch");
                    args.add("true");
                    args.add("--extractSqlExecutionInfo");
                    args.add("true");

                    args.add("--maxTestSize");
                    args.add("4");

                    args.add("--exportDependencies");
                    args.add("true");

                    String dependencies = "target/dependencyInfo/dependencies.csv";

                    args.add("--dependencyFile");
                    args.add(dependencies);

                    args.add("--probOfSmartSampling");
                    args.add("1.0");
                    args.add("--doesApplyNameMatching");
                    args.add("true");

                    args.add("--probOfEnablingResourceDependencyHeuristics");
                    args.add("1.0");
                    args.add("--structureMutationProbability");
                    args.add("1.0");

                    args.add("--probOfApplySQLActionToCreateResources");
                    args.add("0.8");

                    args.add("--skipFailureSQLInTestFile");
                    args.add("true");

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
        SpringTestBase.initClass(new ResourceRestController(Collections.singletonList("/api/rd")));
    }

}
