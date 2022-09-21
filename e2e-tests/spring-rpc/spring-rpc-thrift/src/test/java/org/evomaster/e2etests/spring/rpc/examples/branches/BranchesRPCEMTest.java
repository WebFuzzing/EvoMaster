package org.evomaster.e2etests.spring.rpc.examples.branches;

import com.foo.rpc.examples.spring.branches.BranchesRPCController;
import org.evomaster.core.problem.rpc.RPCCallAction;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.problem.util.ParamUtil;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.core.search.gene.Gene;
import org.evomaster.core.search.gene.numeric.IntegerGene;
import org.evomaster.core.search.gene.ObjectGene;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BranchesRPCEMTest extends SpringRPCTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        BranchesRPCController controller = new BranchesRPCController();
        SpringRPCTestBase.initClass(controller);
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "BranchesRPCEM",
                "org.foo.BranchesRPCEM",
                5000,
                (args) -> {

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    int n =solution.getIndividuals().stream().map(EvaluatedIndividual<RPCIndividual>::getIndividual)
                            .flatMapToInt(i-> i.seeAllActions().stream().filter(a->
                                    {
                                        RPCCallAction action = (RPCCallAction) a;
                                        if (action.getResponse() == null) return false;
                                        Gene g = ParamUtil.Companion.getValueGene(action.getResponse().getGene());
                                        if (g instanceof ObjectGene){
                                            return ((ObjectGene)g).getFields().size() == 1 && ((ObjectGene)g).getFields().get(0) instanceof IntegerGene;
                                        }else return false;
                                    }
                            ).mapToInt(a->  ((IntegerGene)((ObjectGene)ParamUtil.Companion.getValueGene(((RPCCallAction)a).getResponse().getGene())).getFields().get(0)).getValue()))
                            .distinct()
                            .sorted().toArray().length;

//                    assertTrue(n >= 7);
                    assertEquals(9, n);
                });
    }
}
