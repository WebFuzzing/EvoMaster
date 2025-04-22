package org.evomaster.e2etests.spring.examples.resource.db;

import com.google.inject.Injector;
import org.evomaster.core.EMConfig;
import org.evomaster.core.sql.SqlAction;
import org.evomaster.core.problem.rest.resource.RestResourceCalls;
import org.evomaster.core.problem.rest.resource.RestResourceNode;
import org.evomaster.core.problem.rest.service.ResourceManageService;
import org.evomaster.core.problem.util.BindingBuilder;
import org.evomaster.core.search.action.ActionFilter;
import org.evomaster.e2etests.spring.examples.resource.ResourceMIOHWTestBase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResourceDbMIOBasicTest extends ResourceMIOHWTestBase {

    @Test
    public void testResourceSamplerAndParamBinding() {

        List<String> args = generalArgs(1, 42);
        hypmutation(args, false);
        adaptiveMutation(args, 0.0);
        defaultResourceConfig(args, true);
        args.add("--probOfApplySQLActionToCreateResources");
        args.add("0.1"); //this leads to flaky tests

        Injector injector = init(args);

        ResourceManageService rmanger = injector.getInstance(ResourceManageService.class);
        EMConfig config = injector.getInstance(EMConfig.class);

        assertEquals(keysToTemplate.keySet(), rmanger.getResourceCluster().keySet());

        for (String key : keysToTemplate.keySet()){
            RestResourceNode node = rmanger.getResourceNodeFromCluster(key);
            assertEquals(keysToTemplate.get(key), node.getTemplates().keySet(), key);
        }

        config.setProbOfApplySQLActionToCreateResources(0d);
        String raKey = "/api/rA";
        String raPostTemplate = "POST-POST";
        List<RestResourceCalls> calls = new ArrayList<>();
        rmanger.sampleCall(raKey, true, calls, 10, false, Collections.emptyList(), raPostTemplate);
        assertEquals(2, calls.get(0).seeActions(ActionFilter.ALL).size());
        assertEquals(2, calls.get(0).seeGenes(ActionFilter.ALL).stream().filter(s-> !BindingBuilder.INSTANCE.isExtraTaintParam(s.getName()) && s.isMutable()).count());
        checkingBinding(calls.get(0), "POST-POST", raKey, false);

        config.setProbOfApplySQLActionToCreateResources(0d);
        String raIdKey = "/api/rA/{rAId}";
        String raIdPostTemplate = "POST-GET";
        calls.clear();
        rmanger.sampleCall(raIdKey, true, calls, 10, false, Collections.emptyList(), raIdPostTemplate);
        assertEquals(2, calls.get(0).seeActions(ActionFilter.ALL).size());
        assertEquals(2, calls.get(0).seeGenes(ActionFilter.ALL).stream().filter(s-> !BindingBuilder.INSTANCE.isExtraTaintParam(s.getName()) && s.isMutable()).count());
        checkingBinding(calls.get(0), raIdPostTemplate, raIdKey, false);

        // SQL-GET
        config.setProbOfApplySQLActionToCreateResources(0.1d);
        calls.clear();
        rmanger.sampleCall(raIdKey, true, calls, 10, true, Collections.emptyList(), "GET");
        assertEquals(2, calls.get(0).seeActions(ActionFilter.ALL).size());
        assert( calls.get(0).seeActions(ActionFilter.ALL).get(0) instanceof SqlAction);
        assertEquals(3, calls.get(0).seeActions(ActionFilter.ALL).get(0).seeTopGenes().size());
        //check whether the gene binding with rest action is removed with seeGenes(ActionFilter.ONLY_SQL)
        assertEquals(2, calls.get(0).seeGenes(ActionFilter.ONLY_SQL).size());
        checkingBinding(calls.get(0), "GET", raIdKey, true);

    }
}
