package org.evomaster.e2etests.spring.rest.rsa;

import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


public class
RsaEMTest extends RestTestBase {




    @BeforeAll
    public static void initClass() throws Exception {

        RestTestBase.initClass(new RsaController());
    }


    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RsaEM",
                50,
                (args) -> {

                    //UUID.randomUUID() makes assertions flaky, which we don't handle yet
                    setOption(args,"enableBasicAssertions", "false");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bind_card_apply", null);
                }
        );
    }
}