package org.evomaster.e2etests.spring.examples.authenticatedswaggeraccesstest;

import com.foo.rest.examples.spring.authenticatedswaggeraccess.AuthenticatedSwaggerAccessController;
import com.google.inject.Injector;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.core.problem.httpws.auth.AuthenticationHeader;
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo;
import org.evomaster.core.problem.httpws.auth.NoAuth;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.OpenApiAccess;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AuthenticatedSwaggerAccessEMTest extends SpringTestBase {

    private static AuthenticatedSwaggerAccessController controller;

    @BeforeAll
    public static void initClass() throws Exception {

        controller = new AuthenticatedSwaggerAccessController();
        SpringTestBase.initClass(controller);
    }


    /**
     * Since the swagger endpoint is authenticated, it can be retrieved using authentication object.
     * @throws Throwable
     */
    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "AuthenticatedSwaggerAccessEM",
                "org.bar.AuthenticatedSwaggerAccessEM",
                100,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                });
    }




}
