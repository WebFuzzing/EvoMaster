package org.evomaster.e2etests.spring.rest.redis.lettuce.setintersectionnosave;

import com.foo.spring.rest.redis.lettuce.setintersectionnosave.RedisLettuceSetIntersectionNoSaveController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RedisLettuceSetIntersectionNoSaveEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_REDIS(true);
        RestTestBase.initClass(new RedisLettuceSetIntersectionNoSaveController(), config);
    }

    @Test
    public void testSetIntersectionNoSaveEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RedisLettuceSetIntersectionNoSaveEM",
                "org.foo.spring.rest.redis.RedisLettuceSetIntersectionNoSaveEM",
                2000,
                true,
                (args) -> {
                    setOption(args, "maxEvaluations", "10");
                    setOption(args, "heuristicsForRedis", "true");
                    setOption(args, "instrumentMR_REDIS", "true");
                    setOption(args, "extractRedisExecutionInfo", "true");
                    setOption(args, "generateRedisData", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/redislettucesetintersectionnosave/set/variable-intersection/{set1}/{set2}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/redislettucesetintersectionnosave/set/variable-intersection/{set1}/{set2}", null);

                },
                6);

    }
}
