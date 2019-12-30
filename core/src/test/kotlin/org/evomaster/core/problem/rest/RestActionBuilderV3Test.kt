package org.evomaster.core.problem.rest

import io.swagger.parser.OpenAPIParser
import org.evomaster.core.problem.rest.param.FormParam
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.OptionalGene
import org.evomaster.core.search.gene.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class RestActionBuilderV3Test{

    private fun loadAndAssertActions(resourcePath: String, expectedNumberOfActions: Int)
            : MutableMap<String, Action> {


        val schema = OpenAPIParser().readLocation(resourcePath, null, null).openAPI

        val actions: MutableMap<String, Action> = mutableMapOf()

        RestActionBuilderV3.addActionsFromSwagger(schema, actions)

        assertEquals(expectedNumberOfActions, actions.size)

        return actions
    }

    // ----------- V3 --------------

    //TODO v3


    // ----------- V2 --------------

    @Test
    fun testMultiParamPath() {
        loadAndAssertActions("/swagger/artificial/multi_param_path.json", 1)
    }


    @Test
    fun testNews() {
        loadAndAssertActions("/swagger/sut/news.json", 7)
    }


    @Test
    fun testCatWatch() {
        val map = loadAndAssertActions("/swagger/sut/catwatch.json", 23)

        val postScoring = map["POST:/config/scoring.project"] as RestAction
        assertEquals(3, postScoring.seeGenes().size)
        val bodyPostScoring = postScoring.seeGenes().find { it.name == "body" }
        assertNotNull(bodyPostScoring)
        assertTrue(bodyPostScoring is OptionalGene)
        assertTrue((bodyPostScoring as OptionalGene).gene is StringGene)
    }

    @Test
    fun testProxyPrint() {
        loadAndAssertActions("/swagger/sut/proxyprint.json", 115)
    }

    @Test
    fun testCreateActions() {
        loadAndAssertActions("/swagger/artificial/positive_integer_swagger.json", 2)
    }


    @Test
    fun testOCVN() {
        loadAndAssertActions("/swagger/sut/ocvn_1oc.json", 192)
    }

    @Disabled("This is a bug in Swagger Core, reported at https://github.com/swagger-api/swagger-core/issues/2100")
    @Test
    fun testFeaturesServicesNull() {
        loadAndAssertActions("/swagger/sut/features_service_null.json", 18)
    }

    @Test
    fun testFeaturesServices() {
        loadAndAssertActions("/swagger/sut/features_service.json", 18)
    }

    @Test
    fun testScoutApi() {
        loadAndAssertActions("/swagger/sut/scout-api.json", 49)
    }


    @Test
    fun testBranches() {
        loadAndAssertActions("/swagger/artificial/branches.json", 3)
    }



    //TODO need to handle "multipart/form-data"
    @Disabled
    @Test
    fun testSimpleForm() {
        val actions = loadAndAssertActions("/swagger/artificial/simpleform.json", 1)

        val a = actions.values.first() as RestCallAction

        assertEquals(HttpVerb.POST, a.verb)
        assertEquals(2, a.parameters.size)
        assertEquals(2, a.parameters.filter { p -> p is FormParam }.size)
    }

    @Test
    fun testDuplicatedParamsInFeaturesServices() {
        val actions = loadAndAssertActions("/swagger/features_service.json", 18)
        (actions["POST:/products/{productName}/configurations/{configurationName}/features/{featureName}"] as RestCallAction).apply {
            assertEquals(3, parameters.size)
        }
    }


    @Test
    fun testApisGuru() {

        val actions = loadAndAssertActions("/swagger/apisguru-v2/apis.guru.json", 2)

        actions.values
                .filterIsInstance<RestCallAction>()
                .forEach {
                    assertEquals(2, it.produces.size)
                    assertTrue(it.produces.any{ p -> p.contains("application/json")})
                }
    }

    @Test
    fun testGreenPeace() {
        loadAndAssertActions("/swagger/apisguru-v2/greenpeace.org.json", 6)
    }
}