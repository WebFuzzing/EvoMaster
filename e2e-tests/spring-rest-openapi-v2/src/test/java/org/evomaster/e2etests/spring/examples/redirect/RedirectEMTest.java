package org.evomaster.e2etests.spring.examples.redirect;

import com.foo.rest.examples.spring.redirect.RedirectController;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.path.json.config.JsonPathConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.config.RedirectConfig.redirectConfig;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
public class RedirectEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new RedirectController());
    }

    @Test
    public void testRunEM() throws Throwable {

        RestAssured.config().jsonConfig(JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.DOUBLE))
        .redirect(redirectConfig().followRedirects(false));
        //RestAssured

        runTestHandlingFlakyAndCompilation(
                "RedirectEM",
                "org.bar.RedirectEM",
                100,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 301, "/api/redirect/301","301");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 302, "/api/redirect/302","302");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 307, "/api/redirect/307","307");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 308, "/api/redirect/308","308");
                });
    }
}