package org.evomaster.e2etests.spring.examples.splitter;

import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.core.EMConfig;
import org.evomaster.core.output.TestSuiteSplitter;
import org.evomaster.core.output.Termination;
import org.evomaster.core.output.clustering.SplitResult;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSuiteSplitterTest extends SplitterTestBase {



    @Test
    public void testRunEM_NONE() throws Throwable {
        testRunEMMulti(EMConfig.TestSuiteSplitType.NONE, false);
    }

    @Test
    public void testRunEM_CLUSTERING() throws Throwable{
        testRunEMMulti(EMConfig.TestSuiteSplitType.FAULTS, false);
    }

//    @Test
//    public void testRunEM_CLUSTERING_SUMMARY() throws Throwable{
//        testRunEMMulti(EMConfig.TestSuiteSplitType.FAULTS, true);
//    }

    private void testRunEMMulti(EMConfig.TestSuiteSplitType splitType, Boolean executiveSummary) throws Throwable {
        List<String> terminations = Arrays.asList();

        if(splitType == EMConfig.TestSuiteSplitType.FAULTS){
            if(executiveSummary) {
                terminations = Arrays.asList(Termination.FAULTS.getSuffix(),
                        Termination.SUCCESSES.getSuffix(),
                        Termination.OTHERS.getSuffix(),
                        Termination.FAULT_REPRESENTATIVES.getSuffix());
            }
            else {
                terminations = Arrays.asList(Termination.FAULTS.getSuffix(),
                        Termination.SUCCESSES.getSuffix(),
                        Termination.OTHERS.getSuffix());
            }
        }


        EMConfig em = new EMConfig();
        em.setTestSuiteSplitType(splitType);
        //em.setExecutiveSummary(executiveSummary);

        String outputFolderName = "SplitterEM";
        String fullClassName = "org.bar.splitter.Split_" + splitType;
        int iterations = 10_000;


        List<ClassName> classNames = new ArrayList<>();
        String split = splitType.toString();

        if(terminations.isEmpty()){
            classNames.add(new ClassName(fullClassName));
        } else {
            for (String termination : terminations) {
                classNames.add(new ClassName(fullClassName + termination));
            }
        }

        Consumer<List<String>> lambda = (args) -> {

            Solution<RestIndividual> solution = initAndRun(args);
            assertTrue(solution.getIndividuals().size() >= 1);
            SplitResult splits = TestSuiteSplitter.INSTANCE.split(solution, em);
            assertTrue(splits.splitOutcome.size() >= 1);
        };

        assertTimeoutPreemptively(Duration.ofMinutes(3), ()->{
            ClassName className = new ClassName(fullClassName);
            clearGeneratedFiles(outputFolderName, classNames);

            handleFlaky(
                    () -> {
                        List<String> args = getArgsWithCompilation(iterations, outputFolderName, className, true, split, executiveSummary.toString());
                        defaultSeed++;
                        lambda.accept(new ArrayList<>(args));
                    }
            );
        });

        /*runTestHandlingFlakyAndCompilation(
                "SplitterEM",
                "org.bar.splitter.Split_" + splitType,
                terminations,
                10_000,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);
                    assertTrue(solution.getIndividuals().size() >= 1);
                    SplitResult splits = TestSuiteSplitter.INSTANCE.split(solution, em);
                    assertTrue(splits.splitOutcome.size() >= 1);
                }
        );*/
    }
}
