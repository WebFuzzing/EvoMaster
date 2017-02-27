package org.evomaster.core.problem.rest

import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.search.gene.DisruptiveGene
import org.evomaster.core.search.gene.IntegerGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class RestPathTest{

    @Test
    fun testResolvePathWithVariable(){

        val id = 5
        val pathParam = PathParam("id", DisruptiveGene("", IntegerGene("id", id), 1.0))

        val path = "/api/foo/{id}"
        val restPath = RestPath(path)

        val resolved = restPath.resolve(listOf(pathParam))
        assertEquals("/api/foo/"+id, resolved)
    }

    @Test
    fun testExtraSlashes(){

        val path = "/x/y/z"
        val restPath = RestPath("//" + path)

        val toS = restPath.toString()
        assertEquals(path, toS)

        val resolved = restPath.resolve(listOf())
        assertEquals(path, resolved)
    }

    @Test
    fun testEquivalent(){

        val path = "/x/w"

        val a = RestPath(path)
        val b = RestPath(path)

        assertTrue(a.isEquivalent(b))
        assertTrue(b.isEquivalent(a))
        assertTrue(a.isEquivalent(a))

        val withParam = "/x/{w}"
        val c = RestPath(withParam)
        assertFalse(a.isEquivalent(c))
    }

    @Test
    fun testLastIsParam(){

        val a = RestPath("/x/w")
        val b = RestPath("/x/{w}")
        val c = RestPath("/x/{w}/y")
        val d = RestPath("/x/{w}/y/{z}")

        assertFalse(a.isLastElementAParameter())
        assertTrue( b.isLastElementAParameter())
        assertFalse(c.isLastElementAParameter())
        assertTrue( d.isLastElementAParameter())
    }

    @Test
    fun testDirectChild(){
        val a = RestPath("/x/w")
        val b = RestPath("/x/{w}")
        val c = RestPath("/x/{w}/y")
        val d = RestPath("/x/{w}/y/{z}")

        assertFalse(a.isDirectChildOf(a))
        assertFalse(a.isDirectChildOf(b))
        assertFalse(a.isDirectChildOf(c))
        assertFalse(a.isDirectChildOf(d))

        assertFalse(c.isDirectChildOf(a))
        assertTrue(c.isDirectChildOf(b))

        assertFalse(d.isDirectChildOf(b))
        assertTrue(d.isDirectChildOf(c))
    }
}