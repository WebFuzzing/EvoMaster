package org.evomaster.e2etests.spring.examples.assertions;

import com.foo.rest.examples.spring.expectations.ExpectationsController;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.core.EMConfig;
import org.evomaster.core.output.OutputFormat;
import org.evomaster.core.output.TestSuiteSplitter;
import org.evomaster.core.output.clustering.SplitResult;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssertionsEMTest extends SpringTestBase {
    @BeforeAll
    public static void initClass() throws Exception{
        SpringTestBase.initClass(new ExpectationsController());
    }

    @Test
    public void testRunEMAssertionsOn() throws Throwable{
        String outputFolderName = "AssertionsEM";
        ClassName className = new ClassName("org.AssertionsEM");
        clearGeneratedFiles(outputFolderName, className);
        int iterations = 10_000;

        EMConfig em = new EMConfig();
        em.setOutputFormat(OutputFormat.JAVA_JUNIT_5);
        em.setEnableBasicAssertions(false);
        em.setTestSuiteSplitType(EMConfig.TestSuiteSplitType.NONE);

        String split = em.getTestSuiteSplitType().toString();

        Consumer<List<String>> lambda = (args) -> {

            args.add("--enableBasicAssertions");
            args.add("true");

            //args.set(15, "JAVA_JUNIT_5");
            args.replaceAll( s -> s.replace("KOTLIN_JUNIT_5", "JAVA_JUNIT_5"));

            Solution<RestIndividual> solution = initAndRun(args);
            assertTrue(solution.getIndividuals().size() >= 1);
            SplitResult splits = TestSuiteSplitter.INSTANCE.split(solution, em);
            assertTrue(splits.splitOutcome.size() >= 1);
        };

        assertTimeoutPreemptively(Duration.ofMinutes(3), ()->{
            clearGeneratedFiles(outputFolderName, className);

            handleFlaky(
                    () -> {
                        List<String> args = getArgsWithCompilation(iterations, outputFolderName, className, true, split, "true");
                        defaultSeed++;
                        lambda.accept(new ArrayList<>(args));
                    }
            );
        });
    }

    @Test
    public void testRunEMAssertionsOff() throws Throwable{
        String outputFolderName = "AssertionsEM";
        ClassName className = new ClassName("org.AssertionsEM");
        clearGeneratedFiles(outputFolderName, className);
        int iterations = 10_000;

        EMConfig em = new EMConfig();
        em.setOutputFormat(OutputFormat.JAVA_JUNIT_5);
        em.setEnableBasicAssertions(false);
        em.setTestSuiteSplitType(EMConfig.TestSuiteSplitType.NONE);

        String split = em.getTestSuiteSplitType().toString();

        Consumer<List<String>> lambda = (args) -> {

            args.add("--enableBasicAssertions");
            args.add("false");

            //args.set(15, "JAVA_JUNIT_5");
            args.replaceAll( s -> s.replace("KOTLIN_JUNIT_5", "JAVA_JUNIT_5"));

            Solution<RestIndividual> solution = initAndRun(args);
            assertTrue(solution.getIndividuals().size() >= 1);
            SplitResult splits = TestSuiteSplitter.INSTANCE.split(solution, em);
            assertTrue(splits.splitOutcome.size() >= 1);
        };

        assertTimeoutPreemptively(Duration.ofMinutes(3), ()->{
            clearGeneratedFiles(outputFolderName, className);

            handleFlaky(
                    () -> {
                        List<String> args = getArgsWithCompilation(iterations, outputFolderName, className, true, split, "true");
                        defaultSeed++;
                        lambda.accept(new ArrayList<>(args));
                    }
            );
        });
    }
}
