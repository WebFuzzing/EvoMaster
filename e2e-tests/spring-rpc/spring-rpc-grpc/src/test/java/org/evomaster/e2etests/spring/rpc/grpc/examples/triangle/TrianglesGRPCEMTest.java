package org.evomaster.e2etests.spring.rpc.grpc.examples.triangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.foo.rpc.grpc.examples.spring.triangle.CheckTriangleGRPCServiceController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.evomaster.core.problem.rpc.RPCCallAction;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.core.search.gene.Gene;
import org.evomaster.core.search.gene.ObjectGene;
import org.evomaster.core.search.gene.optional.OptionalGene;
import org.evomaster.e2etests.spring.rpc.grpc.examples.GRPCServerTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TrianglesGRPCEMTest extends GRPCServerTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        CheckTriangleGRPCServiceController controller = new CheckTriangleGRPCServiceController();
        GRPCServerTestBase.initClass(controller);
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "TrianglesGRPCEM",
                "org.foo.grpc.TrianglesEM",
                5000,
                (args) -> {

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    List<RPCIndividual> collected = solution.getIndividuals().stream().map(EvaluatedIndividual<RPCIndividual>::getIndividual).collect(Collectors.toList());

                    List<String> intReturnResults = new ArrayList<>();
                    for (RPCIndividual individual : collected) {
                        RPCCallAction actionInd = (RPCCallAction) individual.seeAllActions().get(0);
                        Gene geneInd = actionInd.getResponse().getGene();

                        // getting what would be the IntegerGene value, which matches the classify function result
                        String intResultInd = ((ObjectGene) ((OptionalGene)geneInd).getGene()).getFixedFields().get(0).getValueAsRawString();
                        intReturnResults.add(intResultInd);
                    }

                    /*
                     might differ between executions, but in my tests got:
                        - collected.size() = 5
                        - intReturnResults.stream().distinct().toArray().length = 2

                     which shows some results are repeated.
                     In the classify code, the 0 return value is the only one repeated in the first and max ifs,
                     so it shouldn't come down from 5 to 2
                     */
                    assertEquals(collected.size(), intReturnResults.stream().distinct().toArray().length);
                });
    }
}
