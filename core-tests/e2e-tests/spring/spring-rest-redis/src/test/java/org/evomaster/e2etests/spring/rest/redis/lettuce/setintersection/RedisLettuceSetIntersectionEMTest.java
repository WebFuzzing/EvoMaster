package org.evomaster.e2etests.spring.rest.redis.lettuce.setintersection;

import com.foo.spring.rest.redis.lettuce.setintersection.RedisLettuceSetIntersectionController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RedisLettuceSetIntersectionEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_REDIS(true);
        RestTestBase.initClass(new RedisLettuceSetIntersectionController(), config);
    }

    @Test
    public void testSetIntersectionEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RedisLettuceSetIntersectionEM",
                "org.foo.spring.rest.redis.RedisLettuceSetIntersectionEM",
                2000,
                true,
                (args) -> {
                    setOption(args, "heuristicsForRedis", "true");
                    setOption(args, "instrumentMR_REDIS", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/redislettucesetintersection/set/{key}/{member}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/redislettucesetintersection/set/variable-intersection/{set1}/{set2}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/redislettucesetintersection/set/variable-intersection/{set1}/{set2}", null);

                },
                6);

    }
}
