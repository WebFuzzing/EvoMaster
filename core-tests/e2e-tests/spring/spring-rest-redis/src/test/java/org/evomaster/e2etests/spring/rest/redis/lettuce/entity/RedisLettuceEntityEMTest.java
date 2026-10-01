package org.evomaster.e2etests.spring.rest.redis.lettuce.entity;

import com.foo.spring.rest.redis.lettuce.entity.RedisLettuceEntityController;
import com.foo.spring.rest.redis.lettuce.findhash.RedisLettuceFindHashController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RedisLettuceEntityEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_REDIS(true);
        RestTestBase.initClass(new RedisLettuceEntityController(), config);
    }

    @Test
    public void testFindByIdEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RedisLettuceEntityEM",
                "org.foo.spring.rest.redis.RedisLettuceEntityEM",
                1000,
                true,
                (args) -> {
                    setOption(args, "heuristicsForRedis", "true");
                    setOption(args, "instrumentMR_REDIS", "true");
                    setOption(args, "generateRedisData", "true");
                    setOption(args, "extractRedisExecutionInfo", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/entity/findById/{id}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/entity/findById/{id}", null);
                },
                3);

    }
}
