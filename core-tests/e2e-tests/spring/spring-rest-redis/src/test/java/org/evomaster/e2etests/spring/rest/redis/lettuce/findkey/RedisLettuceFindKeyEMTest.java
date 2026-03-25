package org.evomaster.e2etests.spring.rest.redis.lettuce.findkey;

import com.foo.spring.rest.redis.lettuce.findkey.RedisLettuceFindKeyController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RedisLettuceFindKeyEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_REDIS(true);
        RestTestBase.initClass(new RedisLettuceFindKeyController(), config);
    }

    @Test
    public void testFindKeyEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RedisLettuceFindKeyEM",
                "org.foo.spring.rest.redis.RedisLettuceFindKeyEM",
                1000,
                true,
                (args) -> {
                    setOption(args, "heuristicsForRedis", "true");
                    setOption(args, "instrumentMR_REDIS", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/redislettucefindkey/string/{key}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/redislettucefindkey/findKey/{key}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/redislettucefindkey/findKey/{key}", null);
                },
                3);

    }
}
