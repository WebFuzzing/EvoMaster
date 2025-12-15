package org.evomaster.e2etests.spring.rest.redis.lettuce;

import com.foo.spring.rest.redis.lettuce.RedisLettuceAppController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RedisLettuceEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_REDIS(true);
        RestTestBase.initClass(new RedisLettuceAppController(), config);
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RedisLettuceEM",
                "org.foo.spring.rest.redis.RedisLettuceEM",
                1000,
                true,
                (args) -> {
                    setOption(args, "heuristicsForRedis", "true");
                    setOption(args, "instrumentMR_REDIS", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/redislettuce/string/{key}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/redislettuce/findKey/{key}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/redislettuce/findKey/{key}", null);
                },
                3);

    }
}
