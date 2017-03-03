package org.evomaster.core.problem.rest.service

import io.swagger.parser.SwaggerParser
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.param.FormParam
import org.evomaster.core.search.Action
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class RestActionBuilderTest {

    @Test
    fun testCreateActions(){

        val swagger = SwaggerParser().read("/positive_integer_swagger.json")

        val builder = RestActionBuilder()
        val actions: MutableMap<String, Action> = mutableMapOf()
        builder.createActions(swagger, actions)

        assertEquals(2, actions.size)
    }


    @Disabled("This is a bug in Swagger Core, reported at https://github.com/swagger-api/swagger-core/issues/2100")
    @Test
    fun testFeaturesServicesNull(){

        val swagger = SwaggerParser().read("/features_service_null.json")

        val builder = RestActionBuilder()
        val actions: MutableMap<String, Action> = mutableMapOf()
        builder.createActions(swagger, actions)

        assertEquals(18, actions.size)
    }

    @Test
    fun testFeaturesServices(){

        val swagger = SwaggerParser().read("/features_service.json")

        val builder = RestActionBuilder()
        val actions: MutableMap<String, Action> = mutableMapOf()
        builder.createActions(swagger, actions)

        assertEquals(18, actions.size)
    }

    @Test
    fun testScoutApi(){

        val swagger = SwaggerParser().read("/scout-api.json")

        val builder = RestActionBuilder()
        val actions: MutableMap<String, Action> = mutableMapOf()
        builder.createActions(swagger, actions)

        assertEquals(49, actions.size)
    }


    @Test
    fun testBranches(){

        val swagger = SwaggerParser().read("/branches.json")

        val builder = RestActionBuilder()
        val actions: MutableMap<String, Action> = mutableMapOf()
        builder.createActions(swagger, actions)

        assertEquals(3, actions.size)
    }


    @Test
    fun testSimpleForm(){

        val swagger = SwaggerParser().read("/simpleform.json")

        val builder = RestActionBuilder()
        val actions: MutableMap<String, Action> = mutableMapOf()
        builder.createActions(swagger, actions)

        assertEquals(1, actions.size)
        val a = actions.values.first() as RestCallAction

        assertEquals(HttpVerb.POST, a.verb)
        assertEquals(2, a.parameters.size)
        assertEquals(2, a.parameters.filter { p -> p is FormParam }.size)
    }
}