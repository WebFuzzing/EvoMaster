package org.evomaster.e2etests.spring.examples.resource;

import org.evomaster.core.problem.rest.resource.RestResourceCalls;
import org.evomaster.core.search.Action;
import org.evomaster.core.search.gene.Gene;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;


abstract class ResourceMIOHWTest extends ResourceTestBase {

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

    protected void checkingBinding(RestResourceCalls call, String template, String nodeKey, Boolean withSQL){
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

    protected Gene getGeneByName(Action action, String name){
        return action.seeGenes().stream().flatMap(s-> s.flatView(gene -> false).stream()).filter(s-> s.getName().equalsIgnoreCase(name)).findAny()
                .orElse(null);
    }

    protected List<String> generalArgs(int budget, int seed){
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

    protected void defaultResourceConfig(List<String> args){

        args.add("--resourceSampleStrategy");
        args.add("ConArchive");
        args.add("--probOfSmartSampling");
        args.add("1.0");
        args.add("--probOfEnablingResourceDependencyHeuristics");
        args.add("1.0");
    }

}
