package org.evomaster.core.problem.rest.resource

import io.swagger.parser.OpenAPIParser
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ResourceNodeTest {

   companion object{

       val actionCluster: MutableMap<String, Action> = mutableMapOf()
       val cluster = ResourceCluster()
       val randomness = Randomness()

       @BeforeAll
       @JvmStatic
       fun init(){
           val config = EMConfig()
           config.doesApplyNameMatching = true

           val schema = OpenAPIParser().readLocation("/swagger/artificial/resource_test.json", null, null).openAPI
           RestActionBuilderV3.addActionsFromSwagger(schema, actionCluster, enableConstraintHandling = config.enableSchemaConstraintHandling)
           cluster.initResourceCluster(actionCluster, config = config)
       }
   }

    @Test
    fun testInit(){

        //rfoo, rfoo/{id}, rbar, rbar/{id}, rxyz
        assertEquals(6, cluster.getCluster().size)

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
        assertEquals(2, rfoo!!.paramsInfo.size)
        assertEquals(0, rfoo.paramsInfo.count { it.value.requiredReferToOthers() })

        // rfooid
        val rfooId = cluster.getResourceNode("/v3/api/rfoo/{rfooId}", true)
        // pathparam, queryparam from get, bodyparam from put and patch
        assertEquals(4, rfooId!!.paramsInfo.size)
        assertEquals(1, rfooId.paramsInfo.count { it.value.requiredReferToOthers() })
        assertEquals("rfooId", rfooId.paramsInfo.values.find { it.requiredReferToOthers() }!!.name)
        assertEquals(3, rfooId.getPossiblyBoundParams("GET", false).size)
        // rfooId is required to be bound with POST if it exists
        rfooId.getPossiblyBoundParams("POST-GET", false).apply {
            assertEquals(1, size)
            assertEquals("rfooId", first().name)
        }

        // rbar
        val rbar = cluster.getResourceNode("/v3/api/rfoo/{rfooId}/rbar", true)
        assertEquals(2, rbar!!.paramsInfo.size)
        assertEquals(1, rbar.paramsInfo.count { it.value.requiredReferToOthers() })

        // rbarid
        val rbarId = cluster.getResourceNode("/v3/api/rfoo/{rfooId}/rbar/{rbarId}", true)
        assertEquals(3, rbarId!!.paramsInfo.size)
        assertEquals(2, rbarId.paramsInfo.count { it.value.requiredReferToOthers() })
        assertEquals("rbarId", rbarId.paramsInfo.values.find { it.requiredReferToOthers() }!!.name)

        // rxyz
        val rxyz = cluster.getResourceNode("/v3/api/rfoo/{rfooId}/rbar/{rbarId}/rxyz", true)
        assertEquals(3, rxyz!!.paramsInfo.size)
        assertEquals(2, rxyz.paramsInfo.count { it.value.requiredReferToOthers() })
        // rfooId and rbardId are required to be bound with POST if they exist
        assertEquals(setOf("rfooId", "rbarId"), rxyz.paramsInfo.filter { it.value.requiredReferToOthers() }.map { it.value.name }.toSet())

    }

    // test post creation for resource node
    @ParameterizedTest
    @CsvSource(value = ["/v3/api/rfoo/{rfooId},2","/v3/api/rfoo/{rfooId}/rbar/{rbarId},2", "/v3/api/rfoo/{rfooId}/rbar/{rbarId}/rxyz/{rxyzId},3"])
    fun testCompletePostCreation(path: String, expected: Int){

        val node = cluster.getResourceNode(path)
        assertNotNull(node)
        node!!.getPostChain().apply {
            assertNotNull(this)
            assertEquals(expected, this!!.actions.size)
            assertTrue(isComplete())
        }
    }

    @ParameterizedTest
    @CsvSource(value = [
        "/v3/api/rfoo/{rfooId}/rbar/{rbarId}/rxyz,POST,3",
        "/v3/api/rfoo/{rfooId}/rbar/{rbarId}/rxyz,POST-POST,4",
        "/v3/api/rfoo/{rfooId}/rbar/{rbarId}/rxyz/{rxyzId},GET,1",
        "/v3/api/rfoo/{rfooId}/rbar/{rbarId}/rxyz/{rxyzId},POST-GET,4"])
    fun testCallCreation(path:String, template: String, actionSize: Int){
        val node = cluster.getResourceNode(path)
        assertNotNull(node)
        val call = node!!.createRestResourceCallBasedOnTemplate(template, randomness, 10)

        call.apply {
            assertEquals(actionSize, seeActionSize(ActionFilter.NO_SQL))
        }
    }

    @Test
    fun testGeneBinding(){
        val node = cluster.getResourceNode("/v3/api/rfoo/{rfooId}/rbar/{rbarId}/rxyz/{rxyzId}")
        assertNotNull(node)
        val call = node!!.createRestResourceCallBasedOnTemplate("POST-GET", randomness, 10)
        call.seeActions(ActionFilter.NO_SQL).apply {
            assertEquals(4, size)
            val get = last() as RestCallAction
            assertEquals(HttpVerb.GET, get.verb)
            val getXyzId = get.parameters.find { it.name == "rxyzId" }
            assertNotNull(getXyzId)
            val getXyzIdGene = (getXyzId!!.gene as? CustomMutationRateGene<*>)?.gene
            assertNotNull(getXyzIdGene)
            val getBarId = get.parameters.find { it.name == "rbarId" }
            assertNotNull(getBarId)
            val getBarIdGene = (getBarId!!.gene as? CustomMutationRateGene<*>)?.gene
            assertNotNull(getBarIdGene)
            val getFooId = get.parameters.find { it.name == "rfooId" }
            assertNotNull(getFooId)
            val getFooIdGene = (getFooId!!.gene as? CustomMutationRateGene<*>)?.gene
            assertNotNull(getFooIdGene)

            val postXyz = get(2) as RestCallAction
            val postXyzId = postXyz.parameters.find { it is BodyParam }
            assertNotNull(postXyzId)
            val postXyzIdGene = ((postXyzId as BodyParam).gene as? ObjectGene)?.fields?.find { it.name == "id" }
            assertNotNull(postXyzIdGene)
            //binding
            assertTrue(postXyzIdGene!!.isDirectBoundWith(getXyzIdGene!!))
            assertTrue(getXyzIdGene.isDirectBoundWith(postXyzIdGene))
        }
    }

    @Test
    fun testGeneBindingWithQuery(){
        val node = cluster.getResourceNode("/v3/api/rfoo/{rfooId}")
        assertNotNull(node)
        val call = node!!.createRestResourceCallBasedOnTemplate("POST-GET", randomness, 10)
        call.seeActions(ActionFilter.NO_SQL).apply {
            //assertEquals(2, size)
            val get = get(size-1) as RestCallAction
            assertEquals(HttpVerb.GET, get.verb)
            val getFooId = get.parameters.find { it.name == "rfooId" }
            assertNotNull(getFooId)
            val getFooName = get.parameters.find { it.name == "fooName" }
            assertNotNull(getFooName)

            val post = get(0) as RestCallAction
            val postFoo = post.parameters.find { it is BodyParam }
            assertNotNull(postFoo)
            val postFooIdGene = ((postFoo as BodyParam).gene as? ObjectGene)?.fields?.find { it.name == "id" }
            assertNotNull(postFooIdGene)
            val postFooName = post.parameters.find { it is QueryParam && it.name == "fooName" }
            assertNotNull(postFooName)
            //binding
            assertTrue(postFooName!!.gene.isDirectBoundWith(getFooName!!.gene))
            assertTrue(postFooIdGene!!.isDirectBoundWith((getFooId!!.gene as CustomMutationRateGene<*>).gene))
        }
    }

}