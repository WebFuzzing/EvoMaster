package org.evomaster.e2etests.spring.examples.hypermutation;

import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact;
import org.evomaster.e2etests.spring.examples.SpringTestBase;

import java.util.Map;

public class HypermutationTestBase extends SpringTestBase {


    public boolean check(EvaluatedIndividual<RestIndividual> ind, String action,int max){
        String xGeneId = "POST:/api/highweight/"+action+"/{x}::DisruptiveGene>x";
        String yGeneId = "POST:/api/highweight/"+action+"/{x}::y";
        String zGeneId = "POST:/api/highweight/"+action+"/{x}::HighWeightDto>body";

        boolean result = true;
        for (Map<String, GeneImpact> a : ind.getActionGeneImpact(true)){
            if (a.values().stream().noneMatch(s-> s.getId().contains(xGeneId)))
                continue;


            int x = a.values().stream().filter(s-> s.getId().contains(xGeneId)).findFirst().get().getTimesToManipulate();
            int y = a.values().stream().filter(s-> s.getId().contains(yGeneId)).findFirst().get().getTimesToManipulate();
            int z = a.values().stream().filter(s-> s.getId().contains(zGeneId)).findFirst().get().getTimesToManipulate();

            /*
                TODO what is tested here, and why it should be like that, needs explanations
             */

            if (max == 0){
                result = result && z > x && z > y;
            }else if (max == 1){
                result = result && x > z && x > y;
            }else{
                throw new IllegalArgumentException("invalid max");
            }

            if(!result){
                return false;
            }
        }

        return result;
    }
}
