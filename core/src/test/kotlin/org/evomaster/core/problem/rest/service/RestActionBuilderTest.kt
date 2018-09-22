package org.evomaster.core.problem.rest.service

import io.swagger.parser.SwaggerParser
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestActionBuilder
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.param.FormParam
import org.evomaster.core.search.Action
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class RestActionBuilderTest {

    private fun loadAndAssertActions(resourcePath: String, expectedNumberOfActions: Int)
            : MutableMap<String, Action> {

        val swagger = SwaggerParser().read(resourcePath)

        val actions: MutableMap<String, Action> = mutableMapOf()
        RestActionBuilder.addActionsFromSwagger(swagger, actions)

        assertEquals(expectedNumberOfActions, actions.size)

        return actions
    }

    @Test
    fun testNews() {
        loadAndAssertActions("/swagger/news.json", 7)
    }


    @Test
    fun testCatWatch() {
        loadAndAssertActions("/swagger/catwatch.json", 23)
    }

    @Test
    fun testProxyPrint() {
        loadAndAssertActions("/swagger/proxyprint.json", 115)
    }

    @Test
    fun testCreateActions() {
        loadAndAssertActions("/swagger/positive_integer_swagger.json", 2)
    }


    @Test
    fun testOCVN() {
        loadAndAssertActions("/swagger/ocvn_1oc.json", 192)
    }

    @Disabled("This is a bug in Swagger Core, reported at https://github.com/swagger-api/swagger-core/issues/2100")
    @Test
    fun testFeaturesServicesNull() {
        loadAndAssertActions("/swagger/features_service_null.json", 18)
    }

    @Test
    fun testFeaturesServices() {
        loadAndAssertActions("/swagger/features_service.json", 18)
    }

    @Test
    fun testScoutApi() {
        loadAndAssertActions("/swagger/scout-api.json", 49)
    }


    @Test
    fun testBranches() {
        loadAndAssertActions("/swagger/branches.json", 3)
    }


    @Test
    fun testSimpleForm() {
        val actions = loadAndAssertActions("/swagger/simpleform.json", 1)

        val a = actions.values.first() as RestCallAction

        assertEquals(HttpVerb.POST, a.verb)
        assertEquals(2, a.parameters.size)
        assertEquals(2, a.parameters.filter { p -> p is FormParam }.size)
    }
}