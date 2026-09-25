package org.evomaster.e2etests.spring.rest.redis.jedis.aggregate;

import com.foo.spring.rest.redis.jedis.aggregate.RedisJedisAggregateController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RedisJedisAggregateEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_REDIS(true);
        RestTestBase.initClass(new RedisJedisAggregateController(), config);
    }

    @Test
    public void testAggregateEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RedisJedisAggregateEM",
                "org.foo.spring.rest.redis.RedisJedisAggregateEM",
                1000,
                true,
                (args) -> {
                    setOption(args, "heuristicsForRedis", "true");
                    setOption(args, "instrumentMR_REDIS", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/redisjedisaggregate/item/{name}/{category}/{price}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/redisjedisaggregate/aggregate/countByCategory/{name}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/redisjedisaggregate/aggregate/countByCategory/{name}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/redisjedisaggregate/aggregate/avgPriceByCategory/{min}/{max}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/redisjedisaggregate/aggregate/avgPriceByCategory/{min}/{max}", null);
                },
                3);

    }
}
