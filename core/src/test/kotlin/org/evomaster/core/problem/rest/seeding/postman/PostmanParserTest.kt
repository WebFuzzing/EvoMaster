package org.evomaster.core.problem.rest.seeding.postman

import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.Action
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PostmanParserTest {

    @Test
    fun testPostmanParser() {
        val swaggerPath = "src/test/resources/swagger/postman/comments.yaml"
        val swagger = OpenAPIParser().readLocation(swaggerPath, null, null).openAPI
        val postmanParser = PostmanParser(loadRestCallActions(swagger), swagger)
        val testCases = postmanParser.parseTestCases("src/test/resources/postman/comments_collection.postman_collection.json")
        assertEquals(16, testCases.size)
    }

    private fun loadRestCallActions(swagger: OpenAPI): List<RestCallAction> {
        val actions: MutableMap<String, Action> = mutableMapOf()

        RestActionBuilderV3.addActionsFromSwagger(swagger, actions)

        return actions
                .asSequence()
                .sortedBy { e -> e.key }
                .map { e -> e.value }
                .toList()
                .filterIsInstance<RestCallAction>()
    }
}