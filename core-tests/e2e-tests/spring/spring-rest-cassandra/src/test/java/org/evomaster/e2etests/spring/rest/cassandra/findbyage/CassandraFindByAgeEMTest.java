package org.evomaster.e2etests.spring.rest.cassandra.findbyage;

import com.foo.spring.rest.cassandra.findbyage.CassandraFindByAgeController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class CassandraFindByAgeEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_CASSANDRA(true);
        RestTestBase.initClass(new CassandraFindByAgeController(), config);
    }

    @Test
    public void testFindByAgeEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "CassandraFindByAgeEM",
                "org.foo.spring.rest.cassandra.CassandraFindByAgeEM",
                1000,
                true,
                (args) -> {
                    setOption(args, "instrumentMR_CASSANDRA", "true");
                    setOption(args, "heuristicsForCassandra", "true");
                    setOption(args, "extractCassandraExecutionInfo", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/cassandrafindbyage/person/{name}/{age}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/cassandrafindbyage/person/{age}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/cassandrafindbyage/person/{age}", null);
                },
                3);
    }
}