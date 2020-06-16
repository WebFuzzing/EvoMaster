package org.evomaster.e2etests.spring.examples.impactXYZ;

import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.core.search.impact.impactInfoCollection.GeneMutationSelectionMethod;
import org.junit.jupiter.api.Test;


public class ArchiveGeneMutationImpactXYZTest extends ImpactXYZTestBase {

    private final String folder = "AGM-ImpactXYZ";

    @Test
    public void testOnlyArchiveMutation() throws Throwable {
        testRunEM(GeneMutationSelectionMethod.NONE);
    }

    public void testRunEM(GeneMutationSelectionMethod method) throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "none",
                "none",
                1000,
                false,
                (args) -> {

                    args.add("--probOfArchiveMutation");
                    args.add("1.0");

                    args.add("--adaptiveGeneSelectionMethod");
                    args.add(method.toString());

                    args.add("--archiveGeneMutation");
                    args.add("SPECIFIED");

                    //since there only exist one endpoint, we set the population for each target 3
                    args.add("--archiveTargetLimit");
                    args.add("3");

                    args.add("--enableTrackEvaluatedIndividual");
                    args.add("true");

                    args.add("--focusedSearchActivationTime");
                    args.add("0.0");

                    // only for the test
                    args.add("--saveImpactAfterMutationFile");
                    args.add("target/"+folder+"/impactInfo/Impacts_ImpactXYZ_"+method.toString()+".csv");

                    // only for the test
                    args.add("--saveMutatedGeneFile");
                    args.add("target/"+folder+"/mutatedGeneInfo/MutatedGenes_ImpactXYZ_"+method.toString()+".csv");

                    // only for the test
                    args.add("--saveArchiveAfterMutationFile");
                    args.add("target/"+folder+"/archiveInfo/ArchiveNotCoveredSnapshot_ImpactXYZ_"+method.toString()+".csv");

                    args.add("--exportCoveredTarget");
                    args.add("true");

                    args.add("--coveredTargetFile");
                    String path = "target/"+folder+"/coveredTargets/testImpacts_CoveredTargetsBy"+method.toString()+".csv";
                    args.add(path);


                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 500, "/api/impactxyz/{x}", null);
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/impactxyz/{x}", "NOT_MATCHED");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/impactxyz/{x}", "CREATED_1");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/impactxyz/{x}", "CREATED_2");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/impactxyz/{x}", "CREATED_3");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/impactxyz/{x}", "CREATED_4");

                }, 3);
    }
}
