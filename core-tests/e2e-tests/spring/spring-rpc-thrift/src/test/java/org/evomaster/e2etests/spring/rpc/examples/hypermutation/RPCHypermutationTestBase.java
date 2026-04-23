package org.evomaster.e2etests.spring.rpc.examples.hypermutation;

import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;

import java.util.Map;

/**
 * created by manzhang on 2023/11/13
 */
public abstract class RPCHypermutationTestBase extends SpringRPCTestBase {


    public boolean check(Solution<RPCIndividual> solution, String action, int max){
        int all = solution.getIndividuals().size();
        int satisfied = (int) solution.getIndividuals().stream().filter(s-> check(s, action,max)).count();
        return ((satisfied * 1.0 / all) > 0.9) || (all - satisfied == 1);
    }

    private boolean check(EvaluatedIndividual<RPCIndividual> ind, String action, int max){
        String xGeneId = "com.foo.rpc.examples.spring.hypermutation.HypermutationService$Iface:"+action+"::arg0::0";
        String yGeneId = "com.foo.rpc.examples.spring.hypermutation.HypermutationService$Iface:"+action+"::arg1::1";
        String zGeneId = "com.foo.rpc.examples.spring.hypermutation.HypermutationService$Iface:"+action+"::OptionalGene>com.foo.rpc.examples.spring.hypermutation.HighWeightDto>arg2::2";

        boolean result = true;
        for (Map<String, GeneImpact> a : ind.getActionGeneImpact(true)){
            if (a.values().stream().noneMatch(s-> s.getId().contains(xGeneId)))
                continue;


            int x = a.values().stream().filter(s-> s.getId().contains(xGeneId)).findFirst().get().getTimesToManipulate();
            int y = a.values().stream().filter(s-> s.getId().contains(yGeneId)).findFirst().get().getTimesToManipulate();
            int z = a.values().stream().filter(s-> s.getId().contains(zGeneId)).findFirst().get().getTimesToManipulate();

            if (max == 0){
                result = result && z > x && z > y;
            }else if (max == 1){
                result = result && x > z && x > y;
            }else{
                throw new IllegalArgumentException("invalid max");
            }

        }

        return result;
    }
}
