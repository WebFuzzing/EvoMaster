package org.evomaster.e2etests.spring.examples.displayissue;

import com.foo.rest.examples.spring.bodytypes.BodyTypesController;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.core.search.service.Statistics;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

/**
 * This class is to test the issue: https://github.com/EMResearch/EvoMaster/issues/765
 *
 * In which number of potential faults and number of test targets covered are shown for whitebox
 * but not for blackbox.
 *
 * Run the application
 *
 */

public class DisplayIssueEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        // re-using the already developed application BodyTypes
        BodyTypesController controller = new BodyTypesController();
        SpringTestBase.initClass(controller);
    }

    private String findValueOfItemWithKeyInStats(Solution s, String k) {

        Statistics.Pair currentElement = null;
        List<Statistics.Pair> stats= s.getStatistics();
        Iterator<Statistics.Pair> statsIterator = stats.iterator();

        while(statsIterator.hasNext()) {
            currentElement = statsIterator.next();

            if (currentElement.getHeader().equals(k)) {
                return currentElement.getElement();
            }
        }

        return null;

    }

    @Test
    public void testRunBlackboxAndWhiteBox() throws Throwable {

        // redirect console output to PrintStream

        // old print stream
        PrintStream old = System.out;

        // run a white-box test using the controller first and check that
        runTestHandlingFlakyAndCompilation(
                "DisplayIssueEM",
                "org.DisplayIssueEM",
                1,
                (args) -> {

                    ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
                    System.setOut(new PrintStream(byteArrayOutput));

                    //ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    //PrintStream ps = new PrintStream(baos);

                    // old print Stream
                    //PrintStream old = System.out;


                    // --blackBox true --bbSwaggerUrl https://petstore.swagger.io/v2/swagger.json
                    // --outputFormat JAVA_JUNIT_4 --maxTime 60s --writeStatistics true
                    args.add("--writeStatistics");
                    args.add("true");


                    Solution<RestIndividual> solutionWhite = initAndRun(args);

                    // number of covered targets
                    // the output should contain
                    // * Covered targets (lines, branches, faults, etc.): {same value as the number of covered targets
                    // from statistics} -
                    // * Potential faults: {same value as the number of potential faults from statistics}
                    String expectedNumberOfPotentialFaultsWhite = findValueOfItemWithKeyInStats(solutionWhite, "potentialFaults");
                    String expectedNumberOfCoveredTargetsWhite = findValueOfItemWithKeyInStats(solutionWhite, "coveredTargets");

                    Assert.assertTrue(byteArrayOutput.toString().contains("Covered targets " +
                            "(lines, branches, faults, etc.): " + expectedNumberOfCoveredTargetsWhite));

                    Assert.assertTrue(byteArrayOutput.toString().contains("Potential faults: "
                            + expectedNumberOfPotentialFaultsWhite));

                    // now run the same thins as a blackbox test and ensure that
                    byteArrayOutput = new ByteArrayOutputStream();
                    System.setOut(new PrintStream(byteArrayOutput));

                    args.clear();

                    args.add("--blackBox");
                    args.add("true");
                    args.add("--bbTargetUrl");
                    args.add(baseUrlOfSut);
                    args.add("--bbSwaggerUrl");
                    args.add(baseUrlOfSut + "/v2/api-docs");
                    args.add("--outputFormat");
                    args.add("JAVA_JUNIT_4");
                    args.add("--writeStatistics");
                    args.add("true");

                    Solution<RestIndividual> solutionBlack = initAndRun(args);

                    // number of covered targets
                    // the output should contain
                    // * Covered targets (lines, branches, faults, etc.): {same value as the number of covered targets
                    // from statistics} -
                    // * Potential faults: {same value as the number of potential faults from statistics}
                    String expectedNumberOfPotentialFaultsBlack = findValueOfItemWithKeyInStats(solutionBlack, "potentialFaults");
                    String expectedNumberOfCoveredTargetsBlack = findValueOfItemWithKeyInStats(solutionBlack, "coveredTargets");

                    Assert.assertTrue(byteArrayOutput.toString().contains("Covered targets " +
                            "(lines, branches, faults, etc.): " + expectedNumberOfCoveredTargetsBlack));

                    Assert.assertTrue(byteArrayOutput.toString().contains("Potential faults: "
                            + expectedNumberOfPotentialFaultsBlack));

                }
        );

        System.setOut(old);



    }

}
