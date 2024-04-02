package org.evomaster.e2etests.spring.examples.authenticatedswaggeraccesstest;

import com.foo.rest.examples.spring.authenticatedswaggeraccess.AuthenticatedSwaggerAccessController;
import io.swagger.v3.oas.models.OpenAPI;
import org.evomaster.ci.utils.JUnitExtra;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo;

import org.evomaster.core.problem.httpws.auth.HttpWsNoAuth;
import org.evomaster.core.problem.rest.OpenApiAccess;
import org.evomaster.core.remote.SutProblemException;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AuthenticatedSwaggerAccessManualTest extends SpringTestBase {

    private static AuthenticatedSwaggerAccessController controller;
    private static AuthenticationDto successfulAuthenticationObject;

    @BeforeAll
    public static void initClass() throws Exception {

        controller = new AuthenticatedSwaggerAccessController();
        SpringTestBase.initClass(controller);
    }

    /**
     * Since the swagger endpoint is authenticated, it cannot be retrieved using no authentication object
     * In this case, OpenAPIAccess should throw SutException.
     */
    @Test
    public void accessSwaggerUnauthenticated() {

        // get all paths from the swagger


        JUnitExtra.assertThrowsInnermost(SutProblemException.class, () ->

                OpenApiAccess.INSTANCE.getOpenAPIFromURL(baseUrlOfSut + "/v2/api-docs", new HttpWsNoAuth())
        );


    }

    /**
     * Since the swagger endpoint is authenticated, it can be retrieved using authentication object.
     */
    @Test
    public void accessSwaggerTryAuthenticated() {

        boolean authenticatedRequestSuccessful = false;


        for(int i = 0; i < controller.getInfoForAuthentication().size() && !authenticatedRequestSuccessful; i++) {
            AuthenticationDto currentDto = controller.getInfoForAuthentication().get(i);

            HttpWsAuthenticationInfo currentInfo = HttpWsAuthenticationInfo.Companion.fromDto(currentDto);

            OpenAPI swagger = OpenApiAccess.INSTANCE.getOpenAPIFromURL(baseUrlOfSut + "/v2/api-docs", currentInfo);

            if (swagger != null) {
                successfulAuthenticationObject = currentDto;
                authenticatedRequestSuccessful = true;
            }

        }

        Assertions.assertTrue(authenticatedRequestSuccessful);
    }

    /**
     * Remove the successful authentication object to cause failed authentication
     */

    @Test
    //@Disabled
    public void accessSwaggerFailedAuthenticated() {

        boolean authenticatedRequestSuccessful = false;
        HttpWsAuthenticationInfo unsuccessfulInfo = null;

        for (int i = 0; i < controller.getInfoForAuthentication().size() && !authenticatedRequestSuccessful; i++) {
            AuthenticationDto currentDto = controller.getInfoForAuthentication().get(i);

            if (currentDto == successfulAuthenticationObject) {
                continue;
            }

            unsuccessfulInfo = HttpWsAuthenticationInfo.Companion.fromDto(currentDto);
        }

        HttpWsAuthenticationInfo finalUnsuccessfulInfo = unsuccessfulInfo;
        Assert.assertThrows(SutProblemException.class, () ->

                OpenApiAccess.INSTANCE.getOpenAPIFromURL(baseUrlOfSut + "/v2/api-docs", finalUnsuccessfulInfo)

                );
    }

}

