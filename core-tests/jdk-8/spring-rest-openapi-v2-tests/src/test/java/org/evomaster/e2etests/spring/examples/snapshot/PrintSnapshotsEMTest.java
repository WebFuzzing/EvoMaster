package org.evomaster.e2etests.spring.examples.snapshot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.foo.rest.examples.spring.taintignorecase.TaintIgnoreCaseController;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.core.output.OutputFormat;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by agusaldasoro on 1-Jun-2021.
 */
public class PrintSnapshotsEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new TaintIgnoreCaseController());
    }

    @Test
    public void testRunEM() throws Throwable {


        runTestHandlingFlakyAndCompilation(
                "PrintSnapshotsEM",
                "org.bar.PrintSnapshotsEM",
                1,
                (args) -> {
                    args.add("--maxTimeInSeconds");
                    args.add("2");
                    args.add("--enableWriteSnapshotTests");
                    args.add("true");
                    args.add("--writeSnapshotTestsIntervalInSeconds");
                    args.add("1");

                    initAndRun(args);

                    // Final test suite persists
                    File finalFile = new File(System.getProperty("user.dir") +
                            "/target/em-tests/PrintSnapshotsEM/org/bar/PrintSnapshotsEM.kt");
                    assertTrue(finalFile.exists());
                });
    }

    @Override
    protected List<String> getArgsWithCompilation(int iterations, String outputFolderName, ClassName testClassName, boolean createTests, String split, String summary){
        return new ArrayList<>(Arrays.asList(
                "--createTests", "" + createTests,
                "--seed", "" + defaultSeed,
                "--useTimeInFeedbackSampling" , "false",
                "--sutControllerPort", "" + controllerPort,
                "--stoppingCriterion", "TIME",
                "--outputFolder", outputFolderPath(outputFolderName),
                "--outputFormat", OutputFormat.KOTLIN_JUNIT_5.toString(),
                "--testSuiteFileName", testClassName.getFullNameWithDots(),
                "--testSuiteSplitType", split,
                "--expectationsActive", "TRUE",
                "--executiveSummary", summary
        ));
    }

}