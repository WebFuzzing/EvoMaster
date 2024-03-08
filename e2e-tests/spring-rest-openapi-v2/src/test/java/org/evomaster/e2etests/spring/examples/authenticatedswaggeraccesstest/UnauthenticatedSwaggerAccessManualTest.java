package org.evomaster.e2etests.spring.examples.authenticatedswaggeraccesstest;

import com.foo.rest.examples.spring.unauthenticatedswaggeraccesscontroller.UnauthenticatedSwaggerAccessController;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.core.problem.httpws.auth.AuthenticationHeader;
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo;
import org.evomaster.core.problem.httpws.auth.HttpWsNoAuth;
import org.evomaster.core.problem.rest.OpenApiAccess;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class UnauthenticatedSwaggerAccessManualTest extends SpringTestBase {

    private static UnauthenticatedSwaggerAccessController controller;

    @BeforeAll
    public static void initClass() throws Exception {

        controller = new UnauthenticatedSwaggerAccessController();
        SpringTestBase.initClass(controller);
    }

    /**
     * Since the swagger endpoint is authenticated, it cannot be retrieved using no authentication object
     * @throws Throwable
     */
    @Test
    public void accessSwaggerUnauthenticatedShouldSucceed() throws Throwable {

        // get all paths from the swagger
        OpenAPI swagger = OpenApiAccess.INSTANCE.getOpenAPIFromURL(baseUrlOfSut + "/v2/api-docs", new HttpWsNoAuth());

        // api paths
        Paths apiPaths = swagger.getPaths();

        Assert.assertNotNull(swagger.getPaths());

    }





}
