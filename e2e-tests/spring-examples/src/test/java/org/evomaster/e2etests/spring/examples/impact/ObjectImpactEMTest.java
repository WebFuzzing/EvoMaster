package org.evomaster.e2etests.spring.examples.impact;

import com.foo.rest.examples.spring.impact.ImpactRestController;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.problem.rest.util.ParamUtil;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Individual;
import org.evomaster.core.search.Solution;
import org.evomaster.core.search.gene.Gene;
import org.evomaster.core.search.gene.ObjectGene;
import org.evomaster.core.search.impact.GeneImpact;
import org.evomaster.core.search.impact.ImpactMutationSelection;
import org.evomaster.core.search.impact.ImpactUtils;
import org.evomaster.core.search.impact.value.ObjectGeneImpact;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.glassfish.jersey.message.filtering.spi.ObjectGraph;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * created by manzh on 2019-09-12
 */
public class ObjectImpactEMTest extends SpringTestBase {

    @Test
    public void testAwayBad() throws Throwable {
        testRunEM(ImpactMutationSelection.AWAY_NOIMPACT);
    }

    @Test
    public void testApproachGood() throws Throwable {
        testRunEM(ImpactMutationSelection.APPROACH_IMPACT);
    }
//    @Test
//    public void testFeedback() throws Throwable {
//        testRunEM(ImpactMutationSelection.FEEDBACK_DIRECT);
//    }

    public void testRunEM(ImpactMutationSelection method) throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "",
                "",
                1_000,
                false,
                (args) -> {

                    args.add("--probOfArchiveMutation");
                    args.add("1.0");

                    args.add("--geneSelectionMethod");
                    args.add(method.toString());

                    args.add("--enableTrackEvaluatedIndividual");
                    args.add("true");

                    args.add("--archiveGeneMutation");
                    args.add("SPECIFIED");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    boolean impactInfoCollected = solution.getIndividuals().stream().allMatch(
                            s -> s.getImpactOfGenes().size() > 0 && checkNoImpact("noImpactIntField", s)
                    );

                    assertTrue(impactInfoCollected);

                });
    }

    private String getGeneIdByName(String geneName, EvaluatedIndividual<RestIndividual> ind){

        Gene gene = ind.getIndividual().seeGenes(Individual.GeneFilter.NO_SQL).stream().filter(g -> ParamUtil.Companion.getValueGene(g).getName().equals(geneName))
                .findAny()
                .orElse(null);

        assertNotNull(gene);

        return ImpactUtils.Companion.generateGeneId(ind.getIndividual(), gene);
    }

    private boolean checkNoImpact(String fieldName, EvaluatedIndividual<RestIndividual> ind){

        if (ind.getImpactOfGenes().values().stream().map(s -> ((GeneImpact) s).getTimesToManipulate()).mapToInt(Integer::intValue).sum() == 0 ) return true;

        List<Gene> genes = ind.getIndividual().seeGenes(Individual.GeneFilter.NO_SQL);
        ObjectGene obj = null;
        if (genes.size() == 1 && genes.get(0) instanceof ObjectGene){
            obj = (ObjectGene) genes.get(0);
        }else{
            throw new IllegalArgumentException("mismatched with specified");
        }

        boolean last = true;
        String id = ImpactUtils.Companion.generateGeneId(ind.getIndividual(), obj);
        GeneImpact impact = ind.getImpactOfGenes().get(id);

        assert(impact instanceof ObjectGraph);

        GeneImpact noImpactField = ((ObjectGeneImpact)impact).getFields().get(fieldName);
        assertNotNull(noImpactField);

        for (String keyId : ((ObjectGeneImpact)impact).getFields().keySet()){
            if (keyId != fieldName){
                GeneImpact other = ((ObjectGeneImpact)impact).getFields().get(keyId);
                last = last &&
                        // getTimesOfImpact should be less than any others OR getTimesOfNoImpact should be more than any others
                        (noImpactField.getTimesOfImpact() <= other.getTimesOfImpact()
                                || noImpactField.getTimesOfNoImpacts() >= other.getTimesOfNoImpacts())
                        &&
                        // getTimesToManipulate should be less than any others
                        (noImpactField.getTimesToManipulate() <= other.getTimesToManipulate());
            }
        }
        return last;
    }

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ImpactRestController(Arrays.asList("/api/intImpact/{name}")));
    }

}
