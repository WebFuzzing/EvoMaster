package org.evomaster.e2etests.spring.rest.dynamodb;

import com.foo.spring.rest.dynamodb.PlayerCollectionsController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies end-to-end extraction, gene construction, insertion, and generated-test execution
 * for DynamoDB map, list, string-set, number-set, and null attributes.
 */
public class DynamoDbCollectionsEMTest extends DynamoDbTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        initDynamoDbTest(new PlayerCollectionsController());
    }

    @Test
    public void testCollectionAndSetTemplateInsertionsCompileAndRun() throws Throwable {
        String outputFolderName = "DynamoDbCollectionsEM";
        String fullClassName = "org.foo.spring.rest.dynamodb.DynamoDbCollectionsEM";

        runTestHandlingFlakyAndCompilation(
                outputFolderName,
                fullClassName,
                100,
                true,
                args -> {
                    List<String> baselineArgs = new ArrayList<>(args);
                    configureDynamoDbInsertions(baselineArgs, false);
                    setOption(baselineArgs, "createTests", "false");
                    Solution<RestIndividual> baseline = initAndRun(baselineArgs);
                    assertHasAtLeastOne(baseline, HttpVerb.GET, 404, "/collections/players/{country}", null);
                    assertNone(baseline, HttpVerb.GET, 200, "/collections/players/{country}", null);

                    List<String> insertionArgs = new ArrayList<>(args);
                    configureDynamoDbInsertions(insertionArgs, true);
                    Solution<RestIndividual> generated = initAndRun(insertionArgs);
                    assertHasAtLeastOne(generated, HttpVerb.GET, 200, "/collections/players/{country}",
                            "COLLECTION_TEMPLATE_MATCHED");
                    assertTrue(generated.hasAnyDynamoDbAction());

                    assertTextInTests(outputFolderName, fullClassName, ".ddbJson(\"profile\",");
                    assertTextInTests(outputFolderName, fullClassName, ".ddbJson(\"tags\",");
                    assertTextInTests(outputFolderName, fullClassName, ".ddbJson(\"roles\",");
                    assertTextInTests(outputFolderName, fullClassName, ".ddbJson(\"jerseyNumbers\",");
                    assertTextInTests(outputFolderName, fullClassName, ".ddbJson(\"retiredAt\",");
                    assertTextInTests(outputFolderName, fullClassName, ".ddbJson(\"emptyTags\",");
                },
                5);
    }
}
