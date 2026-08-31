package org.evomaster.core.database.cassandra

import org.evomaster.core.search.gene.cassandra.CqlCollectionKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CqlCollectionTypeParserTest {

    @Test
    fun testList() {
        assertEquals(CqlCollectionType(CqlCollectionKind.LIST, listOf("int")), CqlCollectionTypeParser.parse("list<int>"))
    }

    @Test
    fun testSet() {
        assertEquals(CqlCollectionType(CqlCollectionKind.SET, listOf("text")), CqlCollectionTypeParser.parse("set<text>"))
    }

    @Test
    fun testMap() {
        assertEquals(
            CqlCollectionType(CqlCollectionKind.MAP, listOf("text", "int")),
            CqlCollectionTypeParser.parse("map<text, int>")
        )
    }

    /**
     * The comma separating the parameters of the nested collection is not one of the separators
     * between the parameters of the map.
     */
    @Test
    fun testNestedCollectionIsNotSplitOn() {
        assertEquals(
            CqlCollectionType(CqlCollectionKind.MAP, listOf("text", "map<int, text>")),
            CqlCollectionTypeParser.parse("map<text, map<int, text>>")
        )
    }

    @Test
    fun testFrozenMarkerIsPeeledOff() {
        assertEquals(CqlCollectionType(CqlCollectionKind.LIST, listOf("int")), CqlCollectionTypeParser.parse("frozen<list<int>>"))
    }

    @Test
    fun testFrozenMarkerOfANestedCollectionIsKeptForTheRecursion() {
        assertEquals(
            CqlCollectionType(CqlCollectionKind.MAP, listOf("text", "frozen<list<int>>")),
            CqlCollectionTypeParser.parse("map<text, frozen<list<int>>>")
        )
    }

    @Test
    fun testScalarTypeIsNotACollection() {
        listOf("int", "text", "duration", "inet").forEach {
            assertNull(CqlCollectionTypeParser.parse(it), "$it should not be a collection")
        }
    }

    /**
     * A tuple, a vector and a frozen user defined type are all written with type parameters without
     * being collections, so they have to be told apart from the ones that are.
     */
    @Test
    fun testOtherParameterizedTypesAreNotCollections() {
        listOf("tuple<int, text>", "vector<float, 3>", "frozen<myudt>").forEach {
            assertNull(CqlCollectionTypeParser.parse(it), "$it should not be a collection")
        }
    }

    @Test
    fun testWrongNumberOfParametersIsRejected() {
        assertThrows<IllegalArgumentException> { CqlCollectionTypeParser.parse("map<text>") }
        assertThrows<IllegalArgumentException> { CqlCollectionTypeParser.parse("list<int, text>") }
    }

    @Test
    fun testUnbalancedTypeParametersAreRejected() {
        assertThrows<IllegalArgumentException> { CqlCollectionTypeParser.parse("map<text, list<int>") }
    }
}