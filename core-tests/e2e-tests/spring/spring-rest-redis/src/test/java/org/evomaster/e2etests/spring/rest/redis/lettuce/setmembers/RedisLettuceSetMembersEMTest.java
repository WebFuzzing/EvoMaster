package org.evomaster.e2etests.spring.rest.redis.lettuce.setmembers;

import com.foo.spring.rest.redis.lettuce.setmembers.RedisLettuceSetMembersController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RedisLettuceSetMembersEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_REDIS(true);
        RestTestBase.initClass(new RedisLettuceSetMembersController(), config);
    }

    @Test
    public void testSetMembersEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RedisLettuceSetMembersEM",
                "org.foo.spring.rest.redis.RedisLettuceSetMembersEM",
                1000,
                true,
                (args) -> {
                    setOption(args, "heuristicsForRedis", "true");
                    setOption(args, "instrumentMR_REDIS", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/redislettucesetmembers/set/{key}/{member}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/redislettucesetmembers/set/members/{key}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/redislettucesetmembers/set/members/{key}", null);

                },
                3);

    }
}
