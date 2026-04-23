package org.evomaster.e2etests.spring.examples.db.existingdata;

import com.foo.rest.examples.spring.db.existingdata.ExistingDataController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by arcuri82 on 19-Jun-19.
 */
public class ExistingDataEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ExistingDataController());
    }


    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testRunEM(boolean heuristicsForSQLAdvanced) throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "DbExistingDataEM",
                "org.bar.db.ExistingDataEM" + (heuristicsForSQLAdvanced ? "Complete" : "Partial"),
                50, //this should be trivial to cover
                (args) -> {

                    setOption(args, "heuristicsForSQL", "true");
                    setOption(args, "generateSqlDataWithSearch", "true");
                    setOption(args, "heuristicsForSQLAdvanced", heuristicsForSQLAdvanced ? "true" : "false");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    //trivial
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/db/existingdata", null);
                    //this should only happen if we can generate data with FK pointing to existing PK
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/existingdata", null);
                });
    }

}
