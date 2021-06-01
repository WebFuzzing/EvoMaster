package org.evomaster.core.problem.rest.resource

import io.swagger.parser.OpenAPIParser
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.search.Action
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ResourceNodeTest {

    @Test
    fun testInit(){

        val schema = OpenAPIParser().readLocation("/swagger/artificial/resource_test.json", null, null).openAPI

        val actions: MutableMap<String, Action> = mutableMapOf()
        RestActionBuilderV3.addActionsFromSwagger(schema, actions)

        val cluster = ResourceCluster()
        val config = EMConfig()
        config.doesApplyNameMatching = true
        cluster.initResourceCluster(actions, config = config)
        assertEquals(4, cluster.getCluster().size)

        val postFoo = "/v3/api/foo"
        val postAction = "POST:/v3/api/foo"
        val fooNode = cluster.getResourceNode(postFoo)
        assertNotNull(fooNode)
        assertEquals(2, fooNode!!.getTemplates().size)
        assertEquals(setOf("POST", "POST-POST"), fooNode.getTemplates().keys)
        assertEquals(1, fooNode.paramsInfo.size)

        val getFoo = "/v3/api/foo/{id}"
        val fooIdNode = cluster.getResourceNode(getFoo)
        assertNotNull(fooIdNode)
        assertEquals(2, fooIdNode!!.getTemplates().size)
        assertEquals(setOf("GET", "POST-GET"), fooIdNode.getTemplates().keys)
        assertEquals(2, fooIdNode.paramsInfo.size)

        val postBar = "/v3/api/bar"
        assertNotNull(cluster.getResourceNode(postBar))
        assertEquals(2, cluster.getResourceNode(postBar)!!.getTemplates().size)
        assertEquals(setOf("POST", "POST-POST"), cluster.getResourceNode(postBar)!!.getTemplates().keys)

        val getBar = "/v3/api/bar/{id}"
        assertNotNull(cluster.getResourceNode(getBar))
        assertEquals(2, cluster.getResourceNode(getBar)!!.getTemplates().size)
        assertEquals(setOf("GET", "POST-GET"), cluster.getResourceNode(getBar)!!.getTemplates().keys)


    }

}