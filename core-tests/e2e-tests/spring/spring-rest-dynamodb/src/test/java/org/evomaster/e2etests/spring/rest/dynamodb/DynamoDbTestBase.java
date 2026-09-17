package org.evomaster.e2etests.spring.rest.dynamodb;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.core.EMConfig;
import org.evomaster.e2etests.utils.RestTestBase;

import java.util.List;

/**
 * Shared setup and EvoMaster options for DynamoDB end-to-end tests.
 */
public abstract class DynamoDbTestBase extends RestTestBase {

    private static final String INSTRUMENT_DYNAMODB_OPTION = "instrumentMR_DYNAMODB";
    private static final String DYNAMODB_HEURISTICS_OPTION = "heuristicsForDynamoDb";
    private static final String DYNAMODB_EXTRACTION_OPTION = "extractDynamoDbExecutionInfo";
    private static final String DYNAMODB_GENERATION_OPTION = "generateDynamoDbData";

    /**
     * Starts an instrumented DynamoDB SUT for a concrete test class.
     *
     * @param controller embedded controller for the selected DynamoDB SUT
     * @throws Exception when the embedded controller cannot start
     */
    protected static void initDynamoDbTest(EmbeddedSutController controller) throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_DYNAMODB(true);
        RestTestBase.initClass(controller, config);
    }

    /**
     * Enables DynamoDB instrumentation and configures heuristic calculation.
     *
     * @param args EvoMaster command-line arguments
     * @param heuristicsEnabled whether DynamoDB heuristics should be enabled
     */
    protected void configureDynamoDbHeuristics(List<String> args, boolean heuristicsEnabled) {
        setOption(args, INSTRUMENT_DYNAMODB_OPTION, "true");
        setOption(args, DYNAMODB_HEURISTICS_OPTION, Boolean.toString(heuristicsEnabled));
    }

    /**
     * Configures DynamoDB failed-read extraction and generated initialization data.
     *
     * @param args EvoMaster command-line arguments
     * @param enabled whether insertion generation should be enabled
     */
    protected void configureDynamoDbInsertions(List<String> args, boolean enabled) {
        setOption(args, INSTRUMENT_DYNAMODB_OPTION, "true");
        setOption(args, DYNAMODB_HEURISTICS_OPTION, Boolean.toString(enabled));
        setOption(args, DYNAMODB_EXTRACTION_OPTION, Boolean.toString(enabled));
        setOption(args, DYNAMODB_GENERATION_OPTION, Boolean.toString(enabled));
    }
}
