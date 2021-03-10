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
import org.evomaster.core.search.Action;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.GeneFilter;
import org.evomaster.core.search.gene.Gene;
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


public class ResourceTest extends ResourceTestBase {

    final Map<String, Set<String>> keysToTable = new HashMap<String, Set<String>>(){{
        put("/api/rA", new HashSet<>(Arrays.asList("RA")));
        put("/api/rA/{rAId}", new HashSet<>(Arrays.asList("RA")));
        put("/api/rd", new HashSet<>(Arrays.asList("RD")));
        put("/api/rd/{rdId}",new HashSet<>(Arrays.asList("RD")));
        put("/api/rpR",new HashSet<>(Arrays.asList("RPR")));
        put("/api/rpR/{rpRId}",new HashSet<>(Arrays.asList("RPR")));
    }};

    final Map<String, Set<String>> keysToTemplate = new HashMap<String, Set<String>>(){{
        put("/api/rA", new HashSet<>(Arrays.asList("POST-POST", "POST")));
        put("/api/rA/{rAId}", new HashSet<>(Arrays.asList("POST-GET","GET")));
        put("/api/rd", new HashSet<>(Arrays.asList("POST-POST", "POST")));
        put("/api/rd/{rdId}",new HashSet<>(Arrays.asList("POST-GET","GET")));
        put("/api/rpR",new HashSet<>(Arrays.asList("POST-POST", "POST")));
        put("/api/rpR/{rpRId}",new HashSet<>(Arrays.asList("POST-GET","GET")));
    }};

    @Test
    public void testResourceMIO() {

        List<String> args = generalArgs(1);
        disableAHW(args);
        defaultResourceConfig(args);

        Injector injector = init(args);
        initPartialOracles(injector);

        ResourceManageService rmanger = injector.getInstance(ResourceManageService.class);

        // probOfApplySQLActionToCreateResources = 0
        assertNull(rmanger.getSqlBuilder());

        assertEquals(keysToTemplate.keySet(), rmanger.getResourceCluster().keySet());


        for (String key : keysToTemplate.keySet()){
            RestResourceNode node = rmanger.getResourceNodeFromCluster(key);
            assertEquals(keysToTemplate.get(key), node.getTemplates().keySet(), key);
            // there is no derived table
            assertEquals(0, node.getResourceToTable().getDerivedMap().size(), key);
        }

        //checking representing mutable genes when resource-mio is enabled
        String raKey = "/api/rA";
        String raPostTemplate = "POST-POST";
        RestResourceNode node = rmanger.getResourceNodeFromCluster(raKey);
        RestResourceCalls rAcall = rmanger.genCalls(node, raPostTemplate, 10, false, true, false);
        assertEquals(2, rAcall.seeActions().size());
        assertEquals(1, rAcall.seeGenes(GeneFilter.ALL).size());

        String raIdKey = "/api/rA/{rAId}";
        String raIdPostTemplate = "POST-GET";
        RestResourceNode idNode = rmanger.getResourceNodeFromCluster(raIdKey);
        RestResourceCalls rAIdcall = rmanger.genCalls(idNode, raIdPostTemplate, 10, false, true, false);
        assertEquals(2, rAIdcall.seeActions().size());
        // {rAId} should not be included because it can be bound with ObjectGene of POST
        assertEquals(1, rAIdcall.seeGenes(GeneFilter.ALL).size());
    }

    @Test
    public void testResourceMIOWithPMAndSQLHandling() {
        List<String> args = generalArgs(5);
        disableAHW(args);
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
        RestResourceCalls rAcall = rmanger.genCalls(node, raPostTemplate, 10, false, true, false);
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
        RestResourceCalls rAIdcall = rmanger.genCalls(raIdNode, raIdPostTemplate, 10, false, true, false);
        assertEquals("GET",rAIdcall.getRestTemplate());
        assertEquals(raIdPostTemplate, rAIdcall.getSampledTemplate());
        assertEquals(2, rAIdcall.seeActions().size());
        assertEquals(1, rAIdcall.getDbActions().size());
        assertEquals(1, rAIdcall.getRestActions().size());
        Gene rdIdInRest = rAIdcall.getRestActions().stream().findFirst().orElse(null).seeGenes().stream().findFirst().orElse(null);
        Gene rdIdInDB = getGeneByName(Objects.requireNonNull(rAIdcall.getDbActions().stream().findFirst().orElse(null)),"ID");
        // test binding between DB and RestAction
        assertEquals(rdIdInRest.getValueAsRawString(), rdIdInDB.getValueAsRawString());
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

        rAIdcall = mutatedInd.getResourceCalls().get(0);
        rdIdInRest = rAIdcall.getRestActions().stream().findFirst().orElse(null).seeGenes().stream().findFirst().orElse(null);
        rdIdInDB = getGeneByName(Objects.requireNonNull(rAIdcall.getDbActions().stream().findFirst().orElse(null)),"ID");
        // test binding between DB and RestAction
        assertEquals(rdIdInRest.getValueAsRawString(), rdIdInDB.getValueAsRawString());

        //test binding after stucturemutator, rA, GET->POST-GET
        RestResourceCalls raGetIdCall = rmanger.genCalls(raIdNode, "GET", 10, false, true, false);
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

    }


    private Gene getGeneByName(Action action, String name){
        return action.seeGenes().stream().filter(s-> s.getName().equalsIgnoreCase(name)).findAny()
                .orElse(null);
    }

    private List<String> generalArgs(int budget){
        return new ArrayList<>(
                Arrays.asList(
                        "--createTests", "false",
                        "--seed", "" + defaultSeed,
                        "--useTimeInFeedbackSampling", "false",
                        "--sutControllerPort", "" + controllerPort,
                        "--maxActionEvaluations", "" + budget,
                        "--stoppingCriterion", "FITNESS_EVALUATIONS",
                        //there some bugs here
                        "--baseTaintAnalysisProbability", "0.0"
                )
        );
    }

}
