package org.evomaster.e2etests.spring.examples.db.disablesql;

import com.foo.rest.examples.spring.db.disablesql.DisableSqlController;
import com.foo.rest.examples.spring.db.javatypes.JavaTypesController;
import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.evomaster.e2etests.spring.examples.db.javatypes.JavaTypesTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DisableSqlEMTest extends SpringTestBase {


    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new DisableSqlController());
    }

    @Test
    public void testRunEMWithoutSqlInsertion() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "DisableSqlEM",
                "com.foo.rest.examples.spring.db.disablesql.DisableSqlEM",
                3_000,
                (args) -> {
                    args.add("--generateSqlDataWithSearch");
                    args.add("false");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/api/db/disablesql/", null);

                    boolean hasInsertedSqlData = solution.getIndividuals().stream().anyMatch(
                            ind -> hasAtLeastOne(ind, HttpVerb.GET, 200, "/api/db/disablesql/", null));

                    assertFalse(hasInsertedSqlData);
                 });
    }



}
