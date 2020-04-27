package org.evomaster.resource.rest.generator

import org.evomaster.resource.rest.generator.implementation.java.service.JavaResourceAPI
import org.evomaster.resource.rest.generator.implementation.java.service.JavaRestMethod
import org.evomaster.resource.rest.generator.model.GraphExportFormat
import org.evomaster.resource.rest.generator.model.RestMethod
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * created by manzh on 2020-01-07
 */
class TestServiceWithEndpoints {
    private val graph = ObjectsForTest.getGraph()
    private val usedmethods = listOf(RestMethod.GET_ID, RestMethod.GET_ALL, RestMethod.GET_ALL_CON, RestMethod.PUT, RestMethod.DELETE, RestMethod.DELETE_CON ,RestMethod.PATCH_VALUE)
    private val methodsWithPostId = listOf(RestMethod.POST_ID).plus(usedmethods)
    private val methodsWithPostObj= listOf(RestMethod.POST).plus(usedmethods)


    @Test
    fun testServiceAPIWithShown(){
        val config = GenConfig()
        config.saveGraph = GraphExportFormat.NONE
        config.restMethods = methodsWithPostId
        config.hideExistsDependency = false

        val gen = GenerateREST(config, graph).getResourceCluster()

        assertEquals(graph.nodes.size, gen.size)

        val a = "A"
        val d = "D"

        val referredNode = listOf("B", "C", d, "E")
        val referredNodeId = referredNode.map { FormatUtil.formatResourceIdAsPathParam(it, config.idName) }

        val resASpec = gen.getValue(a)
        assert(resASpec.pathParams.containsAll(referredNodeId))

        val resDSpec = gen.getValue(d)
        assert(resDSpec.pathParams.isEmpty())

        val serviceA = resASpec.getApiService()
        assertNotNull(serviceA)
        assertEquals(2, serviceA!!.dto.referToOthers.size)
        assertEquals(2, serviceA.dto.ownOthers.size)
        assert(serviceA.pathParams.containsAll(referredNodeId))

        val serviceD = resDSpec.getApiService()
        assertNotNull(serviceD)
        assertEquals(0, serviceD!!.dto.referToOthers.size)
        assertEquals(0, serviceD.dto.ownOthers.size)
        assert(serviceD.pathParams.isEmpty())

        val apiD = JavaResourceAPI(serviceD)
        val methodsD = apiD.getMethods()
        //since D does not have outgoings, prohibit get all con and delete con
        assertEquals(methodsWithPostId.size - 2, methodsD.size)

        val expectedAPath = "${
        referredNode.reversed()
                .joinToString("") { "/${FormatUtil.formatResourceOnPath(it)}/{${FormatUtil.formatResourceIdAsPathParam(it, config.idName)}}" }
        }/${FormatUtil.formatResourceOnPath("A")}"

        assertEquals(expectedAPath, resASpec.pathWithId)

        val expectedAPathWithId = "$expectedAPath/{${FormatUtil.formatResourceIdAsPathParam("A", config.idName)}}"

        val apiA = JavaResourceAPI(serviceA)
        val methods = apiA.getMethods()
        //since A has outgoings, a number of applied method should be all
        assertEquals(methodsWithPostId.size, methods.size)

        //for all methods, num of tags and num of params should be same
        methods.forEach {
            val m = it as? JavaRestMethod ?: fail("incorrect method type")
            assertEquals(m.getParamTag().size, m.getParamTag().size)
        }

        //post id
        val postObj = methods.find { it is JavaRestMethod && it.method == RestMethod.POST_ID } as? JavaRestMethod
        assertNotNull(postObj)
        assertEquals(expectedAPathWithId, postObj!!.getPath())
        assert(postObj.getParams().keys.containsAll(referredNodeId))


        //put obj
        val putObj = methods.find { it is JavaRestMethod && it.method == RestMethod.PUT } as? JavaRestMethod
        assertNotNull(putObj)
        assertEquals(expectedAPathWithId, putObj!!.getPath())
        assert(putObj.getParams().keys.containsAll(referredNodeId))

        //patch values
        val patchVal = methods.find { it is JavaRestMethod && it.method == RestMethod.PATCH_VALUE } as? JavaRestMethod
        assertNotNull(patchVal)
        assertEquals(expectedAPathWithId, patchVal!!.getPath())
        assert(patchVal.getParams().keys.containsAll(referredNodeId.plus(FormatUtil.formatResourceIdAsPathParam(a, config.idName))))

        //delete con
        val deleteCon = methods.find { it is JavaRestMethod && it.method == RestMethod.DELETE_CON } as? JavaRestMethod
        assertNotNull(deleteCon)
        assertEquals(expectedAPathWithId, deleteCon!!.getPath())
        assert(deleteCon.getParams().keys.containsAll(referredNodeId.plus(FormatUtil.formatResourceIdAsPathParam(a, config.idName))))

        //delete
        val delete = methods.find { it is JavaRestMethod && it.method == RestMethod.DELETE } as? JavaRestMethod
        assertNotNull(delete)
        assertEquals("/${FormatUtil.formatResourceOnPath(a)}/{${FormatUtil.formatResourceIdAsPathParam(a, config.idName)}}", delete!!.getPath())
        assert(delete.getParams().keys.containsAll(listOf(FormatUtil.formatResourceIdAsPathParam(a, config.idName))))

        //get all
        val getAll = methods.find { it is JavaRestMethod && it.method == RestMethod.GET_ALL } as? JavaRestMethod
        assertNotNull(getAll)
        assertEquals("/${serviceA.resourceOnPath}", getAll!!.getPath())
        assert(getAll.getParams().isEmpty())

        //get all with conditions
        val getConAll = methods.find { it is JavaRestMethod && it.method == RestMethod.GET_ALL_CON } as? JavaRestMethod
        assertNotNull(getConAll)
        assertEquals(expectedAPath, getConAll!!.getPath())
        assert(getConAll.getParams().keys.containsAll(referredNodeId))

        //get by id
        val get = methods.find { it is JavaRestMethod && it.method == RestMethod.GET_ID } as? JavaRestMethod
        assertNotNull(get)
        assertEquals(expectedAPathWithId, get!!.getPath())
        assert(get.getParams().keys.containsAll(referredNodeId.plus(FormatUtil.formatResourceIdAsPathParam(a, config.idName))))
    }

    @Test
    fun testServiceAPIWithHide(){
        val config = GenConfig()
        config.saveGraph = GraphExportFormat.NONE
        config.hideExistsDependency = true
        config.restMethods = methodsWithPostObj

        val gen = GenerateREST(config, graph).getResourceCluster()

        assertEquals(graph.nodes.size, gen.size)

        val a = "A"

        val resASpec = gen.getValue(a)
        assert(resASpec.pathParams.isEmpty())

        val serviceA = resASpec.getApiService()
        assertNotNull(serviceA)
        assertEquals(2, serviceA!!.dto.referToOthers.size)
        assertEquals(2, serviceA.dto.ownOthers.size)
        assert(serviceA.pathParams.isEmpty())

        val expectedAPath = "/${FormatUtil.formatResourceOnPath(a)}"

        assertEquals(expectedAPath, resASpec.pathWithId)

        val expectedAPathWithId = "$expectedAPath/{${FormatUtil.formatResourceIdAsPathParam("A", config.idName)}}"

        val apiA = JavaResourceAPI(serviceA)
        val methods = apiA.getMethods()

        //since dependency is hidden, condition optional such as get all and delete should be prohibited
        assertEquals(config.restMethods.filter { it != RestMethod.GET_ALL_CON && it != RestMethod.DELETE_CON }.size, methods.size)

        //for all methods, num of tags and num of params should be same
        methods.forEach {
            val m = it as? JavaRestMethod ?: fail("incorrect method type")
            assertEquals(m.getParamTag().size, m.getParamTag().size)
        }

        //post obj
        val postObj = methods.find { it is JavaRestMethod && it.method == RestMethod.POST } as? JavaRestMethod
        assertNotNull(postObj)
        assertEquals(expectedAPath, postObj!!.getPath())


        //put obj
        val putObj = methods.find { it is JavaRestMethod && it.method == RestMethod.PUT } as? JavaRestMethod
        assertNotNull(putObj)
        assertEquals(expectedAPathWithId, putObj!!.getPath())

        //patch values
        val patchVal = methods.find { it is JavaRestMethod && it.method == RestMethod.PATCH_VALUE } as? JavaRestMethod
        assertNotNull(patchVal)
        assertEquals(expectedAPathWithId, patchVal!!.getPath())

        //delete
        val delete = methods.find { it is JavaRestMethod && it.method == RestMethod.DELETE } as? JavaRestMethod
        assertNotNull(delete)
        assertEquals("/${FormatUtil.formatResourceOnPath(a)}/{${FormatUtil.formatResourceIdAsPathParam(a, config.idName)}}", delete!!.getPath())
        assert(delete.getParams().keys.containsAll(listOf(FormatUtil.formatResourceIdAsPathParam(a, config.idName))))

        //get all
        val getAll = methods.find { it is JavaRestMethod && it.method == RestMethod.GET_ALL } as? JavaRestMethod
        assertNotNull(getAll)
        assertEquals("/${serviceA.resourceOnPath}", getAll!!.getPath())
        assert(getAll.getParams().isEmpty())

        //get by id
        val get = methods.find { it is JavaRestMethod && it.method == RestMethod.GET_ID } as? JavaRestMethod
        assertNotNull(get)
        assertEquals(expectedAPathWithId, get!!.getPath())
    }
}