package org.evomaster.e2etests.spring.rpc.examples.db.directint;

import com.foo.rpc.examples.spring.db.base.DbBaseController;
import com.foo.rpc.examples.spring.db.base.DbBaseService;
import com.foo.rpc.examples.spring.db.directint.DbDirectIntController;
import com.foo.rpc.examples.spring.db.directint.DbDirectIntService;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbDirectIntEMTest extends SpringRPCTestBase {


    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new DbDirectIntController());
    }


    @Test
    public void testRunEM_AVG_DISTANCE() throws Throwable {
        testRunEM(EMConfig.SecondaryObjectiveStrategy.AVG_DISTANCE);
    }

    @Test
    public void testRunEM_AVG_DISTANCE_SAME_N_ACTIONS() throws Throwable {
        testRunEM(EMConfig.SecondaryObjectiveStrategy.AVG_DISTANCE_SAME_N_ACTIONS);
    }

    @Test
    public void testRunEM_BEST_MIN() throws Throwable {
        testRunEM(EMConfig.SecondaryObjectiveStrategy.BEST_MIN);
    }


    private void testRunEM(EMConfig.SecondaryObjectiveStrategy strategy) throws Throwable {
        final String outputFolder = "DbDirectEM_"+ strategy;
        final String outputTestName = "org.bar.db.DirectEM_" + strategy;

        runTestHandlingFlakyAndCompilation(
                outputFolder,
                outputTestName,
                7_000,
                (args) -> {

                    args.add("--secondaryObjectiveStrategy");
                    args.add("" + strategy);
                    args.add("--heuristicsForSQL");
                    args.add("true");
                    args.add("--generateSqlDataWithSearch");
                    args.add("false");

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertContentInResponseForEndpoint(solution, DbDirectIntService.Iface.class.getName()+":get", "400");
                    assertContentInResponseForEndpoint(solution, DbDirectIntService.Iface.class.getName()+":get", "200");

                    assertTextInTests(outputFolder, outputTestName, "controller.resetDatabase(listOf(\"db_direct_int_entity\"))");
                });
    }
}

