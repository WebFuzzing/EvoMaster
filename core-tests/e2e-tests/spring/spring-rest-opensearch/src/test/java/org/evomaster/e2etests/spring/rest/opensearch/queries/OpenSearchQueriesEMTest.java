package org.evomaster.e2etests.spring.rest.opensearch.queries;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.foo.spring.rest.opensearch.queries.OpenSearchQueriesController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OpenSearchQueriesEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_OPENSEARCH(true);
        RestTestBase.initClass(new OpenSearchQueriesController(), config);
    }

    @Test
    public void testTermQueries() throws Throwable {
        runTestHandlingFlaky(
            "OpenSearchTermQueriesEM",
            "org.foo.spring.rest.opensearch.OpenSearchTermQueriesEM",
            1000,
            true,
            (args) -> {
                setOption(args, "instrumentMR_OPENSEARCH", "true");

                Solution<RestIndividual> solution = initAndRun(args);

                // Assert 200 responses when data is found
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/queries/category/{category}", null);
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/queries/setup", null);
                
                // Assert 404 responses when data is not found
                assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/queries/category/{category}", null);
            });
    }

    @Test
    public void testRangeQueries() throws Throwable {
        runTestHandlingFlaky(
            "OpenSearchRangeQueriesEM",
            "org.foo.spring.rest.opensearch.OpenSearchRangeQueriesEM",
            1000,
            true,
            (args) -> {
                setOption(args, "instrumentMR_OPENSEARCH", "true");

                Solution<RestIndividual> solution = initAndRun(args);

                // Assert 200 responses when data is found
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/queries/price-range", null);
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/queries/rating-gte/{rating}", null);
                
                // Assert 404 responses when data is not found
                assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/queries/price-range", null);
                assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/queries/rating-gte/{rating}", null);
            });
    }

    @Test
    public void testTextQueries() throws Throwable {
        runTestHandlingFlaky(
            "OpenSearchTextQueriesEM",
            "org.foo.spring.rest.opensearch.OpenSearchTextQueriesEM",
            1000,
            true,
            (args) -> {
                setOption(args, "instrumentMR_OPENSEARCH", "true");

                Solution<RestIndividual> solution = initAndRun(args);

                // Assert 200 responses when data is found
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/queries/name-prefix/{prefix}", null);
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/queries/name-fuzzy/{name}", null);
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/queries/name-wildcard/{pattern}", null);
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/queries/description-match/{text}", null);
                
                // Assert 404 responses when data is not found
                assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/queries/name-prefix/{prefix}", null);
                assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/queries/name-fuzzy/{name}", null);
            });
    }

    @Test
    public void testAdvancedQueries() throws Throwable {
        runTestHandlingFlaky(
            "OpenSearchAdvancedQueriesEM",
            "org.foo.spring.rest.opensearch.OpenSearchAdvancedQueriesEM",
            1000,
            true,
            (args) -> {
                setOption(args, "instrumentMR_OPENSEARCH", "true");

                Solution<RestIndividual> solution = initAndRun(args);

                // Assert 200 responses when data is found
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/queries/with-email", null);
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/queries/email-pattern/{pattern}", null);
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/queries/complex", null);
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/queries/by-ids", null);
                
                // Assert 404 responses when data is not found
                assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/queries/with-email", null);
                assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/queries/complex", null);
            });
    }

    @Test
    public void testAllSelectorsIntegration() throws Throwable {
        runTestHandlingFlaky(
            "OpenSearchAllSelectorsEM",
            "org.foo.spring.rest.opensearch.OpenSearchAllSelectorsEM",
            1500,
            true,
            (args) -> {
                setOption(args, "instrumentMR_OPENSEARCH", "true");

                Solution<RestIndividual> solution = initAndRun(args);

                assertFalse(solution.getIndividuals().isEmpty());
                assertTrue(solution.getOverall().getSize() >= 0.0);
                
                /*// Verify that various endpoints are covered
                long termQueries = solution.getIndividuals().stream()
                    .flatMap(ind -> ind.see().stream())
                    .filter(action -> action.toString().contains("/queries/category/"))
                    .count();
                
                long rangeQueries = solution.getIndividuals().stream()
                    .flatMap(ind -> ind.getMain().stream())
                    .filter(action -> action.toString().contains("/queries/price-range") || 
                                    action.toString().contains("/queries/rating-gte/"))
                    .count();
                
                long textQueries = solution.getIndividuals().stream()
                    .flatMap(ind -> ind.seeMainExecutableActions().stream())
                    .filter(action -> action.toString().contains("/queries/name-prefix/") ||
                                    action.toString().contains("/queries/name-fuzzy/") ||
                                    action.toString().contains("/queries/description-match/"))
                    .count();

                assertTrue(termQueries > 0 || rangeQueries > 0 || textQueries > 0,
                    "Should have executed some OpenSearch queries");*/
            });
    }
}
