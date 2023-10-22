package org.evomaster.e2etests.spring.examples.base64;

import com.foo.rest.examples.spring.base64.Base64DecodeController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Created by arcuri82 on 07-Nov-18.
 */
public class Base64DecodeEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        Base64DecodeController controller = new Base64DecodeController();
        SpringTestBase.initClass(controller);
    }


    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "Base64DecodeEM",
                "org.Base64DecodeEM",
                500,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/base64/decode", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/api/base64/decode", null);

                }
        );
    }
}