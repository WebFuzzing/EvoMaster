package org.evomaster.e2etests.spring.examples.wiremock.http;


import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

public class HttpRequestEMTest extends HttpRequestTestBase {

    @Test
    public void testRunEM() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "HttpRequestEM",
                "org.bar.HttpRequestEM",
                500,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wiremock/external/url", "true");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wiremock/external/url/withQuery", "true");
                });
    }
}
