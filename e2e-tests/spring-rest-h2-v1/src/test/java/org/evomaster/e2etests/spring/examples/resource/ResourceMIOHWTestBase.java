package org.evomaster.e2etests.spring.examples.resource;

import org.evomaster.core.problem.rest.resource.RestResourceCalls;
import org.evomaster.core.search.action.Action;
import org.evomaster.core.search.action.ActionFilter;
import org.evomaster.core.search.gene.Gene;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public abstract class ResourceMIOHWTestBase extends ResourceTestBase {

    public final Map<String, Set<String>> keysToTable = new HashMap<String, Set<String>>(){{
        put("/api/rA", new HashSet<>(Collections.singletonList("RA")));
        put("/api/rA/{rAId}", new HashSet<>(Collections.singletonList("RA")));
        put("/api/rd", new HashSet<>(Collections.singletonList("RD")));
        put("/api/rd/{rdId}",new HashSet<>(Collections.singletonList("RD")));
        put("/api/rpR",new HashSet<>(Collections.singletonList("RPR")));
        put("/api/rpR/{rpRId}",new HashSet<>(Collections.singletonList("RPR")));
    }};

    public final Map<String, Set<String>> keysToTemplate = new HashMap<String, Set<String>>(){{
        put("/api/rA", new HashSet<>(Arrays.asList("POST-POST", "POST")));
        put("/api/rA/{rAId}", new HashSet<>(Arrays.asList("POST-GET","GET")));
        put("/api/rd", new HashSet<>(Arrays.asList("POST-POST", "POST")));
        put("/api/rd/{rdId}",new HashSet<>(Arrays.asList("POST-GET","GET")));
        put("/api/rpR",new HashSet<>(Arrays.asList("POST-POST", "POST")));
        put("/api/rpR/{rpRId}",new HashSet<>(Arrays.asList("POST-GET","GET")));
    }};

    protected void checkingBinding(RestResourceCalls call, String template, String nodeKey, Boolean withSQL){
        if (nodeKey.endsWith("Id}")){
            if (withSQL){
                if (template.equals("GET")){
                    Gene rdIdInRest = call.seeActions(ActionFilter.NO_SQL).get(0).seeTopGenes().stream().findFirst().orElse(null);
                    Gene rdIdInDB = getGeneByName(Objects.requireNonNull(call.seeActions(ActionFilter.ONLY_SQL).stream().findFirst().orElse(null)),"ID");
                    // test binding between DB and RestAction
                    assertNotNull(rdIdInRest);
                    assertEquals(rdIdInRest.getValueAsRawString(), rdIdInDB.getValueAsRawString());
                }
            }else {
                if (template.equals("POST-GET")){
                    Gene bodyInPOST = getGeneByName(call.seeActions(ActionFilter.NO_SQL).get(0), "id");
                    Gene rdIdInGet = call.seeActions(ActionFilter.NO_SQL).get(1).seeTopGenes().stream().findFirst().orElse(null);
                    assertNotNull(rdIdInGet);
                    assertEquals(bodyInPOST.getValueAsRawString(), rdIdInGet.getValueAsRawString());

                }
            }
        }else{
            if (withSQL){
                Gene rdIdInRest = getGeneByName(call.seeActions(ActionFilter.NO_SQL).get(0), "id");
                Gene rdIdInDB = getGeneByName(Objects.requireNonNull(call.seeActions(ActionFilter.ONLY_SQL).stream().findFirst().orElse(null)),"ID");
                // test binding between DB and RestAction
                assertEquals(rdIdInRest.getValueAsRawString(), rdIdInDB.getValueAsRawString());
            }else{
                if (template.equals("POST-POST")){
                    Gene bodyInPOST = getGeneByName(call.seeActions(ActionFilter.NO_SQL).get(0), "id");
                    Gene rdIdInGet = getGeneByName(call.seeActions(ActionFilter.NO_SQL).get(0), "id");
                    assertEquals(bodyInPOST.getValueAsRawString(), rdIdInGet.getValueAsRawString());
                }
            }

        }
    }

    protected Gene getGeneByName(Action action, String name){
        return action.seeTopGenes().stream().flatMap(s-> s.flatView(gene -> false).stream()).filter(s-> s.getName().equalsIgnoreCase(name)).findAny()
                .orElse(null);
    }

    protected List<String> generalArgs(int budget, int seed){
        return new ArrayList<>(
                Arrays.asList(
                        "--createTests", "false",
                        "--seed", ""+seed,
                        "--useTimeInFeedbackSampling", "false",
                        "--sutControllerPort", "" + controllerPort,
                        "--maxEvaluations", "" + budget,
                        "--stoppingCriterion", "ACTION_EVALUATIONS",
                        //there some bugs here
                        "--baseTaintAnalysisProbability", "0.0"
                )
        );
    }

    protected void hypmutation(List<String> args, Boolean enable){
        //disable hypermutation
        args.add("--weightBasedMutationRate");
        args.add(""+enable);
    }

    protected void adaptiveMutation(List<String> args, double apc){
        args.add("--enableTrackEvaluatedIndividual");
        if (apc > 0.0)args.add("true"); else args.add("false");
        args.add("--adaptiveGeneSelectionMethod");
        if (apc > 0.0) args.add("APPROACH_IMPACT"); else args.add("NONE");
        args.add("--archiveGeneMutation");
        if (apc > 0.0) args.add("SPECIFIED_WITH_SPECIFIC_TARGETS"); else args.add("NONE");
        args.add("--probOfArchiveMutation");
        args.add(""+apc);
    }

    protected void defaultResourceConfig(List<String> args, boolean applyName){

        args.add("--resourceSampleStrategy");
        args.add("ConArchive");
        args.add("--probOfSmartSampling");
        args.add("1.0");
        args.add("--probOfEnablingResourceDependencyHeuristics");
        args.add("1.0");
        args.add("--doesApplyNameMatching");
        args.add(""+applyName);
    }

    protected void seedTestConfig(List<String> args){
        args.add("--seedTestCases");
        args.add("true");

        args.add("--seedTestCasesPath");
        args.add("src/test/resources/postman/resource.postman_collection.json");
    }

}
