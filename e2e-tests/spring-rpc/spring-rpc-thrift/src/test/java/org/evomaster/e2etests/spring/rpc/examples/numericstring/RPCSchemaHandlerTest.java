package org.evomaster.e2etests.spring.rpc.examples.numericstring;

import com.google.inject.Injector;
import org.evomaster.core.problem.rpc.RPCCallAction;
import org.evomaster.core.problem.rpc.service.RPCSampler;
import org.evomaster.core.search.action.Action;
import org.evomaster.core.search.gene.*;
import org.evomaster.core.search.gene.numeric.BigDecimalGene;
import org.evomaster.core.search.gene.optional.OptionalGene;
import org.evomaster.core.search.gene.string.NumericStringGene;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RPCSchemaHandlerTest extends NumericStringTestBase{

    /**
     * TO FINISH
     */
    @Test
    public void test(){

        List<String> args =  new ArrayList<>(Arrays.asList(
                "--createTests", "false",
                "--seed", "42",
                "--showProgress", "false",
                "--avoidNonDeterministicLogs", "true",
                "--sutControllerPort", "" + controllerPort,
                "--maxEvaluations", "" + 5,
                "--stoppingCriterion", "ACTION_EVALUATIONS",
                "--useTimeInFeedbackSampling" , "false"
        ));

        Injector injector = init(args);

        RPCSampler sampler = injector.getInstance(RPCSampler.class);
        List<Action> actions = sampler.seeAvailableActions();
        assertEquals(1, actions.size());
        assertTrue(actions.get(0) instanceof RPCCallAction);
        RPCCallAction rpcCallAction = (RPCCallAction) actions.get(0);
        assertEquals(1, rpcCallAction.seeTopGenes().size());
        ObjectGene objectGene = (ObjectGene) rpcCallAction.seeTopGenes().get(0);

        for (Gene g : objectGene.getFields()){
            assertTrue(g instanceof OptionalGene);
            assertTrue(((OptionalGene) g).getGene() instanceof NumericStringGene);
            NumericStringGene value = (NumericStringGene) ((OptionalGene) objectGene.getFields().get(0)).getGene();
            if (value.getName().equals("longValue")){
                BigDecimalGene bd = value.getNumber();
                assertFalse(bd.getFloatingPointMode());
                assertNotNull(bd.getMax());
                assertEquals(424242L, bd.getMax().longValue());
                assertFalse(bd.getMaxInclusive());
                assertEquals(424241L, bd.getMaximum().longValue());
                assertNotNull(bd.getMin());
                assertEquals(0L, bd.getMin().longValue());
                assertFalse(bd.getMinInclusive());
                assertEquals(1L, bd.getMinimum().longValue());
                assertEquals(6, bd.getPrecision());
                assertEquals(0, bd.getScale());
            } else if (value.getName().equals("intValue")){
                BigDecimalGene bd = value.getNumber();
                assertFalse(bd.getFloatingPointMode());
                assertNotNull(bd.getMax());
                assertEquals(0, bd.getMax().intValue());
                assertFalse(bd.getMaxInclusive());
                assertEquals(-1, bd.getMaximum().intValue());
                assertNotNull(bd.getMin());
                assertEquals(-9999, bd.getMin().intValue());
                assertTrue(bd.getMinInclusive());
                assertEquals(-9999, bd.getMinimum().intValue());
                assertEquals(4, bd.getPrecision());
                assertEquals(0, bd.getScale());
            } else if (value.getName().equals("doubleValue")){
                BigDecimalGene bd = value.getNumber();
                assertTrue(bd.getFloatingPointMode());
                assertEquals(4, bd.getPrecision());
                assertEquals(2, bd.getScale());
                assertNotNull(bd.getMax());
                assertEquals(99.99, bd.getMax().doubleValue());
                assertTrue(bd.getMaxInclusive());
                assertEquals(-99.99, bd.getMaximum().doubleValue());
                assertNotNull(bd.getMin());
                assertEquals(0.0, bd.getMin().doubleValue());
                assertTrue(bd.getMinInclusive());
                assertEquals(0.0, bd.getMinimum().doubleValue());

            }

        }

    }
}
