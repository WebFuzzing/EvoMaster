package org.evomaster.core.problem.rest.seeding.postman

import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PostmanParserTest {

    @Test
    fun testPostmanParserAllParamTypes() {
        val swaggerPath = "src/test/resources/swagger/postman/all_param_types.yaml"
        val swagger = OpenAPIParser().readLocation(swaggerPath, null, null).openAPI
        val postmanParser = PostmanParser(loadRestCallActions(swagger), swagger)
        val testCases = postmanParser.parseTestCases("src/test/resources/postman/all_param_types.postman_collection.json")

        assertEquals(1, testCases.size)

        // Assert each gene of the request
        val request = testCases[0][0]

        assertEquals("pathParamValue", (request.parameters.filterIsInstance<PathParam>()[0].gene as DisruptiveGene<StringGene>).gene.value)

        assertEquals(true, (request.parameters.filterIsInstance<HeaderParam>()[0].gene as OptionalGene).isActive)
        assertEquals("string3", ((request.parameters.filterIsInstance<HeaderParam>()[0].gene as OptionalGene).gene as StringGene).value)

        assertEquals("string2", (request.parameters.find { it.name == "reqStringQueryParam" }?.gene as StringGene).value)

        assertEquals(true, (request.parameters.find { it.name == "optStringQueryParam" }?.gene as OptionalGene).isActive)
        assertEquals("string1", ((request.parameters.find { it.name == "optStringQueryParam" }?.gene as OptionalGene).gene as StringGene).value)

        val optEnumStringGene = request.parameters.find { it.name == "optStringEnumQueryParam" }?.gene as OptionalGene
        assertEquals(true, optEnumStringGene.isActive)
        assertEquals("val2", (optEnumStringGene.gene as EnumGene<*>).values[(optEnumStringGene.gene as EnumGene<*>).index])

        assertEquals(true, (request.parameters.find { it.name == "optIntQueryParam" }?.gene as OptionalGene).isActive)
        assertEquals(10, ((request.parameters.find { it.name == "optIntQueryParam" }?.gene as OptionalGene).gene as IntegerGene).value)

        val optEnumIntGene = request.parameters.find { it.name == "optIntEnumQueryParam" }?.gene as OptionalGene
        assertEquals(true, optEnumIntGene.isActive)
        assertEquals(3, (optEnumIntGene.gene as EnumGene<*>).values[(optEnumIntGene.gene as EnumGene<*>).index])

        assertEquals(true, (request.parameters.find { it.name == "optBase64QueryParam" }?.gene as OptionalGene).isActive)
        assertEquals("ZXhhbXBsZQ==", ((request.parameters.find { it.name == "optBase64QueryParam" }?.gene as OptionalGene).gene as Base64StringGene).data.value)

        assertEquals(true, (request.parameters.find { it.name == "optBoolQueryParam" }?.gene as OptionalGene).isActive)
        assertEquals(true, ((request.parameters.find { it.name == "optBoolQueryParam" }?.gene as OptionalGene).gene as BooleanGene).value)

        assertEquals(true, (request.parameters.find { it.name == "optDateQueryParam" }?.gene as OptionalGene).isActive)
        assertEquals(2020, ((request.parameters.find { it.name == "optDateQueryParam" }?.gene as OptionalGene).gene as DateGene).year.value)
        assertEquals(12, ((request.parameters.find { it.name == "optDateQueryParam" }?.gene as OptionalGene).gene as DateGene).month.value)
        assertEquals(14, ((request.parameters.find { it.name == "optDateQueryParam" }?.gene as OptionalGene).gene as DateGene).day.value)

        assertEquals(true, (request.parameters.find { it.name == "optTimeQueryParam" }?.gene as OptionalGene).isActive)
        assertEquals("13:45:08", ((request.parameters.find { it.name == "optTimeQueryParam" }?.gene as OptionalGene).gene as StringGene).value)

        assertEquals(true, (request.parameters.find { it.name == "optDateTimeQueryParam" }?.gene as OptionalGene).isActive)
        assertEquals(2020, ((request.parameters.find { it.name == "optDateTimeQueryParam" }?.gene as OptionalGene).gene as DateTimeGene).date.year.value)
        assertEquals(12, ((request.parameters.find { it.name == "optDateTimeQueryParam" }?.gene as OptionalGene).gene as DateTimeGene).date.month.value)
        assertEquals(14, ((request.parameters.find { it.name == "optDateTimeQueryParam" }?.gene as OptionalGene).gene as DateTimeGene).date.day.value)
        assertEquals(13, ((request.parameters.find { it.name == "optDateTimeQueryParam" }?.gene as OptionalGene).gene as DateTimeGene).time.hour.value)
        assertEquals(45, ((request.parameters.find { it.name == "optDateTimeQueryParam" }?.gene as OptionalGene).gene as DateTimeGene).time.minute.value)
        assertEquals(8, ((request.parameters.find { it.name == "optDateTimeQueryParam" }?.gene as OptionalGene).gene as DateTimeGene).time.second.value)

        assertEquals(true, (request.parameters.find { it.name == "optDoubleQueryParam" }?.gene as OptionalGene).isActive)
        assertEquals(12.143425253, ((request.parameters.find { it.name == "optDoubleQueryParam" }?.gene as OptionalGene).gene as DoubleGene).value)

        assertEquals(true, (request.parameters.find { it.name == "optFloatQueryParam" }?.gene as OptionalGene).isActive)
        assertEquals(1.9f, ((request.parameters.find { it.name == "optFloatQueryParam" }?.gene as OptionalGene).gene as FloatGene).value)

        assertEquals(true, (request.parameters.find { it.name == "optLongQueryParam" }?.gene as OptionalGene).isActive)
        assertEquals(3147483647, ((request.parameters.find { it.name == "optLongQueryParam" }?.gene as OptionalGene).gene as LongGene).value)

        assertEquals(true, (request.parameters.find { it.name == "optArrayQueryParam" }?.gene as OptionalGene).isActive)
        assertEquals(6, ((request.parameters.find { it.name == "optArrayQueryParam" }?.gene as OptionalGene).gene as ArrayGene<EnumGene<*>>).maxSize)
        assertTrue(((request.parameters.find { it.name == "optArrayQueryParam" }?.gene as OptionalGene).gene as ArrayGene<EnumGene<*>>).template.values.containsAll(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
        assertTrue(((request.parameters.find { it.name == "optArrayQueryParam" }?.gene as OptionalGene).gene as ArrayGene<EnumGene<*>>).elements.map { it.values[it.index] }.containsAll(listOf(1, 2, 3, 4, 5, 6)))
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