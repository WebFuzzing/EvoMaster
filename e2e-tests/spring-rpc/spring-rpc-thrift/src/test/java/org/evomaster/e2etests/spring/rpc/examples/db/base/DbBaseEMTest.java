package org.evomaster.e2etests.spring.rpc.examples.db.base;

import com.foo.rpc.examples.spring.db.base.DbBaseController;
import com.foo.rpc.examples.spring.db.base.DbBaseService;
import org.evomaster.ci.utils.CIUtils;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbBaseEMTest extends SpringRPCTestBase {


    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new DbBaseController());
    }


    @Test
    public void testRunEM() throws Throwable {

        //TODO check it later, only fail on CI
//        CIUtils.skipIfOnGA();

        defaultSeed = 0;

        runTestHandlingFlakyAndCompilation(
                "DbBaseEM",
                "org.bar.db.BaseEM",
                10_000,
                (args) -> {

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertSizeInResponseForEndpoint(solution, DbBaseService.Iface.class.getName()+":getByName", 1, null);
                });
    }
}
