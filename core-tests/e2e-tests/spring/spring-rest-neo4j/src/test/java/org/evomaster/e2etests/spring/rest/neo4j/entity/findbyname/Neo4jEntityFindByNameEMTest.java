package org.evomaster.e2etests.spring.rest.neo4j.entity.findbyname;

import com.foo.spring.rest.neo4j.entity.findbyname.Neo4jEntityFindByNameController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.database.neo4j.Neo4jDbAction;
import org.evomaster.core.database.neo4j.Neo4jNodeTemplate;
import org.evomaster.core.database.neo4j.Neo4jPropertyGene;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class Neo4jEntityFindByNameEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_NEO4J(true);
        config.setHeuristicsForNeo4j(true);
        config.setExtractNeo4jExecutionInfo(true);
        config.setGenerateNeo4jData(true);
        RestTestBase.initClass(new Neo4jEntityFindByNameController(), config);
    }

    @Test
    public void testFindByNameInsertsThePropertiesOfTheEntityEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "Neo4jEntityFindByNameEM",
                "org.foo.spring.rest.neo4j.Neo4jEntityFindByNameEM",
                100,
                true,
                (args) -> {
                    setOption(args, "heuristicsForNeo4j", "true");
                    setOption(args, "instrumentMR_NEO4J", "true");
                    setOption(args, "extractNeo4jExecutionInfo", "true");
                    setOption(args, "generateNeo4jData", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/neo4jentityfindbyname/findPerson/{name}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/neo4jentityfindbyname/findPerson/{name}", null);

                    /*
                        The query only compares the name. The other properties come from the Person
                        class, except the id, whose value is generated.
                     */
                    Set<String> expected = new HashSet<>(Arrays.asList("name", "age", "active", "score"));
                    boolean inserted = false;
                    for (EvaluatedIndividual<RestIndividual> ind : solution.getIndividuals()) {
                        for (Neo4jDbAction action : seeNeo4jActions(ind)) {
                            for (Neo4jNodeTemplate node : action.getNodes()) {
                                assertEquals(Arrays.asList("Person"), node.getLabels());
                                Set<String> keys = node.getProperties().stream()
                                        .map(Neo4jPropertyGene::getKey).collect(Collectors.toSet());
                                assertEquals(expected, keys);
                                inserted = true;
                            }
                        }
                    }
                    assertTrue(inserted, "No Neo4j insertion was generated");
                },
                3);
    }

    private static List<Neo4jDbAction> seeNeo4jActions(EvaluatedIndividual<RestIndividual> ind) {
        return ind.getIndividual().seeInitializingActions().stream()
                .filter(a -> a instanceof Neo4jDbAction)
                .map(a -> (Neo4jDbAction) a)
                .collect(Collectors.toList());
    }
}
