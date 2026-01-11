package org.evomaster.e2etests.spring.examples.authenticatedswaggeraccesstest;

import com.foo.rest.examples.spring.authenticatedswaggeraccesswithoutauthentication.AuthenticatedSwaggerAccessWithoutAuthenticationController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

public class AuthenticatedSwaggerAccessWithoutAuthenticationManualTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new AuthenticatedSwaggerAccessWithoutAuthenticationController());
    }

    /**
     * Authentication should throw exception since we donot have the right authentication object for accessing the
     * swagger.
     */
    @Test
    public void testAuthenticationThrowsException() {

        // Check that exception is thrown
        Assert.assertThrows(InvocationTargetException.class, () ->
                runTestHandlingFlakyAndCompilation(
                        "AuthenticatedSwaggerAccessWithoutAuthentication",
                        "org.bar.AuthenticatedSwaggerAccessWithoutAuthentication",
                        1,
                        (args) -> {

                            initAndRun(args);

                        })
                );



    }
}
