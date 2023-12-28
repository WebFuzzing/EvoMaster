package org.evomaster.e2etests.spring.examples.security.accesscontrol.deleteput;

import com.foo.rest.examples.spring.security.accesscontrol.deleteput.ACDeletePutController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;


public class ACDeletePutEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new ACDeletePutController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "ACDeletePutEM",
                "org.bar.ACDeletePutEM",
                100,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    // GET request
                    assertHasAtLeastOne(solution, HttpVerb.GET, HttpStatus.OK.value());
                    assertHasAtLeastOne(solution, HttpVerb.GET, HttpStatus.NOT_FOUND.value());

                    // PUT request
                    assertHasAtLeastOne(solution, HttpVerb.PUT, HttpStatus.CREATED.value());

                    // DELETE request
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, HttpStatus.NOT_FOUND.value());

                });
    }

}
