package org.evomaster.e2etests.spring.examples.displayissue;

import com.foo.rest.examples.spring.bodytypes.BodyTypesController;

import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;


/**
 * This class is to test the issue: <a href="https://github.com/EMResearch/EvoMaster/issues/765">#765</a>
 * In which number of potential faults and number of test targets covered are shown for whitebox
 * but not for blackbox.
 * Run the application
 *
 */

public class DisplayIssueEMTest extends SpringTestBase {

    /*
    statistics file to be used
     */
    private final String statisticsFile = "stats.csv";

    @BeforeAll
    public static void initClass() throws Exception {

        // re-using the already developed application BodyTypes
        BodyTypesController controller = new BodyTypesController();
        SpringTestBase.initClass(controller);
    }

    /*
    In this test case, we first run a white box test for a simple web application. After that, we check that the output
    and the statistics match. After that, we run a blackbox test using its swagger, check the output and the generated
    statistics file.
     */
    @Test
    public void testRunBlackboxPotentialFaultsDisplayed() throws Throwable {

        // redirect console output to PrintStream

        // old print stream
        PrintStream old = System.out;

        try {
            runTestHandlingFlakyAndCompilation(
                    "DisplayIssueEM",
                    "org.DisplayIssueEM",
                    5,
                    (args) -> {

                        args.add("--blackBox");
                        args.add("true");
                        args.add("--bbTargetUrl");
                        args.add(baseUrlOfSut);
                        args.add("--bbSwaggerUrl");
                        args.add(baseUrlOfSut + "/v2/api-docs");
                        args.add("--writeStatistics");
                        args.add("true");
                        args.add("--statisticsFile");
                        args.add(statisticsFile);

                        ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
                        System.setOut(new PrintStream(byteArrayOutput));

                        Solution<RestIndividual> solutionWhite = initAndRun(args);

                        String expectedNumberOfPotentialFaultsWhite = findValueOfItemWithKeyInStats
                                (solutionWhite, "potentialFaults");

                        Assertions.assertTrue(byteArrayOutput.toString().contains("Potential faults: "
                                + expectedNumberOfPotentialFaultsWhite));

                    });
        } finally {

            File statsFile = new File(this.statisticsFile);
            statsFile.delete();
            System.setOut(old);
        }

    }

}
