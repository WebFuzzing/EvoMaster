package org.evomaster.e2etests.spring.rpc.examples.testability;

import com.foo.rpc.examples.spring.testability.TestabilityService;
import com.foo.rpc.examples.spring.testability.TestabilityWithSeedTestController;
import org.evomaster.core.problem.rpc.RPCCallResultCategory;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.core.search.service.Statistics;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestabilityWithSeedTestEMTest extends SpringRPCTestBase {

    private final String targetFile = "target/covered-targets/TestabilityWithSeedTestTargets.txt";
    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new TestabilityWithSeedTestController());
    }


    @Test
    public void testRunEM() throws Throwable {


        runTestHandlingFlakyAndCompilation(
                "TestabilityWithSeedTestEM",
                "org.bar.TestabilityWithSeedTestEM",
                10,
                (args) -> {
                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.9");
                    args.add("--seedTestCases");
                    args.add("true");

                    args.add("--exportCoveredTarget");
                    args.add("true");
                    args.add("--coveredTargetFile");
                    args.add(targetFile);

//                    args.add("--minimize");
//                    args.add("false");

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    checkStatistics(solution);

                    assertRPCEndpointResult(solution, TestabilityService.Iface.class.getName()+":getSeparated", RPCCallResultCategory.HANDLED.name());
                    assertAllContentInResponseForEndpoint(solution,TestabilityService.Iface.class.getName()+":getSeparated" , Arrays.asList("ERROR", "OK"));

                    checkSeedingTarget();
                });
    }

    private void checkStatistics(Solution solution){
        List<Statistics.Pair> data = solution.getStatistics();

        for(String key : Arrays.asList("coveredTargets", "coveredLines", "coveredBranches")){
            List<Statistics.Pair> dataByKey = data.stream().filter(s-> s.getHeader().toLowerCase().endsWith(key.toLowerCase())).collect(Collectors.toList());
            assertEquals(4, dataByKey.size());
            int bootTime = Integer.parseInt(dataByKey.stream().filter(s-> s.getHeader().startsWith("bootTime")).findFirst().get().getElement());
            int seedingTime = Integer.parseInt(dataByKey.stream().filter(s-> s.getHeader().startsWith("seedingTime")).findFirst().get().getElement());
            int searchTime = Integer.parseInt(dataByKey.stream().filter(s-> s.getHeader().startsWith("searchTime")).findFirst().get().getElement());
            int total = Integer.parseInt(dataByKey.stream().filter(s-> s.getHeader().startsWith("covered")).findFirst().get().getElement());
            assertTrue(bootTime >= 0);
            assertTrue(seedingTime > 0);
            assertTrue(searchTime > 0);
            assertTrue(total > 0);
            /*
                 comment out this assertion, as targets relating to authentication are counted in booting and search time
                 see more info in FitnessValue.kt.
                 however, here there is auth, so should not be a problem for this E2E
             */
            assertEquals(total, bootTime + seedingTime + searchTime);
        }

    }

    private void checkSeedingTarget(){
        Path path = Paths.get(targetFile);
        assertTrue(path.toFile().exists());

        List<String> targets = null;
        List<String> expectedTargets = Arrays.asList(
                "Branch_at_com.foo.rpc.examples.spring.testability.TestabilityServiceImp_at_line_00019_position_0_trueBranch_160",
                "Branch_at_com.foo.rpc.examples.spring.testability.TestabilityServiceImp_at_line_00019_position_1_trueBranch_160",
                "Branch_at_com.foo.rpc.examples.spring.testability.TestabilityServiceImp_at_line_00019_position_2_trueBranch_153",
                "Line_at_com.foo.rpc.examples.spring.testability.TestabilityServiceImp_00020"
        );
        try {
            targets = Files.readAllLines(path);
            int first = targets.indexOf("");
            List<String> seedingTimeTargets = targets.subList(0, first);
            assertFalse(seedingTimeTargets.isEmpty());
            assertTrue(seedingTimeTargets.containsAll(expectedTargets));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}

