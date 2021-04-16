package org.evomaster.e2etests.spring.examples.resource;

import com.google.inject.Injector;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.problem.rest.SampleType;
import org.evomaster.core.problem.rest.resource.RestResourceCalls;
import org.evomaster.core.problem.rest.resource.RestResourceNode;
import org.evomaster.core.problem.rest.service.ResourceManageService;
import org.evomaster.core.problem.rest.service.ResourceRestMutator;
import org.evomaster.core.problem.rest.service.RestResourceFitness;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.GeneFilter;
import org.evomaster.core.search.gene.Gene;
import org.evomaster.core.search.gene.ObjectGene;
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResourceHypermutationTest extends ResourceMIOHWTest{

    @Test
    public void testResourceHypermutation(){

        List<String> args = generalArgs(3, 42);
        hypmutation(args, true);
        adaptiveMutation(args, 0.0);
        defaultResourceConfig(args);
        args.add("--probOfApplySQLActionToCreateResources");
        args.add("0.0");
        args.add("--doesApplyNameMatching");
        args.add("false");
        args.add("--structureMutationProbability");
        args.add("0.0");

        Injector injector = init(args);
        initPartialOracles(injector);

        ResourceManageService rmanger = injector.getInstance(ResourceManageService.class);
        ResourceRestMutator mutator = injector.getInstance(ResourceRestMutator.class);
        RestResourceFitness ff = injector.getInstance(RestResourceFitness.class);

        String raIdkey = "/api/rA/{rAId}";
        String rdkey = "/api/rd";

        RestResourceNode raIdNode = rmanger.getResourceNodeFromCluster(raIdkey);
        RestResourceCalls rAIdcall = rmanger.genCalls(raIdNode, "POST-GET", 10, false, true, false, false);
        RestResourceNode rdNode = rmanger.getResourceNodeFromCluster(rdkey);
        RestResourceCalls rdcall = rmanger.genCalls(rdNode, "POST-POST", 8, false, true, false, false);

        List<RestResourceCalls> calls = Arrays.asList(rAIdcall, rdcall);
        RestIndividual twoCalls = new RestIndividual(calls, SampleType.SMART_RESOURCE, null, Collections.emptyList(), null, 1);
        EvaluatedIndividual<RestIndividual> twoCallsEval = ff.calculateCoverage(twoCalls, Collections.emptySet());
        // only two ObjectGenes should be candidates
        assertEquals(2, mutator.genesToMutation(twoCalls, twoCallsEval, Collections.emptySet()).size());

        MutatedGeneSpecification spec = new MutatedGeneSpecification();
        RestIndividual mutatedTwoCalls = mutator.mutate(twoCallsEval, Collections.emptySet(), spec);
        assertEquals(0, spec.mutatedDbGeneInfo().size());
        // with specified seed, this should be determinate
        assertEquals(1, spec.mutatedGeneInfo().size());

        Gene rdObj = rdcall.seeGenes(GeneFilter.NO_SQL).stream().findFirst().orElse(null);
        Gene mrdObj = mutatedTwoCalls.getResourceCalls().get(1).seeGenes(GeneFilter.NO_SQL).stream().findFirst().orElse(null);
        assert(rdObj instanceof ObjectGene);
        assert(mrdObj instanceof ObjectGene);
        //two fields are mutated
        assertEquals(2, IntStream.range(0, ((ObjectGene)rdObj).getFields().size()).filter(i->
                !((ObjectGene) rdObj).getFields().get(i).containsSameValueAs(((ObjectGene) mrdObj).getFields().get(i))
        ).count());

        //value should be bound
        mutatedTwoCalls.getResourceCalls().forEach(c->
                checkingBinding(c, c.getSampledTemplate(), c.getResourceNodeKey(), false));
    }
}
