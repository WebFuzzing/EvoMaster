package org.evomaster.e2etests.spring.examples.resource;

import com.foo.rest.examples.spring.resource.ResourceRestController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

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
                50,
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
                    args.add("false");
                    args.add("--generateSqlDataWithSearch");
                    args.add("false");
                    args.add("--extractSqlExecutionInfo");
                    args.add("true");

                    args.add("--maxTestSize");
                    args.add("4");

                    args.add("--exportDependencies");
                    args.add("true");

                    String dependencies = "target/dependencyInfo/dependencies.csv";

                    args.add("--dependencyFile");
                    args.add(dependencies);

                    args.add("--resourceSampleStrategy");
                    args.add("ConArchive");

                    args.add("--probOfSmartSampling");
                    args.add("1.0");
                    args.add("--doesApplyNameMatching");
                    args.add("true");

                    args.add("--probOfEnablingResourceDependencyHeuristics");
                    args.add("1.0");
//                    args.add("--structureMutationProbability");
//                    args.add("1.0");

                    //enable sql to create resources
                    args.add("--probOfApplySQLActionToCreateResources");
                    args.add("0.9");


                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assert(Files.exists(Paths.get(dependencies)));

                    boolean anyDBExecution = solution.getIndividuals().stream().anyMatch(
                            s -> s.getFitness().isAnyDatabaseExecutionInfo()
                    );
                    assertTrue(anyDBExecution);

                    boolean ok = solution.getIndividuals().stream().anyMatch(
                            s -> hasAtLeastOneSequence(s, new HttpVerb[]{HttpVerb.GET}, new int[]{200}, new String[]{"/api/rd/{rdId}"})
                    );

                    assertTrue(ok);
                }, 5);
    }

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ResourceRestController(Arrays.asList("/api/rd","/api/rA","/api/rA/{rAId}","/api/rpR","/api/rpR/{rpRId}")));
    }

}
