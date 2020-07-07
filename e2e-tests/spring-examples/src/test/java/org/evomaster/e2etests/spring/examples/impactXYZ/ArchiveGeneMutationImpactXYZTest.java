package org.evomaster.e2etests.spring.examples.impactXYZ;

import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.core.search.impact.impactinfocollection.GeneMutationSelectionMethod;
import org.junit.jupiter.api.Test;


public class ArchiveGeneMutationImpactXYZTest extends ImpactXYZTestBase {

    private final String folder = "AGM-ImpactXYZ";

    @Test
    public void testOnlyArchiveMutation() throws Throwable {
        testRunEM(GeneMutationSelectionMethod.APPROACH_IMPACT);
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

                    args.add("--enableTrackEvaluatedIndividual");
                    args.add("true");

                    args.add("--focusedSearchActivationTime");
                    args.add("0.0");

                    // only for the test
                    args.add("--saveImpactAfterMutation");
                    args.add("true");
                    args.add("--impactAfterMutationFile");
                    args.add("target/"+folder+"/impactInfo/Impacts_ImpactXYZ_"+method.toString()+".csv");

                    // only for the test
                    args.add("--saveMutationInfo");
                    args.add("true");
                    args.add("--mutatedGeneFile");
                    args.add("target/"+folder+"/mutatedGeneInfo/MutatedGenes_ImpactXYZ_"+method.toString()+".csv");

                    // only for the test
                    args.add("--saveArchiveAfterMutation");
                    args.add("true");
                    args.add("--archiveAfterMutationFile");
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
