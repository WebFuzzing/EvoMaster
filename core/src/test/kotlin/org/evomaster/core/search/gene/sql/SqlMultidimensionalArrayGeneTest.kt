package org.evomaster.core.search.gene.sql

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SqlMultidimensionalArrayGeneTest {

    private val rand = Randomness()

    @BeforeEach
    fun initRand() {
        rand.updateSeed(42)
    }

    private fun sampleOneDimensionalArrayOfIntegerGenes(size: Int): SqlMultidimensionalArrayGene<IntegerGene> {
        val gene = SqlMultidimensionalArrayGene(
                "matrix",
                template = IntegerGene("element"),
                numberOfDimensions = 1
        )
        gene.doInitialize(rand)
        do {
            gene.randomize(rand, tryToForceNewValue = false)
        } while (gene.getDimensionSize(0) != size)

        return gene
    }

    private fun sampleTwoDimensionalArrayOfIntegerGenes(rows: Int,
                                                        columns: Int,
                                                        databaseType: DatabaseType = DatabaseType.POSTGRES): SqlMultidimensionalArrayGene<IntegerGene> {
        val gene = SqlMultidimensionalArrayGene(
                "matrix",
                databaseType = databaseType,
                template = IntegerGene("element"),
                numberOfDimensions = 2
        )
        gene.doInitialize(rand)

        do {
            gene.randomize(rand, tryToForceNewValue = false)
        } while (gene.getDimensionSize(0) != rows
                || gene.getDimensionSize(1) != columns)

        return gene
    }

    private fun sampleThreeDimensionalArrayOfIntegerGenes(rows: Int, columns: Int, pages: Int): SqlMultidimensionalArrayGene<IntegerGene> {
        val gene = SqlMultidimensionalArrayGene(
                "matrix",
                template = IntegerGene("element"),
                numberOfDimensions = 3
        )
        gene.doInitialize(rand)
        do {
            gene.randomize(rand, tryToForceNewValue = false)
        } while (gene.getDimensionSize(0) != rows
                || gene.getDimensionSize(1) != columns
                || gene.getDimensionSize(2) != pages)

        return gene
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
    fun testZeroNumberOfDimensionsArray() {
        assertThrows<IllegalArgumentException> {
            SqlMultidimensionalArrayGene(
                    "multidimensionaArray",
                    template = IntegerGene("element"),
                    numberOfDimensions = 0
            )
        }
    }

    @Test
    fun testOneDimensionalArrayIsValid() {
        val gene = sampleOneDimensionalArrayOfIntegerGenes(3)
        assertTrue(gene.isLocallyValid())
    }

    @Test
    fun testTwoDimensionalArrayIsValid() {
        val gene = sampleTwoDimensionalArrayOfIntegerGenes(3,2)
        assertTrue(gene.isLocallyValid())
    }

    @Test
    fun testThreeDimensionalArrayIsValid() {
        val gene = sampleThreeDimensionalArrayOfIntegerGenes(3,2,3)
        assertTrue(gene.isLocallyValid())
    }

    @Test
    fun testValidDimensionIndex() {
        val gene = SqlMultidimensionalArrayGene(
                "array",
                template = IntegerGene("element"),
                numberOfDimensions = 1
        )
        assertFalse(gene.initialized)
        gene.doInitialize(rand)
        assertTrue(gene.initialized)

        assertTrue(gene.getDimensionSize(0) >= 0)
    }

    @Test
    fun testInvalidDimensionIndex() {
        val gene = sampleOneDimensionalArrayOfIntegerGenes(3)
        assertThrows<IndexOutOfBoundsException> {
            gene.getDimensionSize(1)
        }
    }


    @Test
    fun testOneDimensionalArrayGetElement() {
        val gene = sampleOneDimensionalArrayOfIntegerGenes(3)
        assertEquals(3, gene.getDimensionSize(0))

        assertThrows<IndexOutOfBoundsException> {
            gene.getElement(listOf(-1))
        }
        gene.getElement(listOf(0))
        gene.getElement(listOf(1))
        gene.getElement(listOf(2))
        assertThrows<IndexOutOfBoundsException> {
            gene.getElement(listOf(3))
        }

        /**
         * All genes should be recursively initialized
         */
        // TODO FixME
        //assertTrue(elem0.initialized)
        //assertTrue(elem1.initialized)
        //assertTrue(elem2.initialized)
    }

    @Test
    fun testIncorrectGetElementListOfIndexes() {
        val gene = sampleTwoDimensionalArrayOfIntegerGenes(2, 3)

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
        val gene = sampleTwoDimensionalArrayOfIntegerGenes(rows = 3, columns = 2)

        assertEquals(2, gene.numberOfDimensions)
        assertEquals(3, gene.getDimensionSize(0))
        assertEquals(2, gene.getDimensionSize(1))


    }


    @Test
    fun testThreeDimensionalArray() {
        val gene = sampleThreeDimensionalArrayOfIntegerGenes(rows = 2, columns = 3, pages = 4)

        assertEquals(2, gene.getDimensionSize(0))
        assertEquals(3, gene.getDimensionSize(1))
        assertEquals(4, gene.getDimensionSize(2))

        gene.getElement(listOf(1, 2, 3))
    }

    @Test
    fun testContainsSameValueAsWithEmptyArrays() {
        val emptyArray0 = sampleOneDimensionalArrayOfIntegerGenes(0)
        assertEquals(0, emptyArray0.getDimensionSize(0))

        val emptyArray1 = sampleOneDimensionalArrayOfIntegerGenes(0)
        assertEquals(0, emptyArray1.getDimensionSize(0))

        assertEquals(true, emptyArray0.containsSameValueAs(emptyArray1))
    }


    @Test
    fun testContainsSameValueAsWithNonEmptyArrays() {
        val nonEmptyArray0 = sampleOneDimensionalArrayOfIntegerGenes(2)
        nonEmptyArray0.getElement(listOf(0)).value = 0
        nonEmptyArray0.getElement(listOf(1)).value = 1

        val nonEmptyArray1 = sampleOneDimensionalArrayOfIntegerGenes(2)
        nonEmptyArray1.getElement(listOf(0)).value = 0
        nonEmptyArray1.getElement(listOf(1)).value = 1

        assertEquals(true, nonEmptyArray0.containsSameValueAs(nonEmptyArray1))
    }

    @Test
    fun testNotContainsSameValue() {
        val nonEmptyArray = sampleOneDimensionalArrayOfIntegerGenes(1)
        val emptyArray = sampleOneDimensionalArrayOfIntegerGenes(0)
        assertEquals(false, nonEmptyArray.containsSameValueAs(emptyArray))
        assertEquals(false, emptyArray.containsSameValueAs(nonEmptyArray))
    }

    @Test
    fun testContainsSameValueAsWithDifferentElement() {
        val array = sampleThreeDimensionalArrayOfIntegerGenes(1, 1, 1)
        val copy = array.copy() as SqlMultidimensionalArrayGene<IntegerGene>
        assertTrue(array.containsSameValueAs(copy))
        copy.getElement(listOf(0, 0, 0)).value++
        assertFalse(array.containsSameValueAs(copy))
    }

    @Test
    fun testValues() {
        val array = sampleThreeDimensionalArrayOfIntegerGenes(3, 3, 3)
        array.getElement(listOf(0, 0, 0)).value = 1
        array.getElement(listOf(1, 1, 1)).value = 2
        array.getElement(listOf(2, 2, 2)).value = 0

        assertEquals(1, array.getElement(listOf(0, 0, 0)).value)
        assertEquals(2, array.getElement(listOf(1, 1, 1)).value)
        assertEquals(0, array.getElement(listOf(2, 2, 2)).value)

    }

    @Test
    fun testCopyValuesFrom() {
        val nonEmptyArray = sampleThreeDimensionalArrayOfIntegerGenes(3, 3, 3)

        nonEmptyArray.getElement(listOf(0, 0, 0)).value = 1
        nonEmptyArray.getElement(listOf(1, 1, 1)).value = 2
        nonEmptyArray.getElement(listOf(2, 2, 2)).value = 0

        val copiedArray = sampleThreeDimensionalArrayOfIntegerGenes(3, 3, 3)

        copiedArray.copyValueFrom(nonEmptyArray)

        assertEquals(1, copiedArray.getElement(listOf(0, 0, 0)).value)
        assertEquals(2, copiedArray.getElement(listOf(1, 1, 1)).value)
        assertEquals(0, copiedArray.getElement(listOf(2, 2, 2)).value)

        assertTrue(copiedArray.isLocallyValid())
    }

    @Test
    fun testCopyValuesFromWithDifferentDimensions() {
        val nonEmptyArray = sampleTwoDimensionalArrayOfIntegerGenes(2, 3)
        val copiedArray = sampleOneDimensionalArrayOfIntegerGenes(2)
        assertThrows<IllegalArgumentException> {
            copiedArray.copyValueFrom(nonEmptyArray)
        }

    }

    @Test
    fun testBindValuesBasedOn() {
        val sourceArray = sampleThreeDimensionalArrayOfIntegerGenes(2, 3, 1)

        val targetArray = sampleThreeDimensionalArrayOfIntegerGenes(1, 5, 1)

        assertEquals(true, targetArray.setValueBasedOn(sourceArray))

        assertEquals(2, targetArray.getDimensionSize(0))
        assertEquals(3, targetArray.getDimensionSize(1))
        assertEquals(1, targetArray.getDimensionSize(2))

        assertTrue(targetArray.isLocallyValid())
    }

    @Test
    fun testFailedBindValuesBasedOn() {
        val sourceArray = sampleThreeDimensionalArrayOfIntegerGenes(2, 3, 1)
        val targetArray = sampleTwoDimensionalArrayOfIntegerGenes(2, 3)
        assertFalse( targetArray.setValueBasedOn(sourceArray))
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
        assertFalse( targetArray.setValueBasedOn(sourceArray))
    }

    @Test
    fun testGetPrintableValueOfArray() {
        val gene = sampleOneDimensionalArrayOfIntegerGenes(3)

        gene.getElement(listOf(0)).value = 1
        gene.getElement(listOf(1)).value = 2
        gene.getElement(listOf(2)).value = 3

        assertEquals("\"{1, 2, 3}\"", gene.getValueAsPrintableString())
        assertTrue(gene.isLocallyValid())
    }

    @Test
    fun testGetPrintableValueOfSquareMatrix() {
        val gene = sampleTwoDimensionalArrayOfIntegerGenes(2, 2)

        gene.getElement(listOf(0, 0)).value = 1
        gene.getElement(listOf(0, 1)).value = 2

        gene.getElement(listOf(1, 0)).value = 3
        gene.getElement(listOf(1, 1)).value = 4

        assertEquals("\"{{1, 2}, {3, 4}}\"", gene.getValueAsPrintableString())
        assertTrue(gene.isLocallyValid())
    }

    @Test
    fun testGetPrintableValueOfNonSquareMatrixAsArray() {
        val gene = sampleTwoDimensionalArrayOfIntegerGenes(rows = 1, columns = 3)
        gene.getElement(listOf(0, 0)).value = 1
        gene.getElement(listOf(0, 1)).value = 2
        gene.getElement(listOf(0, 2)).value = 3
        assertEquals("\"{{1, 2, 3}}\"", gene.getValueAsPrintableString())
        assertTrue(gene.isLocallyValid())
    }

    @Test
    fun testGetPrintableValueOfNonSquareMatrixAsVector() {
        val gene = sampleTwoDimensionalArrayOfIntegerGenes(rows = 3, columns = 1)
        gene.getElement(listOf(0, 0)).value = 1
        gene.getElement(listOf(1, 0)).value = 2
        gene.getElement(listOf(2, 0)).value = 3
        assertEquals("\"{{1}, {2}, {3}}\"", gene.getValueAsPrintableString())
        assertTrue(gene.isLocallyValid())
    }

    @Test
    fun testGetPrintableValueOfEmptyArray() {
        val gene = sampleOneDimensionalArrayOfIntegerGenes(0)
        assertEquals("\"{}\"", gene.getValueAsPrintableString())
        assertTrue(gene.isLocallyValid())
    }

    @Test
    fun testGetPrintableValueOfStringGenesNonEmptyArray() {
        val gene = SqlMultidimensionalArrayGene(
                "matrix",
                template = StringGene("element"),
                numberOfDimensions = 1
        )
        gene.doInitialize(rand)
        do {
            gene.randomize(rand, tryToForceNewValue = false)
        } while (gene.getDimensionSize(0) != 2)

        gene.getElement(listOf(0)).value = "Hello"
        gene.getElement(listOf(1)).value = "World"
        assertEquals("\"{\"Hello\", \"World\"}\"", gene.getValueAsPrintableString())
        assertTrue(gene.isLocallyValid())
    }

    // TODO FixMe. It is not clear how mutation weight should be computed
    @Disabled
    @Test
    fun testMutationWeight() {
        val gene = sampleTwoDimensionalArrayOfIntegerGenes(2, 1)
        (gene.getElement(listOf(0, 0))).value = 1
        (gene.getElement(listOf(1, 0))).value = 2

        val w0 = (gene.getElement(listOf(0, 0))).mutationWeight()
        val w1 = (gene.getElement(listOf(0, 0))).mutationWeight()

        val DELTA = 1e-15
        assertEquals(1.0 + w0 + w1, gene.mutationWeight(), DELTA)

    }

    @Test
    fun testFlatView() {
        val gene = sampleTwoDimensionalArrayOfIntegerGenes(2, 3)
        assertEquals(6, gene.flatView().filterIsInstance<IntegerGene>().size)
    }

    @Test
    fun testContainsSameValueAsDiffDimensions() {
        val gene = sampleTwoDimensionalArrayOfIntegerGenes(2, 3)
        val copy = gene.copy()
        assertTrue(gene.containsSameValueAs(copy))
        assertTrue(copy.isLocallyValid())
    }

    @Test
    fun testContainsSameValueArray() {
        val gene = sampleOneDimensionalArrayOfIntegerGenes(4)
        val copy = gene.copy()
        assertTrue(gene.containsSameValueAs(copy))
        assertTrue(copy.isLocallyValid())
    }

    @Test
    fun testGetPrintableValueOfStringGenesNonEmptyArrayWithH2() {
        val gene = SqlMultidimensionalArrayGene(
                "matrix",
                databaseType = DatabaseType.H2,
                template = StringGene("element"),
                numberOfDimensions = 1
        )
        gene.doInitialize(rand)
        do {
            gene.randomize(rand, tryToForceNewValue = false)
        } while (gene.getDimensionSize(0) != 2)

        gene.getElement(listOf(0)).value = "Hello"
        gene.getElement(listOf(1)).value = "World"
        assertEquals("ARRAY[SINGLE_APOSTROPHE_PLACEHOLDERHelloSINGLE_APOSTROPHE_PLACEHOLDER, SINGLE_APOSTROPHE_PLACEHOLDERWorldSINGLE_APOSTROPHE_PLACEHOLDER]", gene.getValueAsPrintableString())
        assertTrue(gene.isLocallyValid())
    }

    @Test
    fun testGetPrintableValueOfNonSquareMatrixAsVectorH2() {
        val gene = sampleTwoDimensionalArrayOfIntegerGenes(rows = 3, columns = 1, databaseType = DatabaseType.H2)
        gene.getElement(listOf(0, 0)).value = 1
        gene.getElement(listOf(1, 0)).value = 2
        gene.getElement(listOf(2, 0)).value = 3
        assertEquals("ARRAY[ARRAY[1], ARRAY[2], ARRAY[3]]", gene.getValueAsPrintableString())
        assertTrue(gene.isLocallyValid())
    }

}