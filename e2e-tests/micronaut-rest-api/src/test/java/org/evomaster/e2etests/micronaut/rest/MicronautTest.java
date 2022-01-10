package org.evomaster.e2etests.micronaut.rest;

import com.foo.micronaut.rest.MicronautTestController;
import io.micronaut.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaderValues;
//import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestCallAction;
import org.evomaster.core.problem.rest.RestCallResult;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.ActionResult;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MicronautTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        RestTestBase.initClass(new MicronautTestController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlaky("MicronautTest", "com.foo.MicronautTest", 1100, false, (args) -> {
            args.add("--killSwitch");
            args.add("false");

            Solution<RestIndividual> solution = initAndRun(args);

            assertTrue(solution.getIndividuals().size() >= 1);
            assertHasAtLeastOne(solution, HttpVerb.GET, 500);
            assertHasAtLeastOne(solution, HttpVerb.POST, 200);
            // this is not completed yet, below one is a experiment
            assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/", "Crashed");
        } );
    }
}
