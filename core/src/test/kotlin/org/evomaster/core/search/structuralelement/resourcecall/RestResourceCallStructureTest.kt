package org.evomaster.core.search.structuralelement.resourcecall

import io.swagger.parser.OpenAPIParser
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.resource.ResourceCluster
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.structuralelement.StructuralElementBaseTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

object ResourceNodeCluster{
    val cluster = ResourceCluster()
    val randomness = Randomness()
    init {
        val schema = OpenAPIParser().readLocation("/swagger/artificial/resource_test.json", null, null).openAPI
        val config = EMConfig()
        config.doesApplyNameMatching = true

        val actions: MutableMap<String, Action> = mutableMapOf()
        RestActionBuilderV3.addActionsFromSwagger(schema, actions, enableConstraintHandling = config.enableSchemaConstraintHandling)
        cluster.initResourceCluster(actions, config = config)
    }
}

class RestResourceCallPostGetStructureTest : StructuralElementBaseTest(){
    override fun getStructuralElement(): RestResourceCalls {
        val foo = "/v3/api/rfoo/{rfooId}"
        val fooNode = ResourceNodeCluster.cluster.getResourceNode(foo)
        return fooNode?.sampleRestResourceCalls("POST-GET", ResourceNodeCluster.randomness, 10)?: throw IllegalStateException("cannot sample resource call with the template")
    }

    override fun getExpectedChildrenSize(): Int = 3



    @Test
    fun testTraverseBackIndex(){
        val root = getStructuralElement()
        assertEquals(root, root.getRoot())

        val id = (root.seeActions(ActionFilter.NO_SQL)[1] as RestCallAction).parameters[0].gene

        val path = listOf(1, 0, 0, 0)
        assertEquals(id, root.targetWithIndex(path))

        val actualPath = mutableListOf<Int>()
        id.traverseBackIndex(actualPath)
        assertEquals(path, actualPath)

    }

}