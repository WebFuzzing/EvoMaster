package org.evomaster.core.search.gene.cassandra

import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.FixedMapGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.sql.SqlAutoIncrementGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CqlCollectionGeneTest {

    private fun arrayOfInts(vararg values: Int): ArrayGene<IntegerGene> {

        val gene = ArrayGene("elements", template = IntegerGene("element"))
        values.forEach { gene.addElement(IntegerGene("element", it)) }

        return gene
    }

    private fun mapOfTextToInt() =
        FixedMapGene("entries", key = StringGene("element"), value = IntegerGene("element"))

    @Test
    fun testMapCannotBeBuiltFromAnArray() {
        assertThrows<IllegalArgumentException> {
            CqlCollectionGene("favs", CqlCollectionKind.MAP, arrayOfInts())
        }
    }

    @Test
    fun testListAndSetCannotBeBuiltFromAMap() {
        assertThrows<IllegalArgumentException> {
            CqlCollectionGene("scores", CqlCollectionKind.LIST, mapOfTextToInt())
        }
        assertThrows<IllegalArgumentException> {
            CqlCollectionGene("tags", CqlCollectionKind.SET, mapOfTextToInt())
        }
    }

    @Test
    fun testCopyKeepsTheKindAndTheContent() {
        val gene = CqlCollectionGene("scores", CqlCollectionKind.LIST, arrayOfInts(1, 2))

        val copy = gene.copy() as CqlCollectionGene

        assertEquals(CqlCollectionKind.LIST, copy.kind)
        assertEquals(2, (copy.content as ArrayGene<*>).getViewOfChildren().size)
        assertTrue(gene.containsSameValueAs(copy))
    }

    /**
     * A list and a set holding the same elements are not the same value, as they are not even
     * written the same way in CQL.
     */
    @Test
    fun testListDoesNotContainTheSameValueAsASet() {
        val list = CqlCollectionGene("elements", CqlCollectionKind.LIST, arrayOfInts(1, 2))
        val set = CqlCollectionGene("elements", CqlCollectionKind.SET, arrayOfInts(1, 2))

        assertFalse(list.containsSameValueAs(set))
        assertFalse(set.containsSameValueAs(list))
    }

    @Test
    fun testCollectionsWithDifferentElementsDoNotContainTheSameValue() {
        val one = CqlCollectionGene("scores", CqlCollectionKind.LIST, arrayOfInts(1, 2))
        val other = CqlCollectionGene("scores", CqlCollectionKind.LIST, arrayOfInts(1, 3))

        assertFalse(one.containsSameValueAs(other))
    }

    /**
     * A gene that is not printable is left out of the insertion altogether, so a collection must
     * not claim to be printable when what it holds is not.
     */
    @Test
    fun testIsPrintableFollowsTheContent() {
        val printable = CqlCollectionGene("scores", CqlCollectionKind.LIST, arrayOfInts(1, 2))
        assertTrue(printable.isPrintable())

        //any gene that is not printable would do here
        val content = ArrayGene("elements", template = SqlAutoIncrementGene("element"))
        content.addElement(SqlAutoIncrementGene("element"))
        val notPrintable = CqlCollectionGene("scores", CqlCollectionKind.LIST, content)

        assertFalse(content.isPrintable())
        assertFalse(notPrintable.isPrintable())
    }
}