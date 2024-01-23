package org.evomaster.e2etests.spring.examples.filecreationissue;

import com.foo.rest.examples.spring.endpointfocusandprefix.EndpointFocusAndPrefixController;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.Assert;



public class FileCreationIssueEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        // re-using the already developed application BodyTypes
        SpringTestBase.initClass(new EndpointFocusAndPrefixController());
    }

    @Test
    public void testFileCreationIssue() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "FileCreationIssueEM",
                "org.FileCreationIssueEM",
                1,
                (args) -> {

                    args.add("--blackBox");
                    args.add("true");
                    args.add("--bbSwaggerUrl");
                    args.add(baseUrlOfSut + "/v2/api-docs");
                    args.add("--writeStatistics");
                    args.add("true");
                    args.add("--maxTime");
                    args.add("60s");

                    Solution<RestIndividual> solution = initAndRun(args);

                    // check for existence of the file EvoMaster_Tests
                    Assert.assertNotNull(solution);


                }
        );

    }
}