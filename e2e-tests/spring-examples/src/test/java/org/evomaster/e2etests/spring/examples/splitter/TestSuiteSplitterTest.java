package org.evomaster.e2etests.spring.examples.splitter;

import org.evomaster.core.EMConfig;
import org.evomaster.core.output.TestSuiteSplitter;
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

    private void testRunEMMulti(EMConfig.TestSuiteSplitType splitType) throws Throwable {
        List<String> terminations = Arrays.asList("_successes", "_remainder");
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
                    List<Solution<?>> splits = TestSuiteSplitter.INSTANCE.split(solution, splitType);
                    assertTrue(splits.size() >= 1);
                }
        );
    }

    private void testRunEM(EMConfig.TestSuiteSplitType splitType) throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "SplitterEM",
                "org.bar.splitter.Split_" + splitType,
                10_000,
                (args) -> {
                    args.add("--testSuiteSplitType");
                    args.add("" + splitType);

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    List<Solution<?>> splits = TestSuiteSplitter.INSTANCE.split(solution, splitType);

                    assertTrue(splits.size() >= 1);

                }
        );
    }


}
