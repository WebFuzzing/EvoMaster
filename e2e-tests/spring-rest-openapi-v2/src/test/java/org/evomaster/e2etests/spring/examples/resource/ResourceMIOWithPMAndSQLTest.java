package org.evomaster.e2etests.spring.examples.resource;

import com.google.inject.Injector;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.problem.rest.SampleType;
import org.evomaster.core.problem.rest.resource.RestResourceCalls;
import org.evomaster.core.problem.rest.resource.RestResourceNode;
import org.evomaster.core.problem.rest.service.ResourceManageService;
import org.evomaster.core.problem.rest.service.ResourceRestMutator;
import org.evomaster.core.problem.rest.service.RestResourceFitness;
import org.evomaster.core.problem.rest.service.RestResourceStructureMutator;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.GeneFilter;
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResourceMIOWithPMAndSQLTest extends ResourceMIOHWTest{

    @Test
    public void testResourceMIOWithPMAndSQLHandling() {
        List<String> args = generalArgs(3, 42);
        hypmutation(args, false);
        adaptiveMutation(args, 0.0);
        defaultResourceConfig(args);
        args.add("--probOfApplySQLActionToCreateResources");
        args.add("1.0");
        // have the derived table at beginning
        args.add("--doesApplyNameMatching");
        args.add("true");
        args.add("--structureMutationProbability");
        args.add("0.0");

        Injector injector = init(args);
        initPartialOracles(injector);

        ResourceManageService rmanger = injector.getInstance(ResourceManageService.class);
        ResourceRestMutator mutator = injector.getInstance(ResourceRestMutator.class);
        RestResourceFitness ff = injector.getInstance(RestResourceFitness.class);
        RestResourceStructureMutator structureMutator = injector.getInstance(RestResourceStructureMutator.class);

        // probOfApplySQLActionToCreateResources = 0
        assertNotNull(rmanger.getSqlBuilder());

        assertEquals(keysToTemplate.keySet(), rmanger.getResourceCluster().keySet());


        for (String key : keysToTemplate.keySet()){
            RestResourceNode node = rmanger.getResourceNodeFromCluster(key);
            assertEquals(keysToTemplate.get(key), node.getTemplates().keySet(), key);
            // there is no derived table
            assertEquals(keysToTable.get(key), node.getResourceToTable().getDerivedMap().keySet(), key);
        }

        //checking representing mutable genes including SQL genes when resource-mio is enabled
        String raKey = "/api/rA";
        String raPostTemplate = "POST-POST";
        RestResourceNode node = rmanger.getResourceNodeFromCluster(raKey);
        RestResourceCalls rAcall = rmanger.genCalls(node, raPostTemplate, 10, false, true, false, false);
        assertEquals("POST",rAcall.getRestTemplate());
        assertEquals(raPostTemplate, rAcall.getSampledTemplate());
        assertEquals(2, rAcall.seeActions().size());
        assertEquals(1, rAcall.getDbActions().size());
        assertEquals(1, rAcall.getRestActions().size());
        // all SQL genes can be bound with POST, so the mutable SQL genes should be empty.
        assertEquals(0, rAcall.seeGenes(GeneFilter.ONLY_SQL).size());
        assertEquals(1, rAcall.seeGenes(GeneFilter.ALL).size());

        String raIdKey = "/api/rA/{rAId}";
        String raIdPostTemplate = "POST-GET";
        RestResourceNode raIdNode = rmanger.getResourceNodeFromCluster(raIdKey);
        RestResourceCalls rAIdcall = rmanger.genCalls(raIdNode, raIdPostTemplate, 10, false, true, false, false);
        assertEquals("GET",rAIdcall.getRestTemplate());
        assertEquals(raIdPostTemplate, rAIdcall.getSampledTemplate());
        assertEquals(2, rAIdcall.seeActions().size());
        assertEquals(1, rAIdcall.getDbActions().size());
        assertEquals(1, rAIdcall.getRestActions().size());
        checkingBinding(rAIdcall, "POST-GET", raIdKey,true);

        //exclude 'id' gene as it can be bound with GET
        assertEquals(2, rAIdcall.seeGenes(GeneFilter.ONLY_SQL).size());
        assertEquals(1, rAIdcall.seeGenes(GeneFilter.NO_SQL).size());

        //test binding after value mutator
        List<RestResourceCalls> calls = new ArrayList<>();
        calls.add(rAIdcall);
        RestIndividual raIdInd = new RestIndividual(calls, SampleType.SMART_RESOURCE, null, Collections.emptyList(), null, 1);
        EvaluatedIndividual<RestIndividual> rdIdEval = ff.calculateCoverage(raIdInd, Collections.emptySet());
        // mutable genes should be 3
        assertEquals(3, mutator.genesToMutation(raIdInd, rdIdEval, Collections.emptySet()).size());

        MutatedGeneSpecification mutatedSpec = new MutatedGeneSpecification();
        RestIndividual mutatedInd = mutator.mutate(rdIdEval, Collections.emptySet(), mutatedSpec);
        assertFalse(mutatedSpec.didStructureMutation());
        rAIdcall = mutatedInd.getResourceCalls().get(0);
        checkingBinding(rAIdcall, "POST-GET", raIdKey,true);

        //test stucturemutator and binding, rA/{rAId}, GET->POST-GET
        RestResourceCalls raGetIdCall = rmanger.genCalls(raIdNode, "GET", 10, false, true, false, false);
        calls.clear();
        calls.add(raGetIdCall);
        RestIndividual raGetIdInd = new RestIndividual(calls, SampleType.SMART_RESOURCE, null, Collections.emptyList(), null, 3);
        EvaluatedIndividual<RestIndividual> rdGetIdEval = ff.calculateCoverage(raGetIdInd, Collections.emptySet());
        RestIndividual raGetIdMutatedInd = (RestIndividual)raGetIdInd.copy();
        MutatedGeneSpecification mutatedSpecModify = new MutatedGeneSpecification();
        structureMutator.mutateRestResourceCalls(raGetIdMutatedInd, rdGetIdEval, RestResourceStructureMutator.MutationType.MODIFY, mutatedSpecModify);
        assertEquals(1, raGetIdMutatedInd.getResourceCalls().size());
        RestResourceCalls mutatedCall = raGetIdMutatedInd.getResourceCalls().get(0);
        assertEquals("POST-GET", mutatedCall.getSampledTemplate());
        checkingBinding(mutatedCall, "POST-GET", raIdKey,true);
    }
}
