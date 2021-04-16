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
import org.evomaster.core.search.service.mutator.EvaluatedMutation;
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification;
import org.evomaster.core.search.tracer.TraceableElementCopyFilter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ResourceMIOWithAWHAndNumSQLTest extends ResourceMIOHWTest{

    @Test
    public void testResourceMIOWithAWHAndSQLHandling() {
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
        args.add("--maxSqlInitActionsPerResource");
        args.add("3");

        Injector injector = init(args);
        initPartialOracles(injector);

        ResourceManageService rmanger = injector.getInstance(ResourceManageService.class);
        ResourceRestMutator mutator = injector.getInstance(ResourceRestMutator.class);
        RestResourceFitness ff = injector.getInstance(RestResourceFitness.class);
        RestResourceStructureMutator structureMutator = injector.getInstance(RestResourceStructureMutator.class);

        assertNotNull(rmanger.getSqlBuilder());

        String raIdKey = "/api/rA/{rAId}";
        String raIdPostTemplate = "POST-GET";
        RestResourceNode raIdNode = rmanger.getResourceNodeFromCluster(raIdKey);
        RestResourceCalls rAIdcall = rmanger.genCalls(raIdNode, raIdPostTemplate, 10, false, true, false, false);

        List<RestResourceCalls> calls = Arrays.asList(rAIdcall);
        RestIndividual raIdInd = new RestIndividual(calls, SampleType.SMART_RESOURCE, null, Collections.emptyList(), null, 1);
        EvaluatedIndividual<RestIndividual> rdIdEval = ff.calculateCoverage(raIdInd, Collections.emptySet());

        //sql add
        RestIndividual raIdMutatedInd = (RestIndividual)raIdInd.copy();
        MutatedGeneSpecification mutatedSpecSQLAdd = new MutatedGeneSpecification();
        mutatedSpecSQLAdd.setMutatedIndividual(raIdMutatedInd);
        mutator.preHandlingTrackedIndividual(rdIdEval);
        structureMutator.mutateRestResourceCalls(raIdMutatedInd, rdIdEval, RestResourceStructureMutator.MutationType.SQL_ADD, mutatedSpecSQLAdd);

        assertTrue(mutatedSpecSQLAdd.didStructureMutation());
        assertFalse(mutatedSpecSQLAdd.getAddedDbActions().isEmpty());

        EvaluatedIndividual<RestIndividual> raIdMutatedIndEval = ff.calculateCoverage(raIdMutatedInd, Collections.emptySet());

        EvaluatedIndividual<RestIndividual> raIdMutatedIndEvalWithTraces = rdIdEval.next(
                raIdMutatedIndEval, TraceableElementCopyFilter.Companion.getWITH_ONLY_EVALUATED_RESULT(), EvaluatedMutation.BETTER_THAN);

        raIdMutatedIndEvalWithTraces.updateImpactOfGenes(rdIdEval,raIdMutatedIndEval, mutatedSpecSQLAdd, Collections.emptyMap());
        assertEquals(raIdMutatedIndEvalWithTraces.getIndividual().seeInitializingActions().size(), raIdMutatedIndEvalWithTraces.getSizeOfImpact(true));

        //sql remove
        RestIndividual raIdRemoveInd = (RestIndividual) (raIdMutatedIndEvalWithTraces.getIndividual()).copy();
        MutatedGeneSpecification mutatedSpecSQLRemove = new MutatedGeneSpecification();
        mutatedSpecSQLRemove.setMutatedIndividual(raIdRemoveInd);
        structureMutator.mutateRestResourceCalls(raIdRemoveInd, raIdMutatedIndEvalWithTraces, RestResourceStructureMutator.MutationType.SQL_REMOVE, mutatedSpecSQLRemove);

        assertTrue(mutatedSpecSQLRemove.didStructureMutation());
        assertFalse(mutatedSpecSQLRemove.getRemovedDbActions().isEmpty());

        EvaluatedIndividual<RestIndividual> raIdRemoveIndEval = ff.calculateCoverage(raIdRemoveInd, Collections.emptySet());

        EvaluatedIndividual<RestIndividual> raIdRemoveIndEvalWithTraces = raIdMutatedIndEvalWithTraces.next(
                raIdRemoveIndEval, TraceableElementCopyFilter.Companion.getWITH_ONLY_EVALUATED_RESULT(), EvaluatedMutation.BETTER_THAN);
        raIdRemoveIndEvalWithTraces.updateImpactOfGenes(raIdMutatedIndEvalWithTraces, raIdRemoveIndEval, mutatedSpecSQLRemove, Collections.emptyMap());
        assertTrue(raIdRemoveIndEvalWithTraces.getIndividual().seeInitializingActions().size() < raIdMutatedIndEvalWithTraces.getIndividual().seeInitializingActions().size());
        assertEquals(raIdRemoveIndEvalWithTraces.getIndividual().seeInitializingActions().size(), raIdRemoveIndEvalWithTraces.getSizeOfImpact(true));
    }
}
