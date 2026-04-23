package org.evomaster.e2etests.spring.rpc.examples.db.tree;

import com.foo.rpc.examples.spring.db.tree.DbTreeController;
import com.foo.rpc.examples.spring.db.tree.DbTreeService;
import com.foo.rpc.examples.spring.db.tree.DbTreeServiceImp;
import org.evomaster.core.problem.rpc.RPCCallResultCategory;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbTreeEMTest extends SpringRPCTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringRPCTestBase.initClass(new DbTreeController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "DbTreeEM",
                "org.bar.db.TreeEM",
                1000,
                (args) -> {


                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertRPCEndpointResult(solution, DbTreeService.Iface.class.getName()+":get", RPCCallResultCategory.HANDLED.name());
                    assertAllContentInResponseForEndpoint(solution,DbTreeService.Iface.class.getName()+":get" ,
                            Arrays.asList(DbTreeServiceImp.NO_PARENT, DbTreeServiceImp.WITH_PARENT, DbTreeServiceImp.NOT_FOUND));
                });
    }
}
