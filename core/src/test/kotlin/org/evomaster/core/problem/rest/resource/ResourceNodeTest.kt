package org.evomaster.core.problem.rest.resource

import io.swagger.parser.OpenAPIParser
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.search.Action
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class ResourceNodeTest {

   companion object{

       val actionCluster: MutableMap<String, Action> = mutableMapOf()
       val cluster = ResourceCluster()

       @BeforeAll
       @JvmStatic
       fun init(){

           val schema = OpenAPIParser().readLocation("/swagger/artificial/resource_test.json", null, null).openAPI
           RestActionBuilderV3.addActionsFromSwagger(schema, actionCluster)
           val config = EMConfig()
           config.doesApplyNameMatching = true
           cluster.initResourceCluster(actionCluster, config = config)
       }
   }

    @Test
    fun testInit(){

        //rfoo, rfoo/{id}, rbar, rbar/{id}, rxyz
        assertEquals(5, cluster.getCluster().size)

        val postFoo = "/v3/api/rfoo"
        val fooNode = cluster.getResourceNode(postFoo)
        assertNotNull(fooNode)
        assertEquals(2, fooNode!!.getTemplates().size)
        assertEquals(setOf("POST", "POST-POST"), fooNode.getTemplates().keys)

        val getFoo = "/v3/api/rfoo/{rfooId}"
        val fooIdNode = cluster.getResourceNode(getFoo)
        assertNotNull(fooIdNode)
        assertEquals(6, fooIdNode!!.getTemplates().size)
        assertEquals(setOf("GET", "POST-GET", "PUT", "POST-PUT", "PATCH", "POST-PATCH"), fooIdNode.getTemplates().keys)

        val postBar = "/v3/api/rfoo/{rfooId}/rbar"
        assertNotNull(cluster.getResourceNode(postBar))
        assertEquals(2, cluster.getResourceNode(postBar)!!.getTemplates().size)
        assertEquals(setOf("POST", "POST-POST"), cluster.getResourceNode(postBar)!!.getTemplates().keys)

        val getBar = "/v3/api/rfoo/{rfooId}/rbar/{rbarId}"
        assertNotNull(cluster.getResourceNode(getBar))
        assertEquals(2, cluster.getResourceNode(getBar)!!.getTemplates().size)
        assertEquals(setOf("GET", "POST-GET"), cluster.getResourceNode(getBar)!!.getTemplates().keys)

    }

    @Test
    fun testParamInfoBuilderWithoutDB(){

        // rfoo
        val rfoo = cluster.getResourceNode("/v3/api/rfoo", nullCheck = true)
        assertEquals(1, rfoo!!.paramsInfo.size)
        assertEquals(0, rfoo.paramsInfo.count { it.value.doesReferToOther })

        // rfooid
        val rfooId = cluster.getResourceNode("/v3/api/rfoo/{rfooId}", true)
        // pathparam, queryparam from get, bodyparam from put and patch
        assertEquals(3, rfooId!!.paramsInfo.size)
        assertEquals(1, rfooId.paramsInfo.count { it.value.doesReferToOther })
        assertEquals("rfooId", rfooId.paramsInfo.values.find { it.doesReferToOther }!!.name)
        assertEquals(2, rfooId.getPossiblyBoundParams("GET", false).size)
        // rfooId is required to be bound with POST if it exists
        rfooId.getPossiblyBoundParams("POST-GET", false).apply {
            assertEquals(1, size)
            assertEquals("rfooId", first().name)
        }

        // rbar
        val rbar = cluster.getResourceNode("/v3/api/rfoo/{rfooId}/rbar", true)
        assertEquals(2, rbar!!.paramsInfo.size)
        assertEquals(1, rbar.paramsInfo.count { it.value.doesReferToOther })

        // rbarid
        val rbarId = cluster.getResourceNode("/v3/api/rfoo/{rfooId}/rbar/{rbarId}", true)
        assertEquals(3, rbarId!!.paramsInfo.size)
        assertEquals(2, rbarId.paramsInfo.count { it.value.doesReferToOther })
        assertEquals("rbarId", rbarId.paramsInfo.values.find { it.doesReferToOther }!!.name)

        // rxyz
        val rxyz = cluster.getResourceNode("/v3/api/rfoo/{rfooId}/rbar/{rbarId}/rxyz", true)
        assertEquals(3, rxyz!!.paramsInfo.size)
        assertEquals(2, rxyz.paramsInfo.count { it.value.doesReferToOther })
        // rfooId and rbardId are required to be bound with POST if they exist
        assertEquals(setOf("rfooId", "rbarId"), rxyz.paramsInfo.filter { it.value.doesReferToOther }.map { it.value.name }.toSet())

    }


    @ParameterizedTest
    @CsvSource(value = ["/v3/api/rfoo/{rfooId},1","/v3/api/rfoo/{rfooId}/rbar/{rbarId},2", "/v3/api/rfoo/{rfooId}/rbar/{rbarId}/rxyz,3"])
    fun testCompletePostCreation(path: String, expected: Int){

        val rfooIdNode = cluster.getResourceNode(path)
        assertNotNull(rfooIdNode)
        rfooIdNode!!.getPostChain().apply {
            assertNotNull(this)
            assertEquals(expected, this!!.actions.size)
            assertTrue(isComplete())
        }
    }
}