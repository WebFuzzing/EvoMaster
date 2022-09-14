package org.evomaster.core.problem.graphql

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.formatter.OutputFormatter
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class GQLActionTest {

    @Test
    fun testQuotesInMutation(){
        val op = OptionalGene("foo", StringGene("foo", "foo\""))
        val param = GQInputParam("foo", op)
        val mutation = GraphQLAction("Mutation:add", "add", GQMethodType.MUTATION, mutableListOf(param))

        val body = GraphQLUtils.generateGQLBodyEntity(mutation, OutputFormat.KOTLIN_JUNIT_5)

        val expected = """
{
  "query": " mutation{ add  (foo : \"foo\\\"\")         } "
}
        """.trimIndent()

        assertNotNull(body)
        assertEquals(expected,  OutputFormatter.JSON_FORMATTER.getFormatted(body!!.entity))
    }

}