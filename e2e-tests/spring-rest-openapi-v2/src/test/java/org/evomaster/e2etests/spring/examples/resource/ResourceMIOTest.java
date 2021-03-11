package org.evomaster.e2etests.spring.examples.resource;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
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
import org.evomaster.core.search.gene.ObjectGene;
import org.evomaster.core.search.impact.impactinfocollection.ImpactsOfIndividual;
import org.evomaster.core.search.service.Archive;
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;


public class ResourceMIOTest extends ResourceTestBase {

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

        List<String> args = generalArgs(1, 42);
        hypmutation(args, false);
        adaptiveMutation(args, 0.0);
        defaultResourceConfig(args);

        Injector injector = init(args);
        initPartialOracles(injector);

        ResourceManageService rmanger = injector.getInstance(ResourceManageService.class);

        // probOfApplySQLActionToCreateResources = 0
        assertNotNull(rmanger.getSqlBuilder());

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
        checkingBinding(rAIdcall, raIdPostTemplate, raIdKey, false);
    }

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
        checkingBinding(mutatedCall, "POST-GET", raIdKey,true);
    }


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
        RestResourceCalls rAIdcall = rmanger.genCalls(raIdNode, "POST-GET", 10, false, true, false);
        RestResourceNode rdNode = rmanger.getResourceNodeFromCluster(rdkey);
        RestResourceCalls rdcall = rmanger.genCalls(rdNode, "POST-POST", 8, false, true, false);

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

    @Test
    public void testResourceWithSQLAndHypermutation(){

        List<String> args = generalArgs(3, 42);
        hypmutation(args, true);
        adaptiveMutation(args, 0.0);
        defaultResourceConfig(args);
        //always employ SQL to create POST
        args.add("--probOfApplySQLActionToCreateResources");
        args.add("1.0");
        args.add("--doesApplyNameMatching");
        args.add("true");
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
        RestResourceCalls rAIdcall = rmanger.genCalls(raIdNode, "POST-GET", 10, false, true, false);
        RestResourceNode rdNode = rmanger.getResourceNodeFromCluster(rdkey);
        RestResourceCalls rdcall = rmanger.genCalls(rdNode, "POST-POST", 8, false, true, false);

        List<RestResourceCalls> calls = Arrays.asList(rAIdcall, rdcall);
        RestIndividual twoCalls = new RestIndividual(calls, SampleType.SMART_RESOURCE, null, Collections.emptyList(), null, 1);
        EvaluatedIndividual<RestIndividual> twoCallsEval = ff.calculateCoverage(twoCalls, Collections.emptySet());

        //there should not exist impactInfo
        assertNull(twoCallsEval.getImpactInfo());

        MutatedGeneSpecification spec = new MutatedGeneSpecification();
        RestIndividual mutatedTwoCalls = mutator.mutate(twoCallsEval, Collections.emptySet(), spec);
        // with specified seed, this should be determinate
        assertEquals(1, spec.mutatedDbGeneInfo().size());
        assertEquals(1, spec.mutatedGeneInfo().size());
        mutatedTwoCalls.getResourceCalls().forEach(c->
                checkingBinding(c, c.getSampledTemplate(), c.getResourceNodeKey(), true)
        );

    }

    @Test
    public void testResourceWithSQLAndAHW(){
        List<String> args = generalArgs(3, 42);
        hypmutation(args, true);
        adaptiveMutation(args, 0.5);
        defaultResourceConfig(args);
        //always employ SQL to create POST
        args.add("--probOfApplySQLActionToCreateResources");
        args.add("1.0");
        args.add("--doesApplyNameMatching");
        args.add("true");
        args.add("--structureMutationProbability");
        args.add("0.0");

        //test impactinfo
        Injector injector = init(args);
        initPartialOracles(injector);

        ResourceManageService rmanger = injector.getInstance(ResourceManageService.class);
        ResourceRestMutator mutator = injector.getInstance(ResourceRestMutator.class);
        RestResourceFitness ff = injector.getInstance(RestResourceFitness.class);
        Archive<RestIndividual> archive = injector.getInstance(Key.get(
                new TypeLiteral<Archive<RestIndividual>>() {}));

        String raIdkey = "/api/rA/{rAId}";
        String rdkey = "/api/rd";

        RestResourceNode raIdNode = rmanger.getResourceNodeFromCluster(raIdkey);
        RestResourceCalls rAIdcall = rmanger.genCalls(raIdNode, "POST-GET", 10, false, true, false);
        RestResourceNode rdNode = rmanger.getResourceNodeFromCluster(rdkey);
        RestResourceCalls rdcall = rmanger.genCalls(rdNode, "POST-POST", 8, false, true, false);

        List<RestResourceCalls> calls = Arrays.asList(rAIdcall, rdcall);
        RestIndividual twoCalls = new RestIndividual(calls, SampleType.SMART_RESOURCE, null, Collections.emptyList(), null, 1);
        EvaluatedIndividual<RestIndividual> twoCallsEval = ff.calculateCoverage(twoCalls, Collections.emptySet());

        ImpactsOfIndividual impactInd = twoCallsEval.getImpactInfo();
        // impactinfo should be initialized
        assertNotNull(impactInd);
        assertEquals(0, impactInd.getSizeOfActionImpacts(true));
        assertEquals(4, impactInd.getSizeOfActionImpacts(false));
        //tracking is null if the eval is generated by sampler
        assertNull(twoCallsEval.getTracking());


        EvaluatedIndividual<RestIndividual> twoCallsEvalNoWorse = mutator.mutateAndSave(1, twoCallsEval, archive);
        //history should affect both of evaluated individual
        assertNotNull(twoCallsEval.getTracking());
        assertNotNull(twoCallsEvalNoWorse.getTracking());
        assertEquals(2, twoCallsEval.getTracking().getHistory().size());
        assertEquals(2, twoCallsEvalNoWorse.getTracking().getHistory().size());
        //this should be determinate with a specific seed
        assert(twoCallsEvalNoWorse.getByIndex(twoCallsEvalNoWorse.getIndex()).getEvaluatedResult().isImpactful());

    }

    private void checkingBinding(RestResourceCalls call, String template, String nodeKey, Boolean withSQL){
        if (nodeKey.endsWith("Id}")){
            if (template.equals("POST-GET")){
                if (withSQL){
                    Gene rdIdInRest = call.getRestActions().get(0).seeGenes().stream().findFirst().orElse(null);
                    Gene rdIdInDB = getGeneByName(Objects.requireNonNull(call.getDbActions().stream().findFirst().orElse(null)),"ID");
                    // test binding between DB and RestAction
                    assertEquals(rdIdInRest.getValueAsRawString(), rdIdInDB.getValueAsRawString());
                }else {
                    Gene bodyInPOST = getGeneByName(call.getRestActions().get(0), "id");
                    Gene rdIdInGet = call.getRestActions().get(1).seeGenes().stream().findFirst().orElse(null);
                    assertEquals(bodyInPOST.getValueAsRawString(), rdIdInGet.getValueAsRawString());
                }
            }
        }else{
            if (template.equals("POST-POST")){
                if (withSQL){
                    Gene rdIdInRest = getGeneByName(call.getRestActions().get(0), "id");
                    Gene rdIdInDB = getGeneByName(Objects.requireNonNull(call.getDbActions().stream().findFirst().orElse(null)),"ID");
                    // test binding between DB and RestAction
                    assertEquals(rdIdInRest.getValueAsRawString(), rdIdInDB.getValueAsRawString());
                }else {
                    Gene bodyInPOST = getGeneByName(call.getRestActions().get(0), "id");
                    Gene rdIdInGet = getGeneByName(call.getRestActions().get(0), "id");
                    assertEquals(bodyInPOST.getValueAsRawString(), rdIdInGet.getValueAsRawString());
                }
            }
        }
    }

    private Gene getGeneByName(Action action, String name){
        return action.seeGenes().stream().flatMap(s-> s.flatView(gene -> false).stream()).filter(s-> s.getName().equalsIgnoreCase(name)).findAny()
                .orElse(null);
    }

    private List<String> generalArgs(int budget, int seed){
        return new ArrayList<>(
                Arrays.asList(
                        "--createTests", "false",
                        "--seed", ""+seed,
                        "--useTimeInFeedbackSampling", "false",
                        "--sutControllerPort", "" + controllerPort,
                        "--maxActionEvaluations", "" + budget,
                        "--stoppingCriterion", "FITNESS_EVALUATIONS",
                        //there some bugs here
                        "--baseTaintAnalysisProbability", "0.0"
                )
        );
    }

    private void hypmutation(List<String> args, Boolean enable){
        //disable hypermutation
        args.add("--weightBasedMutationRate");
        args.add(""+enable);
    }

    private void adaptiveMutation(List<String> args, double apc){
        args.add("--enableTrackEvaluatedIndividual");
        if (apc > 0.0)args.add("true"); else args.add("false");
        args.add("--adaptiveGeneSelectionMethod");
        if (apc > 0.0) args.add("APPROACH_IMPACT"); else args.add("NONE");
        args.add("--archiveGeneMutation");
        if (apc > 0.0) args.add("SPECIFIED_WITH_SPECIFIC_TARGETS"); else args.add("NONE");
        args.add("--probOfArchiveMutation");
        args.add(""+apc);
    }

    private void defaultResourceConfig(List<String> args){

        args.add("--resourceSampleStrategy");
        args.add("ConArchive");
        args.add("--probOfSmartSampling");
        args.add("1.0");
        args.add("--probOfEnablingResourceDependencyHeuristics");
        args.add("1.0");
    }

}
