package org.evomaster.e2etests.spring.examples.splitter;

import org.evomaster.core.EMConfig;
import org.evomaster.core.output.TestSuiteSplitter;
import org.evomaster.core.output.Termination;
import org.evomaster.core.output.clustering.SplitResult;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSuiteSplitterTest extends SplitterTestBase {


    @Test
    public void testRunEM_CODE() throws Throwable {
        testRunEMMulti(EMConfig.TestSuiteSplitType.CODE);
    }

    @Test
    public void testRunEM_NONE() throws Throwable {
        testRunEM(EMConfig.TestSuiteSplitType.NONE);
    }

    @Test
    public void testRunEM_SUMMARY() throws Throwable{
        testRunEMMulti(EMConfig.TestSuiteSplitType.SUMMARY_ONLY);
    }

    @Test
    public void testRunEM_CLUSTERING() throws Throwable{
        testRunEMMulti(EMConfig.TestSuiteSplitType.CLUSTER);
    }

    private void testRunEMMulti(EMConfig.TestSuiteSplitType splitType) throws Throwable {
        List<String> terminations = Arrays.asList();

        if(splitType == EMConfig.TestSuiteSplitType.SUMMARY_ONLY){
            terminations = Arrays.asList();
        }
        if(splitType == EMConfig.TestSuiteSplitType.CODE){
            terminations = Arrays.asList(Termination.FAULTS.getSuffix(),
                    Termination.SUCCESSES.getSuffix(),
                    Termination.OTHER.getSuffix());
        }
        if(splitType == EMConfig.TestSuiteSplitType.CLUSTER){
            terminations = Arrays.asList(Termination.FAULTS.getSuffix(),
                    Termination.SUCCESSES.getSuffix(),
                    Termination.OTHER.getSuffix());
        }

        EMConfig em = new EMConfig();
        em.setTestSuiteSplitType(splitType);

        runTestHandlingFlakyAndCompilation(
                "SplitterEM",
                "org.bar.splitter.Split_" + splitType,
                terminations,
                10_000,
                (args) -> {
                    args.add("--testSuiteSplitType");
                    args.add("" + splitType);
                    Solution<RestIndividual> solution = initAndRun(args);
                    assertTrue(solution.getIndividuals().size() >= 1);
                    SplitResult splits = TestSuiteSplitter.INSTANCE.split(solution, em);
                    assertTrue(splits.splitOutcome.size() >= 1);
                }
        );
    }

    private void testRunEM(EMConfig.TestSuiteSplitType splitType) throws Throwable {
        EMConfig em = new EMConfig();
        em.setTestSuiteSplitType(splitType);

        runTestHandlingFlakyAndCompilation(
                "SplitterEM",
                "org.bar.splitter.Split_" + splitType,
                10_000,
                (args) -> {
                    args.add("--testSuiteSplitType");
                    args.add("" + splitType);

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    SplitResult splits = TestSuiteSplitter.INSTANCE.split(solution, em);

                    assertTrue(splits.splitOutcome.size() >= 1);

                }
        );
    }


}
