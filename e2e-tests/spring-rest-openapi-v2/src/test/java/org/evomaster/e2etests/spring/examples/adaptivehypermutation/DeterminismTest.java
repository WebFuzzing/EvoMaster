package org.evomaster.e2etests.spring.examples.adaptivehypermutation;

import com.foo.rest.examples.spring.adaptivehypermutation.AHypermutationRestController;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import org.evomaster.core.problem.rest.RestActionBuilderV3;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.jetbrains.kotlin.com.intellij.util.containers.hash.LinkedHashMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeterminismTest extends AHypermuationTestBase {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testDeterminismOfLog(boolean enableConstraintHandling){

        OpenAPI schema = (new OpenAPIParser()).readLocation("swagger-ahm/ahm.json", null, null).getOpenAPI();
        isDeterminismConsumer( new ArrayList<>(), (args) -> {
            RestActionBuilderV3.INSTANCE.getModelsFromSwagger(schema, new LinkedHashMap<>(),
                    new RestActionBuilderV3.Options(false,enableConstraintHandling,false,0.0,0.0));
        });
    }


    /*
        WARNING

        giving up for now to try to get these tests to reliably pass.

        The problem is Jersey that does repeat POST commands at times (major bug!), and in general idempotent
        HTTP verbs. When command is repeated, if SUT is using a database, heuristics for SQL will be counted twice,
        leading to different results.

        So, for non-deterministic tests, should avoid using DBs.
        For E2E tests using DBs that are flaky, we might have to just resolve to give it more repeatiions with different
        seeds and/or larger budget
     */


    @Disabled("non-determinism may due to SQL execution failure or multiple retries of cleaning H2 database")
    @Test
    public void testNotDeterminismAHyMIO() {
        runAndCheckDeterminism(3000, (args)->{
            Solution<RestIndividual> solution = initAndRun(args);
            int count = countExpectedCoveredTargets(solution, new ArrayList<>());
            System.out.println(count);
        }, 5,  false);
    }

    @Disabled("non-determinism may due to SQL execution failure or multiple retries of cleaning H2 database")
    @Test
    public void testNotDeterminismMIO() {
        List<String> args =  new ArrayList<>(Arrays.asList(
                "--createTests", "false",
                "--seed", "42",
                "--showProgress", "false",
                "--avoidNonDeterministicLogs", "true",
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "" + 4000,
                "--stoppingCriterion", "FITNESS_EVALUATIONS",
                "--useTimeInFeedbackSampling" , "false"
        ));

        args.add("--probOfArchiveMutation");
        args.add("0.0");

        args.add("--weightBasedMutationRate");
        args.add("false");

        args.add("--adaptiveGeneSelectionMethod");
        args.add("NONE");

        args.add("--archiveGeneMutation");
        args.add("NONE");

        args.add("--enableTrackEvaluatedIndividual");
        args.add("false");

        isDeterminismConsumer(args, (x)->{
            initAndRun(args);
        }, 2, false);
    }

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new AHypermutationRestController(Arrays.asList("/api/bars/{a}")));
    }
}
