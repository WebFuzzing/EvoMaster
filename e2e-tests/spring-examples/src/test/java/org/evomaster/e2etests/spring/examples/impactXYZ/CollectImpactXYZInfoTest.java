package org.evomaster.e2etests.spring.examples.impactXYZ;

import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.problem.rest.util.ParamUtil;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Individual;
import org.evomaster.core.search.Solution;
import org.evomaster.core.search.gene.Gene;
import org.evomaster.core.search.impact.impactInfoCollection.GeneImpact;
import org.evomaster.core.search.impact.impactInfoCollection.GeneMutationSelectionMethod;
import org.evomaster.core.search.impact.impactInfoCollection.ImpactUtils;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


public class CollectImpactXYZInfoTest extends SpringTestBase {

    @Test
    public void testOnlyCollectImpact() throws Throwable {
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

                    //since there only exist one endpoint, we set the population for each target 3
                    args.add("--archiveTargetLimit");
                    args.add("3");

                    args.add("--enableTrackEvaluatedIndividual");
                    args.add("true");

                    args.add("--focusedSearchActivationTime");
                    args.add("0.0");

                    // only for the test
                    args.add("--saveImpactAfterMutationFile");
                    args.add("target/impactXYZ/impactInfo/Impacts_ImpactXYZ_"+method.toString()+".csv");

                    // only for the test
                    args.add("--saveMutatedGeneFile");
                    args.add("target/impactXYZ/mutatedGeneInfo/MutatedGenes_ImpactXYZ_"+method.toString()+".csv");

                    // only for the test
                    args.add("--saveArchiveAfterMutationFile");
                    args.add("target/impactXYZ/archiveInfo/ArchiveNotCoveredSnapshot_ImpactXYZ_"+method.toString()+".csv");

                    args.add("--exportCoveredTarget");
                    args.add("true");

                    args.add("--coveredTargetFile");
                    String path = "target/impactXYZ/coveredTargets/testImpacts_CoveredTargetsBy"+method.toString()+".csv";
                    args.add(path);


                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    solution.getIndividuals().stream().allMatch(
                            s -> s.anyImpactInfo() && checkImpactOfxyz(s)
                    );

                }, 3);
    }

    private String getGeneIdByName(String geneName, EvaluatedIndividual<RestIndividual> ind){

        Gene gene = ind.getIndividual().seeGenes(Individual.GeneFilter.NO_SQL).stream().filter(g -> ParamUtil.Companion.getValueGene(g).getName().equals(geneName))
                .findAny()
                .orElse(null);

        assertNotNull(gene);

        return ImpactUtils.Companion.generateGeneId(ind.getIndividual(), gene);
    }

    private boolean checkImpactOfxyz(EvaluatedIndividual<RestIndividual> ind){

        Set<Integer> targets = new HashSet<>();
        Map<Integer, Double> ximpacts = extract(ind, "x");
        Map<Integer, Double> yimpacts = extract(ind, "y");
        Map<Integer, Double> zimpacts = extract(ind, "z");
        targets.addAll(ximpacts.keySet());
        targets.addAll(yimpacts.keySet());
        targets.addAll(zimpacts.keySet());
        //impact x > y > z
        return targets.stream().allMatch(
                s-> getValue(ximpacts, s) >= getValue(yimpacts, s) && getValue(ximpacts, s) >= getValue(zimpacts, s) && getValue(yimpacts, s) >= getValue(zimpacts, s)
        );
    }

    private double getValue(Map<Integer, Double> map, int key){
        return map.get(key) == null? 0.0: map.get(key);
    }

    private Map<Integer, Double> extract(EvaluatedIndividual<RestIndividual> ind, String name){
        Map<Integer, Double> impacts = new HashMap<>();
        String id = getGeneIdByName(name, ind);
        for (GeneImpact gi : ind.getGeneImpact(id)){
            int m = gi.getTimesToManipulate();
            for (Map.Entry<Integer, Integer> e : gi.getTimesOfImpacts().entrySet()){
                double d = (e.getValue() * 1.0)/m;
                impacts.merge(e.getKey(), d, (prev, one) -> Math.max(prev, one));
            }
        }
        return impacts;
    }



}
