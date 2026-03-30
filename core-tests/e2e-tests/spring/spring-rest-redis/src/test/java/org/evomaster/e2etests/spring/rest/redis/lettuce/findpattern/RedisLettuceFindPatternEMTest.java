package org.evomaster.e2etests.spring.rest.redis.lettuce.findpattern;

import com.foo.spring.rest.redis.lettuce.findpattern.RedisLettuceFindPatternController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RedisLettuceFindPatternEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_REDIS(true);
        RestTestBase.initClass(new RedisLettuceFindPatternController(), config);
    }

    @Test
    public void testFindPatternEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RedisLettuceFindPatternEM",
                "org.foo.spring.rest.redis.RedisLettuceFindPatternEM",
                1000,
                true,
                (args) -> {
                    setOption(args, "heuristicsForRedis", "true");
                    setOption(args, "instrumentMR_REDIS", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/redislettucefindpattern/string/{key}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/redislettucefindpattern/findPattern", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/redislettucefindpattern/findPattern", null);
                },
                3);

    }
}
