package org.evomaster.core.problem.rest.resource

import io.swagger.parser.OpenAPIParser
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ResourceNodeCreationChainTest {

   companion object{

       val actionCluster: MutableMap<String, Action> = mutableMapOf()
       val cluster = ResourceCluster()
       val randomness = Randomness()

       @BeforeAll
       @JvmStatic
       fun init(){
           val config = EMConfig()
           config.doesApplyNameMatching = true

           val schema = OpenAPIParser().readLocation("/swagger/artificial/resource_cross_test.json", null, null).openAPI
           RestActionBuilderV3.addActionsFromSwagger(schema, actionCluster, enableConstraintHandling = config.enableSchemaConstraintHandling)
           cluster.initResourceCluster(actionCluster, config = config)
       }
   }

    @Test
    fun testInit(){

        assertEquals(5, cluster.getCluster().size)

        val root = "/api/root/{rootName}"
        val rootNode = cluster.getResourceNode(root)
        assertNotNull(rootNode)
        assertEquals(4, rootNode!!.getTemplates().size)
        assertEquals(setOf("POST", "GET" ,"POST-POST","POST-GET"), rootNode.getTemplates().keys)

        val bar = "/api/root/{rootName}/bar/{barName}"
        val barNode = cluster.getResourceNode(bar)
        assertNotNull(barNode)
        assertEquals(4, barNode!!.getTemplates().size)
        assertEquals(setOf("POST", "GET" ,"POST-POST","POST-GET"), barNode.getTemplates().keys)

        val foo = "/api/root/{rootName}/foo/{fooName}"
        val fooNode = cluster.getResourceNode(foo)
        assertNotNull(barNode)
        assertEquals(2, fooNode!!.getTemplates().size)
        assertEquals(setOf("POST", "POST-POST"), fooNode.getTemplates().keys)


        val fooBarGet = "/api/root/{rootName}/foo/{fooName}/bar"
        val fooBarGetNode = cluster.getResourceNode(fooBarGet)
        assertNotNull(fooBarGetNode)
        assertEquals(2, fooBarGetNode!!.getTemplates().size)
        assertEquals(setOf("GET", "POST-GET"), fooBarGetNode.getTemplates().keys)


        val fooBarPost = "/api/root/{rootName}/foo/{fooName}/bar/{barName}"
        val fooBarPostNode = cluster.getResourceNode(fooBarPost)
        assertNotNull(fooBarPostNode)
        assertEquals(2, fooBarPostNode!!.getTemplates().size)
        assertEquals(setOf("POST", "POST-POST"), fooBarPostNode.getTemplates().keys)

    }

    @Test
    fun testParamInfoBuilderWithoutDB(){

        val root = cluster.getResourceNode("/api/root/{rootName}", nullCheck = true)
        assertEquals(1, root!!.paramsInfo.size)
        assertEquals(0, root.paramsInfo.count { it.value.requiredReferToOthers() })

        val foo = cluster.getResourceNode("/api/root/{rootName}/foo/{fooName}", true)
        assertEquals(2, foo!!.paramsInfo.size)
        assertEquals(1, foo.paramsInfo.count { it.value.requiredReferToOthers() })
        assertEquals("rootName", foo.paramsInfo.values.find { it.requiredReferToOthers() }!!.name)
        assertEquals(1, foo.getPossiblyBoundParams("POST", false).size)
        foo.getPossiblyBoundParams("POST-POST", false).apply {
            assertEquals(1, size)
            assertEquals("rootName", first().name)
        }

        val bar = cluster.getResourceNode("/api/root/{rootName}/bar/{barName}", true)
        assertEquals(2, bar!!.paramsInfo.size)
        assertEquals(1, bar.paramsInfo.count { it.value.requiredReferToOthers() })
        assertEquals("rootName", bar.paramsInfo.values.find { it.requiredReferToOthers() }!!.name)
        assertEquals(2, bar.getPossiblyBoundParams("GET", false).size)
        bar.getPossiblyBoundParams("POST-GET", false).apply {
            assertEquals(1, size)
            assertEquals("rootName", first().name)
        }

        val fooBarGet = cluster.getResourceNode("/api/root/{rootName}/foo/{fooName}/bar", true)
        assertEquals(2, fooBarGet!!.paramsInfo.size)
        assertEquals(2, fooBarGet.paramsInfo.count { it.value.requiredReferToOthers() })
        assertEquals(listOf("fooName","rootName"), fooBarGet.paramsInfo.values.filter { it.requiredReferToOthers() }.map { it.name }.sorted())
        assertEquals(2, fooBarGet.getPossiblyBoundParams("GET", false).size)
        fooBarGet.getPossiblyBoundParams("POST-GET", false).apply {
            assertEquals(2, size)
        }

        val fooBarPost = cluster.getResourceNode("/api/root/{rootName}/foo/{fooName}/bar/{barName}", true)
        assertEquals(3, fooBarPost!!.paramsInfo.size)
        assertEquals(2, fooBarPost.paramsInfo.count { it.value.requiredReferToOthers() })
        assertEquals(3, fooBarPost.paramsInfo.count { it.value.possiblyReferToOthers() })
        assertEquals(listOf("barName","fooName","rootName"), fooBarPost.paramsInfo.values.filter { it.possiblyReferToOthers() }.map { it.name }.sorted())
        assertEquals(2, fooBarPost.getPossiblyBoundParams("POST", false).size)
        fooBarPost.getPossiblyBoundParams("POST-POST", false).apply {
            assertEquals(2, size)
        }

    }

    // test post creation for resource node
    @ParameterizedTest
    @CsvSource(value = ["/api/root/{rootName},1","/api/root/{rootName}/bar/{barName},2", "/api/root/{rootName}/foo/{fooName},2", "/api/root/{rootName}/foo/{fooName}/bar,4"])
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
        "/api/root/{rootName}/foo/{fooName}/bar/{barName},POST,4",
        "/api/root/{rootName}/foo/{fooName},POST-POST,3",
        "/api/root/{rootName}/bar/{barName},GET,1",
        "/api/root/{rootName}/foo/{fooName}/bar,POST-GET,5"])
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

        /*
            POST /api/root/{rootName}
            POST /api/root/{rootName}/bar/{barName}
            POST /api/root/{rootName}/foo/{fooName}
            POST /api/root/{rootName}/foo/{fooName}/bar/{barName}
            GET /api/root/{rootName}/foo/{fooName}/bar
         */
        val node = cluster.getResourceNode("/api/root/{rootName}/foo/{fooName}/bar")
        assertNotNull(node)
        val call = node!!.createRestResourceCallBasedOnTemplate("POST-GET", randomness, 5)
        call.seeActions(ActionFilter.NO_SQL).apply {
            assertEquals(5, size)

            val postRoot = get(0) as RestCallAction
            val rootName0 = postRoot.parameters.find { it.name == "rootName" }
            assertNotNull(rootName0)
            val rootName0Gene = (rootName0!!.gene as? CustomMutationRateGene<*>)?.gene
            assertNotNull(rootName0Gene)

            val postBar = find { (it as RestCallAction).path.toString() ==  "/api/root/{rootName}/bar/{barName}"} as RestCallAction
            assertEquals(HttpVerb.POST, postBar.verb)
            val rootNamePostBar = postBar.parameters.find { it.name == "rootName" }
            assertNotNull(rootNamePostBar)
            val rootNamePostBarGene = (rootNamePostBar!!.gene as? CustomMutationRateGene<*>)?.gene
            assertNotNull(rootNamePostBarGene)

            val postFoo = find { (it as RestCallAction).path.toString() ==  "/api/root/{rootName}/foo/{fooName}"} as RestCallAction
            assertEquals(HttpVerb.POST, postFoo.verb)
            val fooNamePostFoo = postFoo.parameters.find { it.name == "fooName" }
            assertNotNull(fooNamePostFoo)
            val fooNamePostFooGene = (fooNamePostFoo!!.gene as? CustomMutationRateGene<*>)?.gene
            assertNotNull(fooNamePostFooGene)
            val rootNamePostFoo = postFoo.parameters.find { it.name == "rootName" }
            assertNotNull(rootNamePostFoo)
            val rootNamePostFooGene = (rootNamePostFoo!!.gene as? CustomMutationRateGene<*>)?.gene
            assertNotNull(rootNamePostFooGene)

            val postFooBar = get(3) as RestCallAction
            assertEquals(HttpVerb.POST, postFooBar.verb)
            val fooName3 = postFooBar.parameters.find { it.name == "fooName" }
            assertNotNull(fooName3)
            val fooName3Gene = (fooName3!!.gene as? CustomMutationRateGene<*>)?.gene
            assertNotNull(fooName3Gene)
            val rootName3 = postFooBar.parameters.find { it.name == "rootName" }
            assertNotNull(rootName3)
            val rootName3Gene = (rootName3!!.gene as? CustomMutationRateGene<*>)?.gene
            assertNotNull(rootName3Gene)

            val get = get(4) as RestCallAction
            assertEquals(HttpVerb.GET, get.verb)
            val fooName4 = get.parameters.find { it.name == "fooName" }
            assertNotNull(fooName4)
            val fooName4Gene = (fooName4!!.gene as? CustomMutationRateGene<*>)?.gene
            assertNotNull(fooName4Gene)
            val rootName4 = get.parameters.find { it.name == "rootName" }
            assertNotNull(rootName4)
            val rootNameLastGene = (rootName4!!.gene as? CustomMutationRateGene<*>)?.gene
            assertNotNull(rootNameLastGene)


            assertTrue(rootNameLastGene!!.isDirectBoundWith(rootName3Gene!!))
            assertTrue(rootNameLastGene.isDirectBoundWith(rootNamePostFooGene!!))
            assertTrue(rootNameLastGene.isDirectBoundWith(rootNamePostBarGene!!))
            assertTrue(rootNameLastGene.isDirectBoundWith(rootName0Gene!!))

            assertTrue(fooName4Gene!!.isDirectBoundWith(fooName3Gene!!))
            assertTrue(fooName4Gene.isDirectBoundWith(fooNamePostFooGene!!))


        }
    }

}