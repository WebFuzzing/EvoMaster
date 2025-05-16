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
                    setOption(args, "baseTaintAnalysisProbability","0.0");

                    //disable hypermutation
                    setOption(args, "enableTrackEvaluatedIndividual","false");
                    setOption(args, "weightBasedMutationRate","false");
                    setOption(args, "adaptiveGeneSelectionMethod","NONE");
                    setOption(args, "archiveGeneMutation","NONE");
                    setOption(args, "probOfArchiveMutation","0.0");

                    //disable SQL
                    setOption(args, "heuristicsForSQL","false");
                    setOption(args, "generateSqlDataWithSearch","false");
                    setOption(args, "extractSqlExecutionInfo","true");

                    setOption(args, "maxTestSize","4");

                    setOption(args, "exportDependencies","true");

                    String dependencies = "target/dependencyInfo/dependencies.csv";

                    setOption(args, "dependencyFile", dependencies);

                    setOption(args, "resourceSampleStrategy","EqualProbability");

                    setOption(args, "probOfSmartSampling","1.0");
                    setOption(args, "doesApplyNameMatching","false");

                    setOption(args, "probOfEnablingResourceDependencyHeuristics","1.0");
                    setOption(args, "structureMutationProbability","1.0");

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
