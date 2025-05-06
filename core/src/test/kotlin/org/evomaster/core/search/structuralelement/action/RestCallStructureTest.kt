package org.evomaster.core.search.structuralelement.action

import io.swagger.parser.OpenAPIParser
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.structuralelement.StructuralElementBaseTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

object RestActionCluster{
    private val config : EMConfig = EMConfig()
    private val cluster : MutableMap<String, Action> = mutableMapOf()
    init {
        RestActionBuilderV3.addActionsFromSwagger(OpenAPIParser().readLocation("swagger/artificial/resource_test.json", null, null).openAPI, cluster, enableConstraintHandling = config.enableSchemaConstraintHandling)
    }

    fun getRestCallAction(path: String) : RestCallAction? = cluster[path] as? RestCallAction

}


class RestPostCallStructureTest : StructuralElementBaseTest(){
    override fun getStructuralElement(): RestCallAction {
        return RestActionCluster.getRestCallAction("POST:/v3/api/rfoo")?:throw IllegalStateException("cannot get the expected the action")
    }

    override fun getExpectedChildrenSize(): Int = 2

    @Test
    fun testTraverseBackIndex(){
        val root = getStructuralElement()
        assertEquals(root, root.getRoot())

        val bodyParam = root.parameters.find { it is BodyParam }
        assertNotNull(bodyParam)
        val index = root.parameters.indexOf(bodyParam)

        val floatValue = (bodyParam!!.gene as ObjectGene).fields[3]

        val path = listOf(index, 0, 3)
        assertEquals(floatValue, root.targetWithIndex(path))

        val actualPath = mutableListOf<Int>()
        floatValue.traverseBackIndex(actualPath)
        assertEquals(path, actualPath)

    }
}

class RestGetCallStructureTest : StructuralElementBaseTest(){
    override fun getStructuralElement(): RestCallAction {
        return RestActionCluster.getRestCallAction("GET:/v3/api/rfoo/{rfooId}")?:throw IllegalStateException("cannot get the expected the action")
    }

    override fun getExpectedChildrenSize(): Int = 3
}


