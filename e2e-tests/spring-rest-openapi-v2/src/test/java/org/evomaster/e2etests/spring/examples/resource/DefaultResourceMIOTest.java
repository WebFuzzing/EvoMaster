package org.evomaster.e2etests.spring.examples.resource;

import com.google.inject.Injector;
import org.evomaster.core.problem.rest.resource.RestResourceCalls;
import org.evomaster.core.problem.rest.resource.RestResourceNode;
import org.evomaster.core.problem.rest.service.ResourceManageService;
import org.evomaster.core.search.GeneFilter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DefaultResourceMIOTest extends ResourceMIOHWTest{

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
        checkingBinding(rAIdcall, raIdPostTemplate, raIdKey, false);
    }
}
