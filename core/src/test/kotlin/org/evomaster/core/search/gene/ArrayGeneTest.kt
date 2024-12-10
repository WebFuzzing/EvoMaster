package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ArrayGeneTest {

    @Test
    fun testGene() {
        val gene = ArrayGene("array", template=BooleanGene("boolean"))
        assertEquals(0, gene.getViewOfChildren().size)
    }

    @Test
    fun testGeneMinSize() {
        val gene = ArrayGene("array", minSize = 1, maxSize =10, template=BooleanGene("boolean"))
        assertEquals(0, gene.getViewOfChildren().size)
        val randomness = Randomness()
        gene.randomize(randomness, tryToForceNewValue = true)
        assertTrue(gene.getViewOfChildren().isNotEmpty())
    }

    @Test
    fun testUnique(){
        val boolArray = ArrayGene(
                "array",
                minSize = 1,
                maxSize =10,
                template=BooleanGene("boolean"),
                uniqueElements = true,
                elements = mutableListOf(BooleanGene("true_element", true), BooleanGene("false_element", false))
        )

        assertTrue(boolArray.doesExist(BooleanGene("new_true_element", true)))
        assertTrue(boolArray.doesExist(BooleanGene("new_false_element", false)))
        assertThrows<IllegalArgumentException> {  boolArray.addElement(BooleanGene("new_true_element", true))}
        assertThrows<IllegalArgumentException> {  boolArray.addElement(BooleanGene("new_false_element", false))}

        val intArray = ArrayGene(
                "array",
                minSize = 1,
                maxSize =10,
                template=IntegerGene("int"),
                uniqueElements = true,
                elements = mutableListOf(IntegerGene("0_element", 0), IntegerGene("42_element", 42))
        )

        assertTrue(intArray.doesExist(IntegerGene("0_element", 0)))
        assertTrue(intArray.doesExist(IntegerGene("42_element", 42)))
        assertThrows<IllegalArgumentException> {  intArray.addElement(IntegerGene("0_element", 0))}
        assertThrows<IllegalArgumentException> {  intArray.addElement(IntegerGene("42_element", 42))}

        val longArray = ArrayGene(
                "array",
                minSize = 1,
                maxSize = 10,
                template= LongGene("long"),
                uniqueElements = true,
                elements = mutableListOf(LongGene("0_element", 0L), LongGene("42_element", 42L))
        )

        assertTrue(longArray.doesExist(LongGene("0_element", 0)))
        assertTrue(longArray.doesExist(LongGene("42_element", 42)))
        assertThrows<IllegalArgumentException> {  longArray.addElement(LongGene("0_element", 0))}
        assertThrows<IllegalArgumentException> {  longArray.addElement(LongGene("42_element", 42))}

        val stringArray = ArrayGene(
                "array",
                minSize = 1,
                maxSize =10,
                template= StringGene("string"),
                uniqueElements = true,
                elements = mutableListOf(StringGene("foo_element", "foo"), StringGene("bar_element", "bar"))
        )

        assertTrue(stringArray.doesExist(StringGene("foo_element", "foo")))
        assertTrue(stringArray.doesExist(StringGene("bar_element", "bar")))
        assertThrows<IllegalArgumentException> {  stringArray.addElement(StringGene("foo_element", "foo"))}
        assertThrows<IllegalArgumentException> {  stringArray.addElement(StringGene("bar_element", "bar"))}

        val enumValues = listOf("ONE", "TWO", "THREE")
        val enumTemplate = EnumGene("enum", enumValues)
        val enumArray = ArrayGene(
                "array",
                minSize = 1,
                maxSize =10,
                template= enumTemplate,
                uniqueElements = true,
                elements = mutableListOf(
                        (enumTemplate.copy() as EnumGene<String>).apply { index =0 }, (enumTemplate.copy() as EnumGene<String>).apply { index =1 })
        )

        assertTrue(enumArray.doesExist((enumTemplate.copy() as EnumGene<String>).apply { index =0 }))
        assertTrue(enumArray.doesExist((enumTemplate.copy() as EnumGene<String>).apply { index =1 }))
        assertThrows<IllegalArgumentException> {  enumArray.addElement((enumTemplate.copy() as EnumGene<String>).apply { index =0 })}
        assertThrows<IllegalArgumentException> {  enumArray.addElement((enumTemplate.copy() as EnumGene<String>).apply { index =1 })}
    }

}