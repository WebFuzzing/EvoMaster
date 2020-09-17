package org.evomaster.e2etests.spring.examples.impactXYZ;

import org.evomaster.core.EMConfig;
import org.evomaster.core.EMConfig.ArchiveGeneMutation;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.core.search.impact.impactinfocollection.GeneMutationSelectionMethod;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


public class ArchiveGeneMutationImpactXYZTest extends ImpactXYZTestBase {

    private final String folder = "AGM-ImpactXYZ";

    @Test
    public void testWithout() throws Throwable {
        testRunEM(0.0, GeneMutationSelectionMethod.NONE, ArchiveGeneMutation.NONE);
    }

    @Test
    public void testOnlyApproachImpactSelection() throws Throwable {
        testRunEM(1.0, GeneMutationSelectionMethod.APPROACH_IMPACT, ArchiveGeneMutation.NONE);
    }

    @Test
    public void testOnlyBalanceSelection() throws Throwable {
        testRunEM(1.0, GeneMutationSelectionMethod.BALANCE_IMPACT_NOIMPACT_WITH_E, ArchiveGeneMutation.NONE);
    }

    public void testRunEM(double probOfArchive,GeneMutationSelectionMethod method, ArchiveGeneMutation agm) throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "none",
                "none",
                2000,
                false,
                (args) -> {

                    args.add("--probOfArchiveMutation");
                    args.add(""+probOfArchive);

                    args.add("--weightBasedMutationRate");
                    args.add("true");

                    args.add("--adaptiveGeneSelectionMethod");
                    args.add(method.toString());

                    args.add("--archiveGeneMutation");
                    args.add(agm.toString());

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
