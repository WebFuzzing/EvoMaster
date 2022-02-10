package org.evomaster.e2etests.spring.rpc.examples.db.existingdata;

import com.foo.rpc.examples.spring.db.existingdata.DbExistingDataController;
import com.foo.rpc.examples.spring.db.existingdata.DbExistingDataService;
import org.evomaster.core.problem.rpc.RPCCallResultCategory;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbExistingDataEMTest extends SpringRPCTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringRPCTestBase.initClass(new DbExistingDataController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "DbExistingDataEM",
                "org.bar.db.DbExistingDataEM",
                50,
                (args) -> {


                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertRPCEndpointResult(solution, DbExistingDataService.Iface.class.getName()+":get", RPCCallResultCategory.HANDLED.name());
                    assertAllContentInResponseForEndpoint(solution,DbExistingDataService.Iface.class.getName()+":get" ,
                            Arrays.asList("EMPTY","NOT_EMPTY"));
                });
    }
}
