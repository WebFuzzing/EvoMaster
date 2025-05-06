package org.evomaster.core.utils

import io.swagger.parser.OpenAPIParser
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.util.BindingBuilder
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BindingBuilderTest {

    companion object{

        val actionCluster: MutableMap<String, Action> = mutableMapOf()
        val randomness = Randomness()
        val config = EMConfig()

        @BeforeAll
        @JvmStatic
        fun init(){

            val schema = OpenAPIParser().readLocation("/swagger/artificial/resource_test.json", null, null).openAPI
            RestActionBuilderV3.addActionsFromSwagger(schema, actionCluster, enableConstraintHandling = config.enableSchemaConstraintHandling)
        }
    }


    @Test
    fun testBindingInOneAction(){
        val action = (actionCluster.getValue("PUT:/v3/api/rfoo/{rfooId}") as? RestCallAction)?.copy() as? RestCallAction
        assertNotNull(action)
        action!!.randomize(randomness, false)

        val pairs = BindingBuilder.buildBindingPairsInRestAction(action, randomness)
        assertEquals(1, pairs.size)
        assertEquals(setOf("id","rfooId"), setOf(pairs.first().first.name, pairs.first().second.name))

        BindingBuilder.bindParamsInRestAction(action, true, randomness)
        val patchRfooId = ((action.parameters.find { it is PathParam } as? PathParam)?.gene as? CustomMutationRateGene<*>)?.gene as? LongGene
        assertNotNull(patchRfooId)
        val patchBody = (action.parameters.find { it is BodyParam } as? BodyParam)?.gene as? ObjectGene
        assertNotNull(patchBody)
        val patchBodyId = patchBody!!.fields.find { it.name == "id" } as? LongGene
        assertNotNull(patchBodyId)
        assertTrue(patchBodyId!!.isDirectBoundWith(patchRfooId!!))
        assertTrue(patchRfooId.isDirectBoundWith(patchBodyId))
        assertEquals(patchBodyId.value, patchRfooId.value)
    }

    @Test
    fun testBindingInTwoAction(){
        val getXyz = (actionCluster.getValue("GET:/v3/api/rfoo/{rfooId}/rbar/{rbarId}/rxyz/{rxyzId}") as? RestCallAction)?.copy() as? RestCallAction
        assertNotNull(getXyz)
        getXyz!!.randomize(randomness, false)
        val getXyzId = ((getXyz.parameters.find { it is PathParam && it.name == "rxyzId"} as? PathParam)?.gene as? CustomMutationRateGene<*>)?.gene as? LongGene
        assertNotNull(getXyzId)
        val getXyzBarId = ((getXyz.parameters.find { it is PathParam && it.name == "rbarId"} as? PathParam)?.gene as? CustomMutationRateGene<*>)?.gene as? LongGene
        assertNotNull(getXyzBarId)
        val getXyzFooId = ((getXyz.parameters.find { it is PathParam && it.name == "rfooId"} as? PathParam)?.gene as? CustomMutationRateGene<*>)?.gene as? LongGene
        assertNotNull(getXyzFooId)

        val postXyz = (actionCluster.getValue("POST:/v3/api/rfoo/{rfooId}/rbar/{rbarId}/rxyz") as? RestCallAction)?.copy() as? RestCallAction
        assertNotNull(postXyz)
        postXyz!!.randomize(randomness, false)
        // build binding
        postXyz.bindBasedOn(getXyz, randomness)
        val postBodyXyzId = ((postXyz.parameters.find { it is BodyParam } as? BodyParam)?.gene as? ObjectGene)?.fields?.find { it.name == "id" } as LongGene
        assertNotNull(postBodyXyzId)
        assertTrue(postBodyXyzId.isDirectBoundWith(getXyzId!!))
        assertTrue(getXyzId.isDirectBoundWith(postBodyXyzId))

        val postXyzBarId = ((postXyz.parameters.find { it is PathParam && it.name == "rbarId"} as? PathParam)?.gene as? CustomMutationRateGene<*>)?.gene as? LongGene
        assertNotNull(postXyzBarId)
        assertTrue(postXyzBarId!!.isDirectBoundWith(getXyzBarId!!))
        assertTrue(getXyzBarId.isDirectBoundWith(postXyzBarId))
        assertTrue(getXyzBarId.isSameBinding(setOf(postXyzBarId)))

        val postXyzFooId = ((postXyz.parameters.find { it is PathParam && it.name == "rfooId"} as? PathParam)?.gene as? CustomMutationRateGene<*>)?.gene as? LongGene
        assertNotNull(postXyzFooId)
        assertTrue(postXyzFooId!!.isDirectBoundWith(getXyzFooId!!))
        assertTrue(getXyzFooId.isDirectBoundWith(postXyzFooId))
        assertTrue(getXyzFooId.isSameBinding(setOf(postXyzFooId)))

        assertTrue(getXyzId.isSameBinding(setOf(postBodyXyzId)))
        assertTrue(getXyzBarId.isSameBinding(setOf(postXyzBarId)))
        assertTrue(getXyzFooId.isSameBinding(setOf(postXyzFooId)))

        val postBar = (actionCluster.getValue("POST:/v3/api/rfoo/{rfooId}/rbar") as? RestCallAction)?.copy() as? RestCallAction
        assertNotNull(postBar)
        postBar!!.randomize(randomness, false)
        postBar.bindBasedOn(getXyz, randomness)
        val postBodyBarId = ((postBar.parameters.find { it is BodyParam } as? BodyParam)?.gene as? ObjectGene)?.fields?.find { it.name == "id" } as? LongGene
        assertNotNull(postBodyBarId)
        assertTrue(postBodyBarId!!.isDirectBoundWith(getXyzBarId))
        assertTrue(getXyzBarId.isDirectBoundWith(postBodyBarId))

        val postBarFooId = ((postBar.parameters.find { it is PathParam && it.name == "rfooId"} as? PathParam)?.gene as? CustomMutationRateGene<*>)?.gene as? LongGene
        assertNotNull(postBarFooId)
        assertTrue(postBarFooId!!.isDirectBoundWith(getXyzFooId))
        assertTrue(getXyzFooId.isDirectBoundWith(postBarFooId))

        assertTrue(getXyzBarId.isSameBinding(setOf(postXyzBarId, postBodyBarId)))
        assertTrue(getXyzFooId.isSameBinding(setOf(postXyzFooId, postBarFooId)))


        val postFoo = (actionCluster.getValue("POST:/v3/api/rfoo") as? RestCallAction)?.copy() as? RestCallAction
        assertNotNull(postFoo)
        postFoo!!.randomize(randomness, false)
        postFoo.bindBasedOn(getXyz, randomness)

        val postBodyFooId = ((postFoo.parameters.find { it is BodyParam } as? BodyParam)?.gene as? ObjectGene)?.fields?.find { it.name == "id" } as? LongGene
        assertNotNull(postBodyFooId)
        assertTrue(postBodyFooId!!.isDirectBoundWith(getXyzFooId))
        assertTrue(getXyzFooId.isDirectBoundWith(postBodyFooId))


        assertTrue(getXyzFooId.isSameBinding(setOf(postXyzFooId, postBarFooId, postBodyFooId)))

        // check value
        // xyz
        assertEquals(getXyzId.value, postBodyXyzId.value)
        // bar
        assertEquals(getXyzBarId.value, postXyzBarId.value)
        assertEquals(getXyzBarId.value, postBodyBarId.value)
        // foo
        assertEquals(getXyzFooId.value, postXyzFooId.value)
        assertEquals(getXyzFooId.value, postBarFooId.value)
        assertEquals(getXyzFooId.value, postBodyFooId.value)

        // mutate
        getXyzId.value = getXyzId.value + 1
        assertNotEquals(getXyzId.value, postBodyXyzId.value)
        getXyzId.syncBindingGenesBasedOnThis()
        assertEquals(getXyzId.value, postBodyXyzId.value)

        postXyzBarId.value = postXyzBarId.value + 1
        assertNotEquals(getXyzBarId.value, postXyzBarId.value)
        assertEquals(getXyzBarId.value, postBodyBarId.value)
        // sync any of getXyzBarId, postXyzBarId and postBodyBarId
        postXyzBarId.syncBindingGenesBasedOnThis()
        assertEquals(postXyzBarId.value, getXyzBarId.value)
        assertEquals(postXyzBarId.value, postBodyBarId.value)

        postBodyFooId.value = postBodyFooId.value + 1
        assertNotEquals(getXyzFooId.value, postBodyFooId.value)
        postBodyFooId.syncBindingGenesBasedOnThis()
        assertEquals(postBodyFooId.value, getXyzFooId.value)
        assertEquals(postBodyFooId.value, postXyzFooId.value)
        assertEquals(postBodyFooId.value, postBarFooId.value)

    }

    @Test
    fun testBindingRestActionDbAction(){
        val getXyz = (actionCluster.getValue("GET:/v3/api/rfoo/{rfooId}/rbar/{rbarId}/rxyz/{rxyzId}") as? RestCallAction)?.copy() as? RestCallAction
        assertNotNull(getXyz)
        getXyz!!.randomize(randomness, false)
        val getXyzId = ((getXyz.parameters.find { it is PathParam && it.name == "rxyzId"} as? PathParam)?.gene as? CustomMutationRateGene<*>)?.gene as? LongGene
        assertNotNull(getXyzId)
        val getXyzBarId = ((getXyz.parameters.find { it is PathParam && it.name == "rbarId"} as? PathParam)?.gene as? CustomMutationRateGene<*>)?.gene as? LongGene
        assertNotNull(getXyzBarId)
        val getXyzFooId = ((getXyz.parameters.find { it is PathParam && it.name == "rfooId"} as? PathParam)?.gene as? CustomMutationRateGene<*>)?.gene as? LongGene
        assertNotNull(getXyzFooId)

    }

}