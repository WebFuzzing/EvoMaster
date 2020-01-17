package org.evomaster.e2etests.spring.examples.impact;

import com.foo.rest.examples.spring.impact.ImpactRestController;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.problem.rest.util.ParamUtil;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Individual;
import org.evomaster.core.search.Solution;
import org.evomaster.core.search.gene.Gene;
import org.evomaster.core.search.impact.GeneImpact;
import org.evomaster.core.search.impact.GeneMutationSelectionMethod;
import org.evomaster.core.search.impact.ImpactUtils;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;


/**
 * created by manzh on 2019-09-12
 */
public class ImpactEMTest extends SpringTestBase {

    @Test
    public void testAwayNoImpact() throws Throwable {
        testRunEM(GeneMutationSelectionMethod.AWAY_NOIMPACT);
    }

    @Test
    public void testImpact() throws Throwable {
        testRunEM(GeneMutationSelectionMethod.APPROACH_IMPACT);
    }

    @Test
    public void testLatestImpact() throws Throwable {
        testRunEM(GeneMutationSelectionMethod.APPROACH_LATEST_IMPACT);
    }

    @Test
    public void testLatestImprovement() throws Throwable {
        testRunEM(GeneMutationSelectionMethod.APPROACH_LATEST_IMPROVEMENT);
    }

    @Test
    public void testBalance() throws Throwable {
        testRunEM(GeneMutationSelectionMethod.BALANCE_IMPACT_NOIMPACT);
    }

    public void testRunEM(GeneMutationSelectionMethod method) throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "none",
                "none",
                2_000,
                false,
                (args) -> {

                    args.add("--probOfArchiveMutation");
                    args.add("0.75");

                    args.add("--geneSelectionMethod");
                    args.add(method.toString());

                    args.add("--enableTrackEvaluatedIndividual");
                    args.add("true");

                    args.add("--exportImpacts");
                    args.add("true");
                    args.add("--impactFile");
                    args.add("target/impactInfo/TestSimpleGeneImpacts_"+method.toString()+".csv");

                    args.add("--exportCoveredTarget");
                    args.add("true");
                    args.add("--coveredTargetFile");

                    String path = "target/coveredTargets/testImpacts_CoveredTargetsBy"+method.toString()+".csv";
                    args.add(path);

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertTrue(Files.exists(Paths.get(path)));

                    try {
                        assertEquals(solution.getOverall().coveredTargets() + 1, Files.readAllLines(Paths.get(path)).size());
                    }catch (IOException e){
                        fail("cannot read the file on "+path);
                    }

                    boolean impactInfoCollected = solution.getIndividuals().stream().allMatch(
                            s -> s.getImpactOfGenes().size() > 0 && checkNoImpact("noimpactIntField", s)
                    );

                    assertTrue(impactInfoCollected);

                }, 3);
    }

    private String getGeneIdByName(String geneName, EvaluatedIndividual<RestIndividual> ind){

        Gene gene = ind.getIndividual().seeGenes(Individual.GeneFilter.NO_SQL).stream().filter(g -> ParamUtil.Companion.getValueGene(g).getName().equals(geneName))
                .findAny()
                .orElse(null);

        assertNotNull(gene);

        return ImpactUtils.Companion.generateGeneId(ind.getIndividual(), gene);
    }

    private boolean checkNoImpact(String geneName, EvaluatedIndividual<RestIndividual> ind){

        if (ind.getImpactOfGenes().values().stream().map(s -> ((GeneImpact) s).getTimesToManipulate()).mapToInt(Integer::intValue).sum() == 0 ) return true;

        String id = getGeneIdByName(geneName, ind);

        boolean last = true;

        GeneImpact noimpactGene = ind.getImpactOfGenes().get(id);
        for (String keyId : ind.getImpactOfGenes().keySet()){
            if (keyId != id){
                GeneImpact other = ind.getImpactOfGenes().get(keyId);

                last = last &&
                        // getTimesOfImpact should be less than any others OR getTimesOfNoImpact should be more than any others
                        (noimpactGene.getMaxImpact() <= other.getMaxImpact()
                                || noimpactGene.getTimesOfNoImpacts() >= other.getTimesOfNoImpacts())
                        //&&
                        // ideally getTimesToManipulate might be less than any others
                        //(noimpactGene.getTimesToManipulate() <= ind.getImpactOfGenes().get(keyId).getTimesToManipulate())
                ;
            }
        }
        return last;
    }

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ImpactRestController(Arrays.asList("/api/intImpact")));
    }

}
