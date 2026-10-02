package org.evomaster.e2etests.spring.rest.neo4j.session.findnodenosave;

import com.foo.spring.rest.neo4j.session.findnodenosave.Neo4jSessionFindNodeNoSaveController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Neo4jSessionFindNodeNoSaveEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_NEO4J(true);
        config.setHeuristicsForNeo4j(true);
        config.setExtractNeo4jExecutionInfo(true);
        config.setGenerateNeo4jData(true);
        RestTestBase.initClass(new Neo4jSessionFindNodeNoSaveController(), config);
    }

    @Test
    public void testFindNodeNoSaveWithGeneratedDataEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "Neo4jSessionFindNodeNoSaveEM",
                "org.foo.spring.rest.neo4j.Neo4jSessionFindNodeNoSaveEM",
                100,
                true,
                (args) -> {
                    setOption(args, "heuristicsForNeo4j", "true");
                    setOption(args, "instrumentMR_NEO4J", "true");
                    setOption(args, "extractNeo4jExecutionInfo", "true");
                    setOption(args, "generateNeo4jData", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/neo4jsessionfindnodenosave/findPerson", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/neo4jsessionfindnodenosave/findPerson", null);
                },
                3);
    }
}
