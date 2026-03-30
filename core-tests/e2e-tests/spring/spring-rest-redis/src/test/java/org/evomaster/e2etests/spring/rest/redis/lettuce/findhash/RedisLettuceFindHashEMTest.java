package org.evomaster.e2etests.spring.rest.redis.lettuce.findhash;

import com.foo.spring.rest.redis.lettuce.findhash.RedisLettuceFindHashController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RedisLettuceFindHashEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_REDIS(true);
        RestTestBase.initClass(new RedisLettuceFindHashController(), config);
    }

    @Test
    public void testFindHashEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RedisLettuceFindHashEM",
                "org.foo.spring.rest.redis.RedisLettuceFindHashEM",
                1000,
                true,
                (args) -> {
                    setOption(args, "heuristicsForRedis", "true");
                    setOption(args, "instrumentMR_REDIS", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/redislettucefindhash/hash/{key}/{field}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/redislettucefindhash/findHash/{key}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/redislettucefindhash/findHash/{key}", null);
                },
                3);

    }
}
