package org.evomaster.e2etests.spring.rest.cassandra.findbyagereadonly;

import com.foo.spring.rest.cassandra.findbyagereadonly.CassandraFindByAgeReadOnlyController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class CassandraFindByAgeReadOnlyEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_CASSANDRA(true);
        RestTestBase.initClass(new CassandraFindByAgeReadOnlyController(), config);
    }

    /**
     * The SUT has no endpoint writing into the table, so the 200 can only come from a row inserted
     * by EvoMaster itself, out of the failed query the controller reported.
     */
    @Test
    public void testFindByAgeReadOnlyEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "CassandraFindByAgeReadOnlyEM",
                "org.foo.spring.rest.cassandra.CassandraFindByAgeReadOnlyEM",
                1000,
                true,
                (args) -> {
                    setOption(args, "instrumentMR_CASSANDRA", "true");
                    setOption(args, "heuristicsForCassandra", "true");
                    setOption(args, "extractCassandraExecutionInfo", "true");
                    setOption(args, "generateCassandraData", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/cassandrafindbyagereadonly/person/{age}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/cassandrafindbyagereadonly/person/{age}", null);
                },
                3);
    }
}