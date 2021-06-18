package org.evomaster.e2etests.spring.examples.hypermutation;

import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Individual;
import org.evomaster.core.search.gene.Gene;
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact;
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils;
import org.evomaster.e2etests.spring.examples.SpringTestBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HypermutationTestBase extends SpringTestBase {


    public boolean check(EvaluatedIndividual<RestIndividual> ind, String action,int max){
        List<GeneImpact> allImpacts = new ArrayList<>();
        for (Gene g: ind.getIndividual().seeGenes(Individual.GeneFilter.ALL)){
            String geneId = ImpactUtils.Companion.generateGeneId(ind.getIndividual(), g);
            allImpacts.addAll(ind.getGeneImpact(geneId));
        }
        List<Integer> x = allImpacts.stream().filter(s-> s.getId().contains("POST:/api/highweight/"+action+"::DisruptiveGene>x")).map(s-> s.getTimesToManipulate()).collect(Collectors.toList());
        int maxX = Collections.max(x);
        List<Integer> y = allImpacts.stream().filter(s-> s.getId().contains("POST:/api/highweight/"+action+"::y")).map(s-> s.getTimesToManipulate()).collect(Collectors.toList());
        int maxY = Collections.max(y);
        List<Integer> z = allImpacts.stream().filter(s-> s.getId().contains("POST:/api/highweight/"+action+"::HighWeightDto>body")).map(s-> s.getTimesToManipulate()).collect(Collectors.toList());
        int maxZ = Collections.max(z);
        // z should be mutated more times than x and y

        if (max == 0){
            return z.stream().allMatch(s-> s> maxX && s>maxY);
        }else if (max == 1){
            return x.stream().allMatch(s-> s> maxZ && s> maxY);
        }else{
            throw new IllegalArgumentException("invalid max");
        }
    }
}
