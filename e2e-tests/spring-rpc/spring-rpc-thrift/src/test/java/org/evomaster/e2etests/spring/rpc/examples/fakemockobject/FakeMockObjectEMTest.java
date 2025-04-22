package org.evomaster.e2etests.spring.rpc.examples.fakemockobject;

import com.foo.rpc.examples.spring.fakemockobject.FakeMockObjectController;
import com.foo.rpc.examples.spring.fakemockobject.generated.FakeMockObjectService;
import org.evomaster.core.problem.rpc.RPCCallResultCategory;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.util.Arrays;


import static org.junit.jupiter.api.Assertions.*;

public class FakeMockObjectEMTest extends SpringRPCTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new FakeMockObjectController());
    }


    @Test
    public void testRunEM() throws Throwable {


        runTestHandlingFlakyAndCompilation(
                "FakeMockObjectEM",
                "org.bar.FakeMockObjectEM",
                10,
                (args) -> {
                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.9");
                    args.add("--seedTestCases");
                    args.add("true");

                    args.add("--enableCustomizedMethodForMockObjectHandling");
                    args.add("true");

                    args.add("--enableBasicAssertions");
                    args.add("true");



//                    setOption(args, "saveScheduleTaskInvocationAsSeparatedFile", "true");
                    setOption(args, "enableCustomizedMethodForScheduleTaskHandling", "true");
                    setOption(args, "probOfSamplingScheduleTask", "0.5");


                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertRPCEndpointResult(solution, FakeMockObjectService.Iface.class.getName()+":getFooFromExternalService", RPCCallResultCategory.HANDLED.name());
                    assertContentInResponseForEndpoint(solution, FakeMockObjectService.Iface.class.getName()+":getFooFromExternalService", "EX:::");
                    assertRPCEndpointResult(solution, FakeMockObjectService.Iface.class.getName()+":getBarFromDatabase", RPCCallResultCategory.HANDLED.name());
                    assertContentInResponseForEndpoint(solution, FakeMockObjectService.Iface.class.getName()+":getBarFromDatabase", "DB:::");
                    assertContentInResponseForEndpoint(solution, FakeMockObjectService.Iface.class.getName()+":isExecutedToday", "true");


                    assertTrue(solution.getIndividuals().size() >= 1);

                });
    }


}

