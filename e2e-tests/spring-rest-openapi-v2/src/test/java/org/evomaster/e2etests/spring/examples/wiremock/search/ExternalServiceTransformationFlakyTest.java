package org.evomaster.e2etests.spring.examples.wiremock.search;

import com.foo.rest.examples.spring.wiremock.search.SearchController;
import io.restassured.http.ContentType;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

public class ExternalServiceTransformationFlakyTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SearchController searchController = new SearchController();
        EMConfig config = new EMConfig();
        config.setInstrumentMR_NET(true);
        SpringTestBase.initClass(searchController, config);
    }

    @Test
    public void requestTransformationTest() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "SearchTransformationEMTest",
                "org.bar.SearchTransformationEMTest",
                500,
                (args) -> {

                    args.add("--externalServiceIPSelectionStrategy");
                    args.add("USER");
                    args.add("--externalServiceIP");
                    args.add("127.0.0.2");

                    Solution<RestIndividual> solution = initAndRun(args);

//                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wiremock/search/foo", "true");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wiremock/search/return/foo", "foo");
                });
    }

    @Test
    public void manualTest() {
        /*
         * The test will check whether the external call is a success or
         * not. If the target host replaced with the Wiremock, it'll respond
         * true otherwise false.
         * */
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/search/foo")
                .then()
                .statusCode(200)
                .body(is("true"));
    }
}
