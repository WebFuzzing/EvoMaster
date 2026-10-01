package org.evomaster.e2etests.spring.rest.redis.jedis.search;

import com.foo.spring.rest.redis.jedis.search.RedisJedisSearchController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RedisJedisSearchEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_REDIS(true);
        RestTestBase.initClass(new RedisJedisSearchController(), config);
    }

    @Test
    public void testSearchEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RedisJedisSearchEM",
                "org.foo.spring.rest.redis.RedisJedisSearchEM",
                2000,
                true,
                (args) -> {
                    setOption(args, "heuristicsForRedis", "true");
                    setOption(args, "instrumentMR_REDIS", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/redisjedissearch/person/{name}/{age}/{street}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/redisjedissearch/search/name/{name}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/redisjedissearch/search/name/{name}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/redisjedissearch/search/street/{street}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/redisjedissearch/search/street/{street}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/redisjedissearch/search/age/{min}/{max}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/redisjedissearch/search/age/{min}/{max}", null);
                },
                3);

    }
}
