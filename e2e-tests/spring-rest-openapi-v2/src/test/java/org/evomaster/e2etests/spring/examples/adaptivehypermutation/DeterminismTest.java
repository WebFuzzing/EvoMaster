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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeterminismTest extends AHypermuationTestBase {

    @Test
    public void testDeterminismOfLog(){

        OpenAPI schema = (new OpenAPIParser()).readLocation("swagger-ahm/ahm.json", null, null).getOpenAPI();
        isDeterminismConsumer( new ArrayList<>(), (args) -> {
            RestActionBuilderV3.INSTANCE.getModelsFromSwagger(schema, new LinkedHashMap<>());
        });
    }

    //NotDeterminism can be identified by AHY-MIO with 3k budget
    @Test
    public void testNotDeterminismAHyMIO() throws Throwable {
        handleFlaky(()->{
            runAndCheckDeterminism(3000, (args)->{
                Solution<RestIndividual> solution = initAndRun(args);
                int count = countExpectedCoveredTargets(solution, new ArrayList<>());
                System.out.println(count);
            }, 2,  false);
        });

    }

    //NotDeterminism can be identified by MIO with 4k budget
    @Test
    public void testNotDeterminismMIO() throws Throwable {
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

        handleFlaky(()->{
            isDeterminismConsumer(args, (x)->{
                initAndRun(args);
            }, 2, false);
        });
    }

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new AHypermutationRestController(Arrays.asList("/api/bars/{a}")));
    }
}
