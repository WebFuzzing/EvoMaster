package org.evomaster.e2etests.spring.examples.impactXYZ;

import com.foo.rest.examples.spring.impactXYZ.ImpactXYZRestController;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.problem.util.ParamUtil;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.core.search.action.ActionFilter;
import org.evomaster.core.search.gene.Gene;
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact;
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


public class ArchiveGeneSelectionImpactXYZInfoTest extends SpringTestBase {

    /**
     * this test aims at testing whether impacts are collected correctly without any impact-based solutions.
     */
    @Test
    public void testCollectionRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "TestImpactCollection",
                "org.impactxyz.TestImpactCollection",
                1000,
                true,
                (args) -> {
                    args.add("--weightBasedMutationRate");
                    args.add("false");

                    args.add("--archiveGeneMutation");
                    args.add("NONE");

                    args.add("--doCollectImpact");
                    args.add("true");

                    args.add("--adaptiveGeneSelectionMethod");
                    args.add("NONE");

                    //since there only exist one endpoint, we set the population for each target 3
                    args.add("--archiveTargetLimit");
                    args.add("3");

                    args.add("--enableTrackEvaluatedIndividual");
                    args.add("true");

                    args.add("--focusedSearchActivationTime");
                    args.add("0.0");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    solution.getIndividuals().stream().allMatch(
                            s -> s.anyImpactInfo() && checkImpactOfxyz(s)
                    );

                }, 3);
    }

    /**
     * this test aims at testing whether impactful gene has more chance to be selected.
     */
    @Test
    public void testGeneSelectionRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "TestGeneSelection",
                "org.impactxyz.TestGeneSelection",
                1000,
                true,
                (args) -> {

                    args.add("--probOfArchiveMutation");
                    args.add("0.5");

                    args.add("--weightBasedMutationRate");
                    args.add("true");

                    args.add("--adaptiveGeneSelectionMethod");
                    args.add("APPROACH_IMPACT");

                    args.add("--archiveGeneMutation");
                    args.add("SPECIFIED_WITH_SPECIFIC_TARGETS");

                    args.add("--enableTrackEvaluatedIndividual");
                    args.add("true");

                    //since there only exist one endpoint, we set the population for each target 3
                    args.add("--archiveTargetLimit");
                    args.add("3");

                    args.add("--focusedSearchActivationTime");
                    args.add("0.0");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    solution.getIndividuals().stream().allMatch(
                            s -> s.anyImpactInfo() && checkManipulatedTimes(s)
                    );

                }, 3);
    }

    private String getGeneIdByName(String geneName, EvaluatedIndividual<RestIndividual> ind){

        Gene gene = ind.getIndividual().seeTopGenes(ActionFilter.NO_SQL).stream().filter(g -> ParamUtil.Companion.getValueGene(g).getName().equals(geneName))
                .findAny()
                .orElse(null);

        assertNotNull(gene);

        return ImpactUtils.Companion.generateGeneId(ind.getIndividual(), gene);
    }

    private boolean checkImpactOfxyz(EvaluatedIndividual<RestIndividual> ind){

        // skip call to get swagger
        if (ind.getIndividual().seeAllActions().stream().noneMatch(a-> a.getName().startsWith("/api")))
            return true;


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

    private boolean checkManipulatedTimes(EvaluatedIndividual<RestIndividual> ind){
        return ind.getGeneImpact("x").stream().map(s->s.getTimesToManipulate()).reduce(0, Integer::sum)
                >= ind.getGeneImpact("y").stream().map(s->s.getTimesToManipulate()).reduce(0, Integer::sum) &&
                ind.getGeneImpact("y").stream().map(s->s.getTimesToManipulate()).reduce(0, Integer::sum)
                    >= ind.getGeneImpact("z").stream().map(s->s.getTimesToManipulate()).reduce(0, Integer::sum);
    }

    private double getValue(Map<Integer, Double> map, int key){
        return map.get(key) == null? 0.0: map.get(key);
    }

    private Map<Integer, Double> extract(EvaluatedIndividual<RestIndividual> ind, String name){
        Map<Integer, Double> impacts = new HashMap<>();
        String id = getGeneIdByName(name, ind);
        for (GeneImpact gi : ind.getGeneImpact(id)){
            int m = gi.getTimesToManipulate();
            for (Map.Entry<Integer, Double> e : gi.getTimesOfImpacts().entrySet()){
                double d = (e.getValue() * 1.0)/m;
                impacts.merge(e.getKey(), d, (prev, one) -> Math.max(prev, one));
            }
        }
        return impacts;
    }


    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ImpactXYZRestController(Arrays.asList("/api/impactdto/{x}")));
    }
}
