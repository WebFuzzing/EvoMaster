package org.evomaster.e2etests.spring.examples.resource.dependency;

import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.resource.ResourceTestBase;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * created by manzh on 2019-08-12
 */
public class ResourceDependencyEMTest extends ResourceTestBase {

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "ResourceEM",
                "org.resource.ResourceEM",
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
                    args.add("EqualProbability");

                    args.add("--probOfSmartSampling");
                    args.add("1.0");
                    args.add("--doesApplyNameMatching");
                    args.add("false");

                    args.add("--probOfEnablingResourceDependencyHeuristics");
                    args.add("1.0");
                    args.add("--structureMutationProbability");
                    args.add("1.0");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assert(Files.exists(Paths.get(dependencies)));

                    boolean anyDBExecution = solution.getIndividuals().stream().anyMatch(
                            s -> s.getFitness().isAnyDatabaseExecutionInfo()
                    );
                    assertTrue(anyDBExecution);

                    boolean ok = solution.getIndividuals().stream().anyMatch(
                            s -> hasAtLeastOneSequence(s, new HttpVerb[]{HttpVerb.POST, HttpVerb.POST}, new int[]{201, 201}, new String[]{"/api/rd","/api/rpR"}) ||
                                    hasAtLeastOneSequence(s, new HttpVerb[]{HttpVerb.GET, HttpVerb.POST}, new int[]{200, 201}, new String[]{"/api/rd/{rdId}","/api/rpR"})

                    );

                    assertTrue(ok);
                }, 3);
    }
}
