package org.evomaster.e2etests.spring.rpc.examples.numericstring;

import com.foo.rpc.examples.spring.numericstring.NumericStringService;
import com.foo.rpc.examples.spring.numericstring.NumericStringWithSeedTestController;
import org.evomaster.core.problem.rpc.RPCCallAction;
import org.evomaster.core.problem.rpc.RPCCallResultCategory;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.foo.rpc.examples.spring.numericstring.NumericStringWithSeedTestController.CUSTOMIZED_FILE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class NumericStringWithSeedEMTest extends SpringRPCTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new NumericStringWithSeedTestController());
    }

    @BeforeEach
    public void cleanSaveDir() throws IOException {
        Path path = Paths.get(CUSTOMIZED_FILE);
        Files.deleteIfExists(path);
        if (!Files.exists(path.getParent()))
            Files.createDirectories(path.getParent());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "NumericStringWithSeedEM",
                "org.bar.NumericStringWithSeedEM",
                60,
                (args) -> {
                    args.add("--seedTestCases");
                    args.add("true");
                    args.add("--enableRPCCustomizedTestOutput");
                    args.add("true");
                    args.add("--exportTestCasesDuringSeeding");
                    args.add("true");


                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertRPCEndpointResult(solution, NumericStringService.Iface.class.getName()+":getNumber", RPCCallResultCategory.HANDLED.name());
                    assertAllContentInResponseForEndpoint(solution, NumericStringService.Iface.class.getName()+":getNumber",
                            Arrays.asList("NULL","LONG", "INT", "DOUBLE", "L_FOUND", "I_FOUND","D_FOUND","0_FOUND"));
                    // all values should conform to numeric format, ERROR should not exist in the response
                    assertNoneContentInResponseForEndpoint(solution, NumericStringService.Iface.class.getName()+":getNumber",
                            Arrays.asList("ERROR"));

                    checkCustomizedTests(solution);
                });

    }

    private void checkCustomizedTests(Solution<RPCIndividual> solution){
        Path customizedTests = Paths.get(CUSTOMIZED_FILE);
        assertTrue(Files.exists(customizedTests));

        List<String> content = solution.getIndividuals().stream().flatMap(s-> s.getIndividual().seeAllActions().stream().map(a-> ((RPCCallAction)a).getName())).collect(Collectors.toList());

        try {
            assertTrue(Files.readAllLines(customizedTests).containsAll(content));
        } catch (IOException e) {
            fail("Fail to read the customized tests");
        }
    }
}
