package org.evomaster.core.search.structuralelement.gene

import org.evomaster.core.search.gene.*
import org.evomaster.core.search.structuralelement.StructuralElementBaseTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


// numeric

class IntGeneStructureTest : StructuralElementBaseTest() {
    override fun getStructuralElement(): IntegerGene = IntegerGene("foo", 1)

    override fun getExpectedChildrenSize(): Int  = 0
}

class LongGeneStructureTest : StructuralElementBaseTest() {
    override fun getStructuralElement(): LongGene = LongGene("foo", 1L)

    override fun getExpectedChildrenSize(): Int  = 0
}

class DoubleGeneStructureTest : StructuralElementBaseTest() {
    override fun getStructuralElement(): DoubleGene = DoubleGene("foo", 1.0)

    override fun getExpectedChildrenSize(): Int = 0
}

class FloatGeneStructureTest : StructuralElementBaseTest() {
    override fun getStructuralElement(): FloatGene = FloatGene("foo", 1.0f)

    override fun getExpectedChildrenSize(): Int = 0
}

// enum
class EnumGeneStructureTest: StructuralElementBaseTest(){
    override fun getStructuralElement(): EnumGene<Int> = EnumGene("foo", listOf(1,2,3), 0)

    override fun getExpectedChildrenSize(): Int = 0
}

// bool gene
class BooleanGeneStructureTest: StructuralElementBaseTest(){
    override fun getStructuralElement(): BooleanGene = BooleanGene("foo", true)

    override fun getExpectedChildrenSize(): Int = 0
}

//string

class SimpleStringStructureTest: StructuralElementBaseTest(){
    override fun getStructuralElement(): StringGene = StringGene("foo", "foo")

    override fun getExpectedChildrenSize(): Int = 0
}

class StringWithSpecialization : StructuralElementBaseTest(){
    override fun getStructuralElement(): StringGene = StringGene("foo", "foo", specializationGenes = mutableListOf(DateGene("now"), IntegerGene("foo")))

    override fun getExpectedChildrenSize(): Int = 2
}

class Base64StringStructureTest: StructuralElementBaseTest(){
    override fun getStructuralElement(): Base64StringGene = Base64StringGene("foo", StringGene("foo", "foo"))

    override fun getExpectedChildrenSize(): Int = 1
}

// collection

class ArrayGeneIntStructureTest : StructuralElementBaseTest() {
    private val size = 10

    override fun getStructuralElement(): ArrayGene<IntegerGene> = ArrayGene(
        "foo",
        template = IntegerGene("foo"),
        maxSize = 20,
        elements = (0 until size).map { IntegerGene("foo", it) }.toMutableList())

    override fun getExpectedChildrenSize(): Int  = size
}

class MapGeneIntStructureTest : StructuralElementBaseTest() {
    private val size = 10

    override fun getStructuralElement(): MapGene<DoubleGene> = MapGene(
        "foo",
        template = DoubleGene("foo"),
        maxSize = 20,
        elements = (0 until size).map { DoubleGene("foo", it.toDouble()) }.toMutableList())

    override fun getExpectedChildrenSize(): Int  = size
}

// date and time

class DateGeneStructureTest: StructuralElementBaseTest() {

    override fun getStructuralElement(): DateGene = DateGene("2021-06-3", IntegerGene("year", 2021), IntegerGene("month", 6), IntegerGene("day",3))
    override fun getExpectedChildrenSize(): Int  = 3

    @Test
    fun sameFormat(){
        val template = getStructuralElement()
        val copy = template.copy()
        assertEquals(template.dateGeneFormat, (copy as DateGene).dateGeneFormat)
    }
}

class TimeGeneStructureTest: StructuralElementBaseTest() {

    override fun getStructuralElement(): TimeGene = TimeGene("now")
    override fun getExpectedChildrenSize(): Int  = 3
}

class DateTimeGeneStructureTest: StructuralElementBaseTest() {

    override fun getStructuralElement(): DateTimeGene = DateTimeGene("now")
    override fun getExpectedChildrenSize(): Int  = 2
}

// object gene

class ObjectGeneStructureTest: StructuralElementBaseTest() {

    override fun getStructuralElement(): ObjectGene = ObjectGene("foo", listOf(
        StringGene("f1"), IntegerGene("f2"), DoubleGene("f3"), ObjectGene("f4", listOf(DateGene("f4_1"), DateTimeGene("f4_2")))
    ))

    override fun getExpectedChildrenSize(): Int  = 4

    @Test
    fun testTraverseBackIndex(){
        val root = getStructuralElement()
        assertEquals(root, root.getRoot())

        val f4_2 = (root.fields[3] as ObjectGene).fields[1]
        assertEquals("f4_2", f4_2.name)

        val path = mutableListOf<Int>()
        f4_2.traverseBackIndex(path)
        assertEquals(mutableListOf(3,1), path)
    }
}

class CycleObjectGeneStructureTest: StructuralElementBaseTest() {

    override fun getStructuralElement(): CycleObjectGene = CycleObjectGene("foo")

    override fun getExpectedChildrenSize(): Int  = 0
}

// optional gene
class OptionalGeneStructureTest: StructuralElementBaseTest() {

    override fun getStructuralElement(): OptionalGene = OptionalGene("foo", OptionalGene("foo", IntegerGene("foo")))

    override fun getExpectedChildrenSize(): Int  = 1
}

// disruptive gene
class DisruptiveGeneStructureTest: StructuralElementBaseTest() {

    override fun getStructuralElement(): DisruptiveGene<OptionalGene> = DisruptiveGene("foo", OptionalGene("foo", IntegerGene("foo")), 1.0)

    override fun getExpectedChildrenSize(): Int  = 1
}

//ImmutableDataHolderGene
class ImmutableDataHolderGeneStructureTest: StructuralElementBaseTest() {

    override fun getStructuralElement(): ImmutableDataHolderGene = ImmutableDataHolderGene("foo", "foo", false)

    override fun getExpectedChildrenSize(): Int  = 0
}
