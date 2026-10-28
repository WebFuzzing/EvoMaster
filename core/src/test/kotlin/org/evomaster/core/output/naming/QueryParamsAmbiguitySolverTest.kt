package org.evomaster.core.output.naming

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getRestCallAction
import org.evomaster.core.output.naming.rest.QueryParamsAmbiguitySolver
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class QueryParamsAmbiguitySolverTest {

    @ParameterizedTest
    @CsvSource("jwt-encode-user,jwt_encode_user", "enabled.x,enabled_x", "enabled.y,enabled_y", "ordinary_name,ordinary_name")
    fun sanitizeQueryParameterNames(name: String, expected: String) {
        // WebGoat exposes jwt-encode-user and enabled.x/y as query parameters.
        // Their HTTP names must remain unchanged; only the test name is sanitized.
        val cases = listOf<Pair<Gene, List<String>>>(
            BooleanGene(name, true) to listOf(expected),
            IntegerGene(name, value = -1) to listOf("negative", expected),
            StringGene(name, value = "") to listOf("empty", expected)
        )
        for ((gene, suffix) in cases) {
            val param = QueryParam(name, gene)
            val action = getRestCallAction(parameters = mutableListOf(param))
            val tokens = QueryParamsAmbiguitySolver(true).apply(action, 100)
            assertEquals(listOf("with", "queryParam") + suffix, tokens)
            val methodName = LanguageConventionFormatter(OutputFormat.JAVA_JUNIT_4)
                .formatName(listOf("test") + tokens)
            assertTrue(methodName.all { Character.isJavaIdentifierPart(it) }, methodName)
            assertEquals(name, param.name)
        }
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
