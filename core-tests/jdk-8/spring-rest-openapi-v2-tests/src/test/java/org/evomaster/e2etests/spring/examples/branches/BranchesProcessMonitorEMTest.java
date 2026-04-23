package org.evomaster.e2etests.spring.examples.branches;

import com.foo.rest.examples.spring.branches.BranchesController;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class BranchesProcessMonitorEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        BranchesController controller = new BranchesController();
        SpringTestBase.initClass(controller);
    }


    @Test
    public void testRunEnablePMEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "BranchesEnablePMEM",
                "org.foo.BranchesEnablePMEM",
                1000,
                (args) -> {
                    args.add("--enableProcessMonitor");
                    args.add("true");

                    args.add("--processFormat");
                    args.add("TARGET_TEST_IND");

                    args.add("--processInterval");
                    args.add("10");


                    String outputDir = "target/process_data";
                    args.add("--processFiles");
                    args.add(outputDir);

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    Path dir = Paths.get(outputDir+"/data");
                    assertTrue(Files.exists(dir));
                    try (Stream<Path> stream = Files.list(dir)) {
                        assertEquals(20, stream.count());
                    } catch (IOException e) {
                        fail();
                    }


                });
    }
}