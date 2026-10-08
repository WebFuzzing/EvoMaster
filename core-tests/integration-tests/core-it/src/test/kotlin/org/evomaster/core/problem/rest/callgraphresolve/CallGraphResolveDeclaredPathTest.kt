package org.evomaster.core.problem.rest.callgraphresolve

import bar.examples.it.spring.callgraphresolve.CallGraphResolveController
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.service.CallGraphService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class CallGraphResolveDeclaredPathTest : IntegrationTestRestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(CallGraphResolveController())
        }
    }

    @Test
    fun testResolveDeclaredPath() {

        val graph = injector.getInstance(CallGraphService::class.java)

        // exact literal path, no params
        val items = graph.resolveDeclaredPath("/items")!!
        assertEquals("/items", items.toString())

        // parametrized path resolved from a concrete path
        val itemById = graph.resolveDeclaredPath("/items/42")!!
        assertEquals("/items/{id}", itemById.toString())

        // when both a literal and a parametrized template match, the most specific
        // one (fewest path parameters) wins
        val itemSpecial = graph.resolveDeclaredPath("/items/special")!!
        assertEquals("/items/special", itemSpecial.toString())

        // DELETE-only path (no GET declared) must still resolve
        val constraint = graph.resolveDeclaredPath("/items/42/constraints/7")!!
        assertEquals("/items/{id}/constraints/{constraintId}", constraint.toString())
        val verbs = graph.endpointsForPath(constraint).map { it.verb }.toSet()
        assertEquals(setOf(HttpVerb.DELETE), verbs)

        // absolute URL: scheme/host/port/query must be stripped, only the path used
        val fromAbsoluteUrl = graph.resolveDeclaredPath("http://example.com:8080/items/42?x=1")!!
        assertEquals("/items/{id}", fromAbsoluteUrl.toString())

        // relative path with query params
        val fromRelativeWithQuery = graph.resolveDeclaredPath("/items/42?x=1")!!
        assertEquals("/items/{id}", fromRelativeWithQuery.toString())

        // not declared anywhere in the schema
        assertNull(graph.resolveDeclaredPath("/does/not/exist"))
    }
}
