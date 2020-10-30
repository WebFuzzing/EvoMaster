package org.evomaster.core.problem.rest

import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.gene.DisruptiveGene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class RestPathTest{


    @Test
    fun testResolveMultiVariableInterluded(){

        val x = 7
        val xParam = PathParam("x", DisruptiveGene("d_", IntegerGene("x", x), 1.0))

        val y = 42
        val yParam = PathParam("y", DisruptiveGene("d_", IntegerGene("y", y), 1.0))

        val path = "/api/foo/{x}/{y}/x-{x}/{y}-y/a{x}b{y}c"
        val restPath = RestPath(path)

        val resolved = restPath.resolve(listOf(yParam,xParam))
        assertEquals("/api/foo/$x/$y/x-$x/$y-y/a${x}b${y}c", resolved)
    }

    @Test
    fun testResolveMultiVariable(){
        val x = 7
        val xParam = PathParam("x", DisruptiveGene("d_", IntegerGene("x", x), 1.0))

        val y = 42
        val yParam = PathParam("y", DisruptiveGene("d_", IntegerGene("y", y), 1.0))

        val path = "/api/foo/{x}{y}"
        val restPath = RestPath(path)

        val resolved = restPath.resolve(listOf(yParam,xParam))
        assertEquals("/api/foo/$x$y", resolved)
    }

    @Test
    fun testResolvePathWithVariable(){

        val id = 5
        val pathParam = PathParam("id", DisruptiveGene("d_", IntegerGene("id", id), 1.0))

        val path = "/api/foo/{id}"
        val restPath = RestPath(path)

        val resolved = restPath.resolve(listOf(pathParam))
        assertEquals("/api/foo/$id", resolved)
    }

    @Test
    fun testExtraSlashes(){

        val path = "/x/y/z"
        val restPath = RestPath("//$path")

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

    @Test
    fun testQueryInt(){

        val path = RestPath("/x")

        val a = QueryParam("a", IntegerGene("a", 5))

        val uri = path.resolve(listOf(a))

        assertEquals("/x?a=5", uri)
    }

    @Test
    fun testQueryString(){

        val path = RestPath("/x")

        val a = QueryParam("a", StringGene("a", "foo"))

        val uri = path.resolve(listOf(a))

        assertEquals("/x?a=foo", uri)
    }

    @Test
    fun testQueryCombination(){

        val path = RestPath("/x")

        val a = QueryParam("a", IntegerGene("a", 5))
        val b = QueryParam("b", StringGene("b", "foo"))

        val uri = path.resolve(listOf(a,b))

        assertEquals("/x?a=5&b=foo", uri)
    }

    @Test
    fun testQueryEmptySpace(){

        val path = RestPath("/x")

        val a = QueryParam("a", StringGene("a", "foo bar"))

        val uri = path.resolve(listOf(a))

        assertEquals("/x?a=foo+bar", uri)
    }

    @Test
    fun testQuerySquareBrackets(){

        val path = RestPath("/x")

        val a = QueryParam("a", StringGene("a", "[foo]"))

        val uri = path.resolve(listOf(a))

        assertEquals("/x?a=%5Bfoo%5D", uri)
    }

    @Test
    fun testQueryAndEqual(){

        val path = RestPath("/x")

        val a = QueryParam("a", StringGene("a", "foo &= bar"))
        val b = QueryParam("b", StringGene("b", "z"))

        val uri = path.resolve(listOf(a,b))

        assertEquals("/x?a=foo+%26%3D+bar&b=z", uri)
    }

    @Test
    fun testQueryQuotesAndUTF8(){

        val path = RestPath("/x")

        val a = QueryParam("a", StringGene("a", "\"A\" ± B"))

        val uri = path.resolve(listOf(a))

        assertEquals("/x?a=%22A%22+%C2%B1+B", uri)
    }

    @Test
    fun testEscapeInPath(){

        val path = RestPath("/x y")

        val uri = path.resolve(listOf())

        assertEquals("/x%20y", uri)
    }

    @Test
    fun testEscapeInPathAndQuery(){

        val path = RestPath("/x y")

        val a = QueryParam("a", StringGene("a", "k w"))

        val uri = path.resolve(listOf(a))

        assertEquals("/x%20y?a=k+w", uri)
    }

    @Test
    fun testPlus(){
        val path = RestPath("/x + y")

        val a = QueryParam("a", StringGene("a", "k + w"))

        val uri = path.resolve(listOf(a))

        assertEquals("/x%20+%20y?a=k+%2B+w", uri)
    }

    @Test
    fun testMatchResolvedPath(){
        val path = RestPath("/x/{y}/z")

        val resolvedPath = "/x/example/z"

        assertTrue(path.matches(resolvedPath))
    }

    @Test
    fun testMatchResolvedPathCompound(){
        val path = RestPath("/x/{y}-{anotherParam}/z")

        val resolvedPath = "/x/example-2/z"

        assertTrue(path.matches(resolvedPath))
    }

    @Test
    fun testMatchResolvedPathSemiCompound(){
        val path = RestPath("/x/y-{anotherParam}/z")

        val resolvedPath = "/x/y-2/z"

        assertTrue(path.matches(resolvedPath))
    }

    @Test
    fun testNotMatchResolvedPath(){
        val path = RestPath("/x/y")

        val resolvedPath = "/x/y/z"

        assertFalse(path.matches(resolvedPath))
    }

    @Test
    fun testNotMatchResolvedPathWithParam(){
        val path = RestPath("/x/y/{param}")

        val resolvedPath = "/x/error/param"

        assertFalse(path.matches(resolvedPath))
    }

    @Test
    fun testNotMatchResolvedPathCompound(){
        val path = RestPath("/x/{y}-{anotherParam}/z")

        val resolvedPath = "/x/example/z"

        assertFalse(path.matches(resolvedPath))
    }

    @Test
    fun testNotMatchResolvedPathSemiCompound(){
        val path = RestPath("/x/y-{anotherParam}/z")

        val resolvedPath = "/x/example/z"

        assertFalse(path.matches(resolvedPath))
    }

    @Test
    fun testGetKeyValuesWrongPath() {
        val path = RestPath("/x/{y}/{z}")

        val resolvedPath = "/x/example/example2/example3"

        assertNull(path.getKeyValues(resolvedPath))
    }

    @Test
    fun testGetKeyValues() {
        val path = RestPath("/x/{y}/{z}")

        val resolvedPath = "/x/example/z"

        val keyValues = path.getKeyValues(resolvedPath)

        assertEquals(2, keyValues?.size)
        assertEquals("example", keyValues?.get("y"))
        assertEquals("z", keyValues?.get("z"))
    }

    @Test
    fun testGetKeyValuesPathCompound(){
        val path = RestPath("/x/{y}-{anotherParam}/z")

        val resolvedPath = "/x/example-2/z"

        val keyValues = path.getKeyValues(resolvedPath)

        assertEquals(2, keyValues?.size)
        assertEquals("example", keyValues?.get("y"))
        assertEquals("2", keyValues?.get("anotherParam"))
    }

    @Test
    fun testGetKeyValuesPathSemiCompound(){
        val path = RestPath("/{firsParam}/y-{anotherParam}/{thirdParam}/z")

        val resolvedPath = "/x/y-2/z/z"

        val keyValues = path.getKeyValues(resolvedPath)

        assertEquals(3, keyValues?.size)
        assertEquals("x", keyValues?.get("firsParam"))
        assertEquals("2", keyValues?.get("anotherParam"))
        assertEquals("z", keyValues?.get("thirdParam"))
    }
}