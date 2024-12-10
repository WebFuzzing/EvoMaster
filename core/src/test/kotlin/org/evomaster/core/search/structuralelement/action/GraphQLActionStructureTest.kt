package org.evomaster.core.search.structuralelement.action

import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.builder.GraphQLActionBuilder
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.structuralelement.StructuralElementBaseTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

object GQLActionCluster{
    val cluster : MutableMap<String, Action> = mutableMapOf()
    init {
        val schema = this::class.java.getResource("/graphql/online/PetsClinic.json")?.readText()?:throw IllegalStateException("fail to get the resource")
        GraphQLActionBuilder.addActionsFromSchema(schema, cluster)
    }
}

class GraphQLActionStructureTest :StructuralElementBaseTest(){

    override fun getStructuralElement(): GraphQLAction = (GQLActionCluster.cluster["pettypes"] as? GraphQLAction)?:throw IllegalStateException("cannot get the specified action")

    override fun getExpectedChildrenSize(): Int =1


    @Test
    fun testTraverseBackIndex(){
        val root = getStructuralElement()
        assertEquals(root, root.getRoot())

        val name = ((root.parameters[0] as GQReturnParam).gene as ObjectGene).fields[1]

        assertTrue(name is BooleanGene)

        val path = listOf(0, 0, 1)
        assertEquals(name, root.targetWithIndex(path))

        val actualPath = mutableListOf<Int>()
        name.traverseBackIndex(actualPath)
        assertEquals(path, actualPath)

    }

}