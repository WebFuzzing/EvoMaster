package org.evomaster.e2etests.spring.examples.tcpport;

import com.foo.rest.examples.spring.tcpport.TcpPortController;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
public class TcpPortEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new TcpPortController());
    }

    @Test
    public void testRunEM() throws Throwable {

        /*
            Make sure that, by default, EvoMaster re-use TCP connections, instead
            of creating new ones at HTTP request.

            Unfortunately, by default Tomcat will close a connection every 100 calls,
            EVEN IF the client send a Keep-Alive.
            To make this test pass, we actually had to modify the maxKeepAliveRequests
            in Tomcat via the configuration class TomcatCustomizer.
            Unfortunately, maxKeepAliveRequests is not modifiable from application.yml:
            https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html
            although several other server.tomcat.* settings can
         */

        runTestHandlingFlaky(
                "TcpPortEM",
                "org.bar.TcpPortEM",
                1100,
                false,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/tcpPort", null);
                });

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        given().contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/tcpPort")
                .then()
                .statusCode(200)
                .body("size()", is(2)); // 1 from search, and 1 here from RestAssured
    }
}