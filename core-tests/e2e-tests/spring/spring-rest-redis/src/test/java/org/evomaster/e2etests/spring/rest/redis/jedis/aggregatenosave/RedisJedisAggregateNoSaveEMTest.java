package org.evomaster.e2etests.spring.rest.redis.jedis.aggregatenosave;

import com.foo.spring.rest.redis.jedis.aggregatenosave.RedisJedisAggregateNoSaveController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RedisJedisAggregateNoSaveEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_REDIS(true);
        RestTestBase.initClass(new RedisJedisAggregateNoSaveController(), config);
    }

    @Test
    public void testAggregateNoSaveEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RedisJedisAggregateNoSaveEM",
                "org.foo.spring.rest.redis.RedisJedisAggregateNoSaveEM",
                1000,
                true,
                (args) -> {
                    setOption(args, "heuristicsForRedis", "true");
                    setOption(args, "instrumentMR_REDIS", "true");
                    setOption(args, "extractRedisExecutionInfo", "true");
                    setOption(args, "generateRedisData", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/redisjedisaggregatenosave/countByCategory", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/redisjedisaggregatenosave/countByCategory", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/redisjedisaggregatenosave/avgPriceByCategory", null);
                },
                6);

    }
}
