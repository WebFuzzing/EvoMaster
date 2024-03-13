package org.evomaster.e2etests.spring.examples.db.existingdata;

import com.foo.rest.examples.spring.db.existingdata.ExistingDataController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by arcuri82 on 19-Jun-19.
 */
public class ExistingDataEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ExistingDataController());
    }


    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "DbExistingDataEM",
                "org.bar.db.ExistingDataEM",
                50, //this should be trial to cover
                (args) -> {

                    args.add("--heuristicsForSQL");
                    args.add("true");
                    args.add("--generateSqlDataWithSearch");
                    args.add("true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    //trivial
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/db/existingdata", null);
                    //this should only happen if we can generate data with FK pointing to existing PK
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/existingdata", null);
                });
    }

}
