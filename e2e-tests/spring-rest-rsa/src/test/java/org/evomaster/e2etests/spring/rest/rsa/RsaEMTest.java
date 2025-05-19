package org.evomaster.e2etests.spring.rest.rsa;

import com.example.demo.controller.EmController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


public class RsaEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        RestTestBase.initClass(new EmController());
    }


    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RsaEM",
                50,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bind_card_apply", null);
                }
        );
    }
}