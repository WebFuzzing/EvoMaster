package org.evomaster.e2etests.spring.rest.cassandra.findbydayrange;

import com.foo.spring.rest.cassandra.findbydayrange.CassandraFindByDayRangeController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class CassandraFindByDayRangeEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_CASSANDRA(true);
        RestTestBase.initClass(new CassandraFindByDayRangeController(), config);
    }

    /**
     * Two conditions have to hold at once for the 200: the generated row must be on the day the query
     * asks for, and later than the instant it asks for. Both can only be met by data EvoMaster
     * inserts, as the SUT has no endpoint writing into the table.
     */
    @Test
    public void testFindByDayRangeEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "CassandraFindByDayRangeEM",
                "org.foo.spring.rest.cassandra.CassandraFindByDayRangeEM",
                1000,
                true,
                (args) -> {
                    setOption(args, "instrumentMR_CASSANDRA", "true");
                    setOption(args, "heuristicsForCassandra", "true");
                    setOption(args, "extractCassandraExecutionInfo", "true");
                    setOption(args, "generateCassandraData", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200,
                            "/cassandrafindbydayrange/measurement/{day}/after/{millis}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404,
                            "/cassandrafindbydayrange/measurement/{day}/after/{millis}", null);
                },
                3);
    }
}