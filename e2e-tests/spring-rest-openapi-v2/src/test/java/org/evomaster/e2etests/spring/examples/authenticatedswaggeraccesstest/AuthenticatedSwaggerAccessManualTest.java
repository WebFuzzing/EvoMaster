package org.evomaster.e2etests.spring.examples.authenticatedswaggeraccesstest;

import com.foo.rest.examples.spring.authenticatedswaggeraccess.AuthenticatedSwaggerAccessController;
import com.foo.rest.examples.spring.security.accesscontrol.deleteput.ACDeletePutController;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.core.problem.httpws.auth.AuthenticationHeader;
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo;
import org.evomaster.core.problem.httpws.auth.NoAuth;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.OpenApiAccess;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.remote.SutProblemException;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

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
     * @throws Throwable
     */
    @Test
    public void accessSwaggerUnauthenticated() throws Throwable {

        // get all paths from the swagger
        OpenAPI swagger = OpenApiAccess.INSTANCE.getOpenAPIFromURL(baseUrlOfSut + "/v2/api-docs", new NoAuth());

        // api paths
        Paths apiPaths = swagger.getPaths();

        Assert.assertNull(swagger.getPaths());

    }

    /**
     * Since the swagger endpoint is authenticated, it can be retrieved using authentication object.
     * @throws Throwable
     */
    @Test
    public void accessSwaggerTryAuthenticated() throws Throwable {

        boolean authenticatedRequestSuccessful = false;

        for(int i = 0; i < controller.getInfoForAuthentication().size() && !authenticatedRequestSuccessful; i++) {
            AuthenticationDto currentDto = controller.getInfoForAuthentication().get(i);

            List<AuthenticationHeader> headers = new ArrayList<>();

            for(int j = 0; j < currentDto.headers.size(); j++)
            {
                headers.add(new AuthenticationHeader(currentDto.headers.get(j).name.trim(),
                        currentDto.headers.get(j).value.trim()));
            }

            HttpWsAuthenticationInfo currentInfo = new HttpWsAuthenticationInfo(currentDto.name, headers,
                    null, null);

            OpenAPI swagger = OpenApiAccess.INSTANCE.getOpenAPIFromURL(baseUrlOfSut + "/v2/api-docs", currentInfo);

            if (swagger.getPaths() != null) {
                successfulAuthenticationObject = currentDto;
                authenticatedRequestSuccessful = true;
            }

        }

        Assert.assertTrue(authenticatedRequestSuccessful);
    }

    /**
     * Remove the successfulm authentication object to cause failed authentication
     * @throws Throwable
     */
    @Test
    public void accessSwaggerFailedAuthenticated() throws Throwable {

        boolean authenticatedRequestSuccessful = false;
        HttpWsAuthenticationInfo unsuccessfulInfo = null;

        for (int i = 0; i < controller.getInfoForAuthentication().size() && !authenticatedRequestSuccessful; i++) {
            AuthenticationDto currentDto = controller.getInfoForAuthentication().get(i);

            if (currentDto == successfulAuthenticationObject) {
                continue;
            }

            List<AuthenticationHeader> headers = new ArrayList<>();

            for (int j = 0; j < currentDto.headers.size(); j++) {
                headers.add(new AuthenticationHeader(currentDto.headers.get(j).name.trim(),
                        currentDto.headers.get(j).value.trim()));
            }

            unsuccessfulInfo = new HttpWsAuthenticationInfo(currentDto.name, headers,
                    null, null);
        }

        HttpWsAuthenticationInfo finalUnsuccessfulInfo = unsuccessfulInfo;
        Assert.assertThrows(SutProblemException.class, () ->

                OpenApiAccess.INSTANCE.getOpenAPIFromURL(baseUrlOfSut + "/v2/api-docs", finalUnsuccessfulInfo)

                );
    }

}

