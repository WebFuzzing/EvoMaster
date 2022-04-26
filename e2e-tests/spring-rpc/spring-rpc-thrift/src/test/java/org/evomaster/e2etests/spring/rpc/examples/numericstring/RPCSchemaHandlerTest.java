package org.evomaster.e2etests.spring.rpc.examples.numericstring;

import com.google.inject.Injector;
import org.evomaster.core.problem.rpc.service.RPCEndpointsHandler;
import org.evomaster.core.problem.rpc.service.RPCSampler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RPCSchemaHandlerTest extends NumericStringTestBase{

    /**
     * TO FINISH
     */
    @Disabled
    @Test
    public void test(){

        List<String> args =  new ArrayList<>(Arrays.asList(
                "--createTests", "false",
                "--seed", "42",
                "--showProgress", "false",
                "--avoidNonDeterministicLogs", "true",
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "" + 5,
                "--stoppingCriterion", "FITNESS_EVALUATIONS",
                "--useTimeInFeedbackSampling" , "false"
        ));

        Injector injector = init(args);

        RPCSampler sampler = injector.getInstance(RPCSampler.class);
        RPCEndpointsHandler handler = injector.getInstance(RPCEndpointsHandler.class);
    }
}
