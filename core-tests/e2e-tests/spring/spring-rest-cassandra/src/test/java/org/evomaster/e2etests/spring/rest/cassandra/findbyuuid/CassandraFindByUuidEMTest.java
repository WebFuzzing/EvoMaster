package org.evomaster.e2etests.spring.rest.cassandra.findbyuuid;

import com.foo.spring.rest.cassandra.findbyuuid.CassandraFindByUuidController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class CassandraFindByUuidEMTest extends RestTestBase {

    private static final String OUTPUT_FOLDER = "CassandraFindByUuidEM";
    private static final String TEST_CLASS = "org.foo.spring.rest.cassandra.CassandraFindByUuidEM";

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_CASSANDRA(true);
        RestTestBase.initClass(new CassandraFindByUuidController(), config);
    }

    /**
     * The SUT has no endpoint writing into the table, so the 200 can only come from a row inserted by
     * EvoMaster itself, with a uuid matching the one the query asks for.
     */
    @Test
    public void testFindByUuidEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                OUTPUT_FOLDER,
                TEST_CLASS,
                1000,
                true,
                (args) -> {
                    setOption(args, "instrumentMR_CASSANDRA", "true");
                    setOption(args, "heuristicsForCassandra", "true");
                    setOption(args, "extractCassandraExecutionInfo", "true");
                    setOption(args, "generateCassandraData", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/cassandrafindbyuuid/record/{id}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/cassandrafindbyuuid/record/{id}", null);

                    /*
                        One assertion per column of the table: a written literal is what says a value
                        was generated for that CQL type and rendered in a way Cassandra accepts, as the
                        generated tests are not only compiled but also executed.
                     */
                    assertTextInTests(OUTPUT_FOLDER, TEST_CLASS, ".d(\"id\"");
                    assertTextInTests(OUTPUT_FOLDER, TEST_CLASS, ".d(\"created\"");
                    assertTextInTests(OUTPUT_FOLDER, TEST_CLASS, ".d(\"elapsed\"");
                    assertTextInTests(OUTPUT_FOLDER, TEST_CLASS, ".d(\"ip\"");
                    assertTextInTests(OUTPUT_FOLDER, TEST_CLASS, ".d(\"tags\"");
                },
                3);
    }
}