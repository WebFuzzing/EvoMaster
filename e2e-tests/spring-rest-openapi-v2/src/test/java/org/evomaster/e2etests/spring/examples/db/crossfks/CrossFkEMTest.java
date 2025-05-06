package org.evomaster.e2etests.spring.examples.db.crossfks;

import com.foo.rest.examples.spring.db.crossfks.CrossFkController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CrossFkEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new CrossFkController());
    }
    @Test
    public void testEnableTaintSampleEM() throws Throwable {
        forceSqlAllColumnInsertion(true);
    }

    @Test
    public void testDisableTaintSampleEM() throws Throwable {
        forceSqlAllColumnInsertion(false);
    }


    private void forceSqlAllColumnInsertion(boolean taintOnSampling) throws Throwable{
        runTestHandlingFlakyAndCompilation(
                "CrossFkTaintSampling_"+taintOnSampling+"_EM",
                "org.bar.db.CrossFkTaintSampling_"+taintOnSampling+"_EM",
                5_000,
                true,
                (args) -> {

                    args.add("--probOfEnablingSingleInsertionForTable");
                    args.add("1.0");
                    args.add("--forceSqlAllColumnInsertion");
                    args.add("true");
                    args.add("--taintOnSampling");
                    args.add(""+taintOnSampling);


                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/root/{rootName}/foo/{fooName}/bar", "NOT EMPTY");
                },
                5
                );
    }
}
