package org.evomaster.core.output.naming.rest

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.naming.LanguageConventionFormatter
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getRestCallAction
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import javax.lang.model.SourceVersion

class QueryParamsAmbiguitySolverTest {

    companion object {
        @JvmStatic
        fun queryParameters(): List<Arguments> = listOf(
            "jwt-encode-user" to "jwt_encode_user",
            "enabled.x" to "enabled_x",
            "enabled.y" to "enabled_y",
            "ordinary_name" to "ordinary_name"
        ).flatMap { (name, expected) ->
            listOf(
                Arguments.of(name, BooleanGene(name, true), listOf(expected)),
                Arguments.of(name, IntegerGene(name, value = -1), listOf("negative", expected)),
                Arguments.of(name, StringGene(name, value = ""), listOf("empty", expected))
            )
        }
    }

    @ParameterizedTest
    @MethodSource("queryParameters")
    fun sanitizeQueryParameterNames(name: String, gene: Gene, suffix: List<String>) {
        val param = QueryParam(name, gene)
        val action = getRestCallAction(parameters = mutableListOf(param))
        val tokens = QueryParamsAmbiguitySolver(true).apply(action, 100)
        assertEquals(listOf("with", "queryParam") + suffix, tokens)
        val methodName = LanguageConventionFormatter(OutputFormat.JAVA_JUNIT_4)
            .formatName(listOf("test") + tokens)
        assertTrue(SourceVersion.isIdentifier(methodName) && !SourceVersion.isKeyword(methodName), methodName)
        assertEquals(name, param.name)
    }

    @Test
    fun sanitizedNameMustFitRemainingBudget() {
        val action = getRestCallAction(parameters = mutableListOf(QueryParam("1x", BooleanGene("1x", true))))
        val solver = QueryParamsAmbiguitySolver(true)
        val prefix = listOf("with", "queryParam")
        val prefixLength = prefix.sumOf { it.length }
        assertEquals(prefix, solver.apply(action, prefixLength + 2))
        assertEquals(prefix + "_1x", solver.apply(action, prefixLength + 3))
    }
}
