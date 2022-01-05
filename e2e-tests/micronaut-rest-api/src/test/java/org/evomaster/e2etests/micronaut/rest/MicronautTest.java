package org.evomaster.e2etests.micronaut.rest;

import com.foo.micronaut.rest.MicronautTestController;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MicronautTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        RestTestBase.initClass(new MicronautTestController());
    }

    @Test
    public void testRunEM() throws Throwable {
        handleFlaky(() -> {
            String[] args = new String[] {
                    "--createTests", "false",
                    "--seed", "" + defaultSeed++,
                    "--sutControllerPort", "" + controllerPort,
                    "--maxActionEvaluations", "20000",
                    "--stoppingCriterion", "FITNESS_EVALUATIONS"
            };

            Solution<RestIndividual> solution = (Solution<RestIndividual>) Main.initAndRun(args);

            assertTrue(solution.getIndividuals().size() >= 1);

            assertHasAtLeastOne(solution, HttpVerb.GET, 500);
            assertHasAtLeastOne(solution, HttpVerb.POST, 200);
            assertHasAtLeastOne(solution, HttpVerb.PUT, 501);
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 503);
            assertEquals(HttpHeaderValues.KEEP_ALIVE, HttpHeaderValues.KEEP_ALIVE);
        });
    }
}
