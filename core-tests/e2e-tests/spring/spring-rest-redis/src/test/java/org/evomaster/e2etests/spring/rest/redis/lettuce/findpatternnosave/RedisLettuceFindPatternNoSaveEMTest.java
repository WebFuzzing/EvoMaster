package org.evomaster.e2etests.spring.rest.redis.lettuce.findpatternnosave;

import com.foo.spring.rest.redis.lettuce.findpatternnosave.RedisLettuceFindPatternNoSaveController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RedisLettuceFindPatternNoSaveEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_REDIS(true);
        config.setHeuristicsForRedis(true);
        config.setExtractRedisExecutionInfo(true);
        config.setGenerateRedisData(true);
        RestTestBase.initClass(new RedisLettuceFindPatternNoSaveController(), config);
    }

    @Test
    public void testFindPatternWithGeneratedDataEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RedisLettuceFindPatternNoSaveEM",
                "org.foo.spring.rest.redis.RedisLettuceFindPatternNoSaveEM",
                2000,
                true,
                (args) -> {
                    //maxEvaluations value of 10 to avoid timeout due to the RegexGene.
                    setOption(args, "maxEvaluations", "10");
                    setOption(args, "heuristicsForRedis", "true");
                    setOption(args, "instrumentMR_REDIS", "true");
                    setOption(args, "extractRedisExecutionInfo", "true");
                    setOption(args, "generateRedisData", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/redislettucefindpatternnosave/findPattern", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/redislettucefindpatternnosave/findPattern", null);
                },
                3);

    }
}
