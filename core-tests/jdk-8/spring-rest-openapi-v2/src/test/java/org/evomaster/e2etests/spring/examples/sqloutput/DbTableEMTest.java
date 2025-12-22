package org.evomaster.e2etests.spring.examples.sqloutput;

import com.foo.rest.examples.spring.sqloutput.DbTableController;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbTableEMTest extends SpringTestBase {


    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new DbTableController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "DbTableEM",
                "org.db.sqloutput.DbTableEM",
                1000,
                (args) -> {

                    String saveExecutedSQLToFile = "target/executionInfo/org/db/sqloutput/sql.txt";

                    args.add("--outputExecutedSQL");
                    args.add("ALL_AT_END");
                    args.add("--saveExecutedSQLToFile");
                    args.add(saveExecutedSQLToFile);

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertTrue(Files.exists(Paths.get(saveExecutedSQLToFile)));
                });
    }
}
