package org.evomaster.e2etests.spring.rest.redis.lettuce.richentity;

import com.foo.spring.rest.redis.lettuce.richentity.RedisLettuceRichEntityController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RedisLettuceRichEntityEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_REDIS(true);
        RestTestBase.initClass(new RedisLettuceRichEntityController(), config);
    }

    @Test
    public void testFindByIdEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RedisLettuceRichEntityEM",
                "org.foo.spring.rest.redis.RedisLettuceRichEntityEM",
                1000,
                true,
                (args) -> {
                    setOption(args, "heuristicsForRedis", "true");
                    setOption(args, "instrumentMR_REDIS", "true");
                    setOption(args, "generateRedisData", "true");
                    setOption(args, "extractRedisExecutionInfo", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    // found, with the id of the key and a populated nested object: every leaf of the
                    // entity, in the format Spring reads it back, must have been written for it to load
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/richentity/findById/{id}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 201, "/richentity/findById/{id}", null);
                    // found, with a non-empty list: it is stored as one field per element ("tags.[0]")
                    assertHasAtLeastOne(solution, HttpVerb.GET, 203, "/richentity/findById/{id}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/richentity/findById/{id}", null);
                },
                3);

    }
}
