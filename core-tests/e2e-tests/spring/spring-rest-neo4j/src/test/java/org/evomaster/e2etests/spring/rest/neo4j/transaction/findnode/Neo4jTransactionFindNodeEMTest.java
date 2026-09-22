package org.evomaster.e2etests.spring.rest.neo4j.transaction.findnode;

import com.foo.spring.rest.neo4j.transaction.findnode.Neo4jTransactionFindNodeController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Neo4jTransactionFindNodeEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_NEO4J(true);
        RestTestBase.initClass(new Neo4jTransactionFindNodeController(), config);
    }

    @Test
    public void testFindNodeEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "Neo4jTransactionFindNodeEM",
                "org.foo.spring.rest.neo4j.Neo4jTransactionFindNodeEM",
                1000,
                true,
                (args) -> {
                    setOption(args, "heuristicsForNeo4j", "true");
                    setOption(args, "instrumentMR_NEO4J", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/neo4jtransactionfindnode/x/foo/{y}/bar", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/neo4jtransactionfindnode/findPerson/{name}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/neo4jtransactionfindnode/findPerson/{name}", null);
                },
                3);
    }
}
