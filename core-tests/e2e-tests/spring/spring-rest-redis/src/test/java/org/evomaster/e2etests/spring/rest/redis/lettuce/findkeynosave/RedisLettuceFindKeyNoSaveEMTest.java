package org.evomaster.e2etests.spring.rest.redis.lettuce.findkeynosave;

import com.foo.spring.rest.redis.lettuce.findkeynosave.findkey.RedisLettuceFindKeyNoSaveController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RedisLettuceFindKeyNoSaveEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_REDIS(true);
        config.setHeuristicsForRedis(true);
        config.setExtractRedisExecutionInfo(true);
        config.setGenerateRedisData(true);
        RestTestBase.initClass(new RedisLettuceFindKeyNoSaveController(), config);
    }

    @Test
    public void testFindKeyWithGeneratedDataEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RedisLettuceFindKeyNoSaveEM",
                "org.foo.spring.rest.redis.RedisLettuceFindKeyNoSaveEM",
                100,
                true,
                (args) -> {
                    setOption(args, "heuristicsForRedis", "true");
                    setOption(args, "instrumentMR_REDIS", "true");
                    setOption(args, "extractRedisExecutionInfo", "true");
                    setOption(args, "generateRedisData", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/redislettucefindkeynosave/findKey", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/redislettucefindkeynosave/findKey", null);
                },
                3);

    }
}
