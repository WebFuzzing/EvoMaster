package org.evomaster.e2etests.spring.examples.valid;

import com.foo.rest.examples.spring.valid.ValidController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ValidEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ValidController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "ValidEM",
                "org.bar.ValidEM",
                1000,
                (args) -> {

                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.9");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/valid", null);
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/valid", "OK");
                });
    }
}
