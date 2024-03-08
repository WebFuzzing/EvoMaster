package org.evomaster.e2etests.spring.examples.authenticatedswaggeraccesstest;

import com.foo.rest.examples.spring.authenticatedswaggeraccess.AuthenticatedSwaggerAccessController;

import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


public class AuthenticatedSwaggerAccessEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        AuthenticatedSwaggerAccessController controller = new AuthenticatedSwaggerAccessController();
        SpringTestBase.initClass(controller);
    }


    /**
     * Since the swagger endpoint is authenticated, it can be retrieved using authentication object.
     */
    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "AuthenticatedSwaggerAccessEM",
                "org.bar.AuthenticatedSwaggerAccessEM",
                100,
                (args) -> initAndRun(args) );
    }

}
