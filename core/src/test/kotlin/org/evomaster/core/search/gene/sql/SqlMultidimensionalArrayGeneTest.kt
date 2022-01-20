package org.evomaster.core.search.gene.sql

import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.StringGene
import org.junit.jupiter.api.Test
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.assertThrows

class SqlMultidimensionalArrayGeneTest {

    @Test
    fun testZeroDimensions() {
        val gene = SqlMultidimensionalArrayGene(
            "array",
            template = IntegerGene("element"),
            numberOfDimensions = 0
        )
        assertEquals(0, gene.numberOfDimensions)
    }


    @Test
    fun testInvalidNumberOfDimensions() {
        assertThrows<IllegalArgumentException> {
            SqlMultidimensionalArrayGene(
                "array",
                template = IntegerGene("element"),
                numberOfDimensions = -2
            )
        }
    }

    @Test
    fun testInvalidDimensionSize() {
        val gene = SqlMultidimensionalArrayGene(
            "array",
            template = IntegerGene("element"),
            numberOfDimensions = 1
        )
        assertEquals(0, gene.getDimensionSize(0))

        assertThrows<IndexOutOfBoundsException> {
            assertEquals(0, gene.getDimensionSize(1))
        }
    }

    @Test
    fun testOneDimensionalArray() {
        val gene = SqlMultidimensionalArrayGene(
            "array",
            template = IntegerGene("element"),
            numberOfDimensions = 1
        )

        assertEquals(1, gene.numberOfDimensions)
        assertEquals(0, gene.getDimensionSize(0))
    }

    @Test
    fun testOneDimensionalArrayGetElement() {
        val gene = SqlMultidimensionalArrayGene(
            "array",
            template = IntegerGene("element"),
            numberOfDimensions = 1
        )

        assertEquals(1, gene.numberOfDimensions)
        assertEquals(0, gene.getDimensionSize(0))

        gene.replaceElements(dimensionSizes = listOf(5))

        assertEquals(5, gene.getDimensionSize(0))

        assert(gene.getElement(listOf(0)) is IntegerGene)
        assert(gene.getElement(listOf(1)) is IntegerGene)
        assert(gene.getElement(listOf(2)) is IntegerGene)
        assert(gene.getElement(listOf(3)) is IntegerGene)
        assert(gene.getElement(listOf(4)) is IntegerGene)

        assertThrows<IndexOutOfBoundsException> {
            gene.getElement(listOf(-1))
        }

        assertThrows<IndexOutOfBoundsException> {
            gene.getElement(listOf(5))
        }
    }

    @Test
    fun testIncorrectGetElementListOfIndexes() {
        val gene = SqlMultidimensionalArrayGene(
            "matrix",
            template = IntegerGene("element"),
            numberOfDimensions = 2
        )

        assertThrows<IllegalArgumentException> {
            gene.getElement(listOf())
        }

        assertThrows<IllegalArgumentException> {
            gene.getElement(listOf(1))
        }

        assertThrows<IllegalArgumentException> {
            gene.getElement(listOf(1, 1, 1))
        }


    }

    @Test
    fun testTwoDimensionalArrayGetElement() {
        val gene = SqlMultidimensionalArrayGene(
            "matrix",
            template = IntegerGene("element"),
            numberOfDimensions = 2
        )

        assertEquals(2, gene.numberOfDimensions)
        assertEquals(0, gene.getDimensionSize(0))
        assertEquals(0, gene.getDimensionSize(1))

        gene.replaceElements(dimensionSizes = listOf(3, 2))

        assertEquals(3, gene.getDimensionSize(0))
        assertEquals(2, gene.getDimensionSize(1))

        assert(gene.getElement(listOf(0, 0)) is IntegerGene)
        assert(gene.getElement(listOf(1, 0)) is IntegerGene)
        assert(gene.getElement(listOf(2, 0)) is IntegerGene)

        assert(gene.getElement(listOf(0, 1)) is IntegerGene)
        assert(gene.getElement(listOf(1, 1)) is IntegerGene)
        assert(gene.getElement(listOf(2, 1)) is IntegerGene)

    }


    @Test
    fun testTwoDimensionalArray() {
        val gene = SqlMultidimensionalArrayGene(
            "matrix",
            template = IntegerGene("element"),
            numberOfDimensions = 2
        )

        assertEquals(2, gene.numberOfDimensions)

        assertEquals(0, gene.getDimensionSize(0))
        assertEquals(0, gene.getDimensionSize(1))

        gene.replaceElements(dimensionSizes = listOf(3, 7))

        assertEquals(3, gene.getDimensionSize(0))
        assertEquals(7, gene.getDimensionSize(1))

    }

    @Test
    fun testThreeDimensionalArray() {
        val gene = SqlMultidimensionalArrayGene(
            "matrix",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )

        assertEquals(0, gene.getDimensionSize(0))
        assertEquals(0, gene.getDimensionSize(1))
        assertEquals(0, gene.getDimensionSize(2))

        gene.replaceElements(listOf(5, 3, 7))

        assertEquals(5, gene.getDimensionSize(0))
        assertEquals(3, gene.getDimensionSize(1))
        assertEquals(7, gene.getDimensionSize(2))

        assert(gene.getElement(listOf(1, 2, 3)) is IntegerGene)
    }

    @Test
    fun testContainsSameValueAsWithEmptyArrays() {
        val emptyArray0 = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )
        val emptyArray1 = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )

        assertEquals(true, emptyArray0.containsSameValueAs(emptyArray1))
    }


    @Test
    fun testContainsSameValueAsWithNonEmptyArrays() {
        val nonEmptyArray0 = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )
        nonEmptyArray0.replaceElements(listOf(5, 3, 7))

        val nonEmptyArray1 = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )
        nonEmptyArray1.replaceElements(listOf(5, 3, 7))

        assertEquals(true, nonEmptyArray0.containsSameValueAs(nonEmptyArray1))
    }

    @Test
    fun testNotContainsSameValue() {
        val nonEmptyArray = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )
        nonEmptyArray.replaceElements(listOf(5, 3, 7))

        val emptyArray = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )

        assertEquals(false, emptyArray.containsSameValueAs(nonEmptyArray))
    }

    @Test
    fun testContainsSameValueAsWithDifferentElement() {
        val nonEmptyArray0 = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )
        nonEmptyArray0.replaceElements(listOf(5, 3, 7))
        (nonEmptyArray0.getElement(listOf(0, 0, 0)) as IntegerGene).value = 1

        val nonEmptyArray1 = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )
        nonEmptyArray1.replaceElements(listOf(5, 3, 7))

        assertEquals(false, nonEmptyArray0.containsSameValueAs(nonEmptyArray1))
    }

    @Test
    fun testValues() {
        val nonEmptyArray = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )
        nonEmptyArray.replaceElements(listOf(5, 3, 7))
        (nonEmptyArray.getElement(listOf(0, 0, 0)) as IntegerGene).value = 1
        (nonEmptyArray.getElement(listOf(1, 1, 1)) as IntegerGene).value = 2

        assertEquals(1, (nonEmptyArray.getElement(listOf(0, 0, 0)) as IntegerGene).value)
        assertEquals(2, (nonEmptyArray.getElement(listOf(1, 1, 1)) as IntegerGene).value)
        assertEquals(0, (nonEmptyArray.getElement(listOf(2, 2, 2)) as IntegerGene).value)

    }

    @Test
    fun testCopyValuesFrom() {
        val nonEmptyArray = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )
        nonEmptyArray.replaceElements(listOf(5, 3, 7))
        (nonEmptyArray.getElement(listOf(0, 0, 0)) as IntegerGene).value = 1
        (nonEmptyArray.getElement(listOf(1, 1, 1)) as IntegerGene).value = 2

        val copiedArray = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )

        copiedArray.copyValueFrom(nonEmptyArray)

        assertEquals(1, (copiedArray.getElement(listOf(0, 0, 0)) as IntegerGene).value)
        assertEquals(2, (copiedArray.getElement(listOf(1, 1, 1)) as IntegerGene).value)
        assertEquals(0, (copiedArray.getElement(listOf(2, 2, 2)) as IntegerGene).value)

    }

    @Test
    fun testCopyValuesFromWithDifferentDimensions() {
        val nonEmptyArray = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )
        nonEmptyArray.replaceElements(listOf(5, 3, 7))
        (nonEmptyArray.getElement(listOf(0, 0, 0)) as IntegerGene).value = 1
        (nonEmptyArray.getElement(listOf(1, 1, 1)) as IntegerGene).value = 2

        val copiedArray = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 1
        )

        assertThrows<IllegalArgumentException> {
            copiedArray.copyValueFrom(nonEmptyArray)
        }

    }

    @Test
    fun testBindValuesBasedOn() {
        val sourceArray = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )
        sourceArray.replaceElements(listOf(5, 3, 7))

        val targetArray = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )

        assertEquals(true, targetArray.bindValueBasedOn(sourceArray))

        assertEquals(5, targetArray.getDimensionSize(0))
        assertEquals(3, targetArray.getDimensionSize(1))
        assertEquals(7, targetArray.getDimensionSize(2))
    }

    @Test
    fun testFailedBindValuesBasedOn() {
        val sourceArray = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )
        val targetArray = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 5
        )

        assertEquals(false, targetArray.bindValueBasedOn(sourceArray))
    }

    @Test
    fun testFailedBindValuesBasedOnDifferentTemplates() {
        val sourceArray = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 3
        )
        val targetArray = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = StringGene("element"),
            numberOfDimensions = 3
        )
        assertEquals(false, targetArray.bindValueBasedOn(sourceArray))
    }

    @Test
    fun testGetPrintableValueOfArray() {
        val arrayGene = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 1
        )
        assertEquals("\"{}\"", arrayGene.getValueAsPrintableString())

        arrayGene.replaceElements(dimensionSizes = listOf(3))
        (arrayGene.getElement(listOf(0)) as IntegerGene).value = 1
        (arrayGene.getElement(listOf(1)) as IntegerGene).value = 2
        (arrayGene.getElement(listOf(2)) as IntegerGene).value = 3

        assertEquals("\"{1,2,3}\"", arrayGene.getValueAsPrintableString())

    }

    @Test
    fun testGetPrintableValueOfSquareMatrix() {
        val arrayGene = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 2
        )
        assertEquals("\"{}\"", arrayGene.getValueAsPrintableString())

        arrayGene.replaceElements(dimensionSizes = listOf(2,2))
        (arrayGene.getElement(listOf(0,0)) as IntegerGene).value = 1
        (arrayGene.getElement(listOf(0,1)) as IntegerGene).value = 2

        (arrayGene.getElement(listOf(1,0)) as IntegerGene).value = 3
        (arrayGene.getElement(listOf(1,1)) as IntegerGene).value = 4

        assertEquals("\"{{1,2},{3,4}}\"", arrayGene.getValueAsPrintableString())

    }

    @Test
    fun testGetPrintableValueOfNonSquareMatrix() {
        val arrayGene = SqlMultidimensionalArrayGene(
            "multidimensionaArray",
            template = IntegerGene("element"),
            numberOfDimensions = 2
        )
        assertEquals("\"{}\"", arrayGene.getValueAsPrintableString())

        arrayGene.replaceElements(dimensionSizes = listOf(1,3))
        (arrayGene.getElement(listOf(0,0)) as IntegerGene).value = 1
        (arrayGene.getElement(listOf(0,1)) as IntegerGene).value = 2
        (arrayGene.getElement(listOf(0,2)) as IntegerGene).value = 3

        assertEquals("\"{{1,2,3}}\"", arrayGene.getValueAsPrintableString())

        arrayGene.replaceElements(dimensionSizes = listOf(3,1))
        (arrayGene.getElement(listOf(0,0)) as IntegerGene).value = 1
        (arrayGene.getElement(listOf(1,0)) as IntegerGene).value = 2
        (arrayGene.getElement(listOf(2,0)) as IntegerGene).value = 3

        assertEquals("\"{{1},{2},{3}}\"", arrayGene.getValueAsPrintableString())

    }

    @Test
    fun testGetPrintableValueOfStringGenes() {
        val arrayGene = SqlMultidimensionalArrayGene(
            "stringArray",
            template = StringGene("element"),
            numberOfDimensions = 1
        )
        assertEquals("\"{}\"", arrayGene.getValueAsPrintableString())

        arrayGene.replaceElements(dimensionSizes = listOf(2))
        (arrayGene.getElement(listOf(0)) as StringGene).value = "Hello"
        (arrayGene.getElement(listOf(1)) as StringGene).value = "World"



        assertEquals("\"{\"Hello\",\"World\"}\"", arrayGene.getValueAsPrintableString())

    }

}