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
import org.evomaster.core.search.impact.Impact;
import org.evomaster.core.search.impact.GeneMutationSelectionMethod;
import org.evomaster.core.search.impact.ImpactUtils;
import org.evomaster.core.search.impact.value.ObjectGeneImpact;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * created by manzh on 2019-09-12
 */
public class ObjectImpactEMTest extends SpringTestBase {

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
                1_000,
                false,
                (args) -> {

                    args.add("--probOfArchiveMutation");
                    args.add("1.0");

                    args.add("--geneSelectionMethod");
                    args.add(method.toString());

                    args.add("--enableTrackEvaluatedIndividual");
                    args.add("true");

                    args.add("--exportImpacts");
                    args.add("true");
                    args.add("--impactFile");
                    args.add("target/impactInfo/TestobjImpacts_"+method.toString()+".csv");

                    args.add("--exportCoveredTarget");
                    args.add("true");
                    args.add("--coveredTargetFile");
                    String path = "target/coveredTargets/testobjImpacts_CoveredTargetsBy"+method.toString()+".csv";
                    args.add(path);
                    args.add("--coveredTargetSortedBy");
                    args.add("TEST");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertTrue(Files.exists(Paths.get(path)));

                    try {
                        Set tests = new HashSet<String>();
                        Set targets = new HashSet<String>();
                        /*
                          skip header
                         */
                        Files.readAllLines(Paths.get(path)).stream().skip(1).forEach(
                                s -> {
                                    String[] line = s.split(",");
                                    assertEquals(2, line.length);
                                    tests.add(line[0]);
                                    targets.add(line[1]);
                                }
                        );
                        assertEquals(solution.getIndividuals().size(), tests.size());
                        assertEquals(solution.getOverall().coveredTargets(), targets.size());

                    }catch (IOException e){
                        fail("cannot read the file on "+path);
                    }

                    boolean impactInfoCollected = solution.getIndividuals().stream().allMatch(
                            s -> s.getImpactOfGenes().size() > 0 && checkNoImpact("noImpactIntField", s)
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

    private boolean checkNoImpact(String fieldName, EvaluatedIndividual<RestIndividual> ind){

        if (ind.getImpactOfGenes().values().stream().map(s -> ((GeneImpact) s).getTimesToManipulate()).mapToInt(Integer::intValue).sum() == 0 ) return true;

        List<Gene> genes = ind.getIndividual().seeGenes(Individual.GeneFilter.NO_SQL).stream().filter(s-> s instanceof ObjectGene).collect(Collectors.toList());

        assert(!genes.isEmpty());
        boolean last = true;

        for (Gene obj : genes){
            String id = ImpactUtils.Companion.generateGeneId(ind.getIndividual(), obj);
            GeneImpact impact = ind.getImpactOfGenes().get(id);

            assert(impact instanceof ObjectGeneImpact);

            Impact noImpactField = ((ObjectGeneImpact)impact).getFields().get(fieldName);
            assertNotNull(noImpactField);

            for (String keyId : ((ObjectGeneImpact)impact).getFields().keySet()){
                if (keyId != fieldName){
                    Impact other = ((ObjectGeneImpact)impact).getFields().get(keyId);
                    last = last &&
                            // getTimesOfImpact should be less than any others OR getTimesOfNoImpact should be more than any others
                           // (noImpactField.getTimesOfImpact() <= other.getTimesOfImpact()
                            (noImpactField.getMaxImpact() <=other.getMaxImpact()
                                    || noImpactField.getTimesOfNoImpacts() >= other.getTimesOfNoImpacts())
                            &&
                            // ideally getTimesToManipulate should be less than any others
                            (noImpactField.getTimesToManipulate() <= other.getTimesToManipulate());
                }
            }
        }

        return last;
    }

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ImpactRestController(Arrays.asList("/api/intImpact/{name}")));
    }

}
