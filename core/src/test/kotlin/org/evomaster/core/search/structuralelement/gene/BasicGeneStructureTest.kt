package org.evomaster.core.search.structuralelement.gene

import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.FixedMapGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.string.Base64StringGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test


// numeric

class IntGeneStructureTest : GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): Gene = IntegerGene("foo", 2)

    override fun assertCopyFrom(base: Gene) {
        assertEquals(2, (base as IntegerGene).value)
    }

    override fun getStructuralElement(): IntegerGene = IntegerGene("foo", 1)

    override fun getExpectedChildrenSize(): Int  = 0

    override fun additionalAssertionsAfterRandomness(base: Gene) {
        assertNotEquals(1, (base as IntegerGene).value)
    }
}

class LongGeneStructureTest : GeneStructuralElementBaseTest() {

    override fun getCopyFromTemplate(): Gene = LongGene("foo", 2L)

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is LongGene)
        assertEquals(2L, (base as LongGene).value)
    }

    override fun getStructuralElement(): LongGene = LongGene("foo", 1L)

    override fun getExpectedChildrenSize(): Int  = 0
}

class DoubleGeneStructureTest : GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): DoubleGene = DoubleGene("foo", 2.0)

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is DoubleGene)
        assertEquals(2.0, (base as DoubleGene).value)
    }

    override fun getStructuralElement(): DoubleGene = DoubleGene("foo", 1.0)

    override fun getExpectedChildrenSize(): Int = 0
}

class FloatGeneStructureTest : GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): Gene = FloatGene("foo", 2.0f)

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is FloatGene)
        assertEquals(2.0f, (base as FloatGene).value)
    }

    override fun getStructuralElement(): FloatGene = FloatGene("foo", 1.0f)

    override fun getExpectedChildrenSize(): Int = 0
}

// enum
class EnumGeneStructureTest: GeneStructuralElementBaseTest(){
    override fun getCopyFromTemplate(): Gene = EnumGene("foo", listOf(1,2,3), 2)

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is EnumGene<*>)
        assertEquals(2, (base as EnumGene<*>).index)
    }

    override fun getStructuralElement(): EnumGene<Int> = EnumGene("foo", listOf(1,2,3), 0)

    override fun getExpectedChildrenSize(): Int = 0
}

// bool gene
class BooleanGeneStructureTest: GeneStructuralElementBaseTest(){
    override fun getCopyFromTemplate(): BooleanGene = BooleanGene("foo", false)

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is BooleanGene)
        assertEquals(false, (base as BooleanGene).value)
    }

    override fun getStructuralElement(): BooleanGene = BooleanGene("foo", true)

    override fun getExpectedChildrenSize(): Int = 0
}

//string

class SimpleStringStructureTest: GeneStructuralElementBaseTest(){
    override fun getCopyFromTemplate(): Gene = StringGene("foo", "bar")

    override fun assertCopyFrom(base: Gene) {
        assertEquals("bar", (base as StringGene).value)
    }

    override fun additionalAssertionsAfterRandomness(base: Gene) {
        assertNotEquals("foo", (base as StringGene).value)
    }

    override fun getStructuralElement(): StringGene = StringGene("foo", "foo")

    override fun getExpectedChildrenSize(): Int = 0
}

class StringWithSpecialization : GeneStructuralElementBaseTest(){
    override fun getCopyFromTemplate(): Gene = StringGene("foo", "bar", specializationGenes = mutableListOf(DateGene("now"), IntegerGene("foo"), TimeGene("now")))

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is StringGene)
        assertEquals("bar", (base as StringGene).value)
        assertEquals(3, base.specializationGenes.size)
        assertChildren(base, 3)
    }


    override fun getStructuralElement(): StringGene = StringGene("foo", "foo", specializationGenes = mutableListOf(
        DateGene("now"), IntegerGene("foo")
    ))

    override fun getExpectedChildrenSize(): Int = 2
}

class Base64StringStructureTest: GeneStructuralElementBaseTest(){
    override fun getCopyFromTemplate(): Base64StringGene = Base64StringGene("foo", StringGene("foo", "bar"))

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is Base64StringGene)
        assertEquals("bar", (base as Base64StringGene).data.value)
        assertEquals(base, base.data.parent)
    }

    override fun getStructuralElement(): Base64StringGene = Base64StringGene("foo", StringGene("foo", "foo"))

    override fun getExpectedChildrenSize(): Int = 1
}

// collection

class ArrayGeneIntStructureTest : GeneStructuralElementBaseTest() {
    private val size = 10
    private val copyFromTemplateSize = 5

    override fun expectedChildrenSizeAfterRandomness(): Int = -1

    override fun getStructuralElement(): ArrayGene<IntegerGene> = ArrayGene(
        "foo",
        template = IntegerGene("foo"),
        maxSize = 20,
        elements = (0 until size).map { IntegerGene("foo", it) }.toMutableList())

    override fun getExpectedChildrenSize(): Int  = size

    override fun getCopyFromTemplate(): Gene = ArrayGene(
        "foo",
        template = IntegerGene("foo"),
        maxSize = 20,
        elements = (0 until copyFromTemplateSize).map { IntegerGene("foo", it) }.toMutableList())

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is ArrayGene<*>)
        assertEquals(copyFromTemplateSize, (base as ArrayGene<*>).getViewOfElements().size)
    }
}

class ArrayGeneObjStructureTest : GeneStructuralElementBaseTest() {
    private val size = 10
    private val copyFromTemplateSize = 5
    private val objTemplate = ObjectGene("f", (0 until 6).map { IntegerGene("f$it", it) })

    override fun expectedChildrenSizeAfterRandomness(): Int = -1

    override fun getStructuralElement(): ArrayGene<ObjectGene> = ArrayGene(
        "foo",
        template = objTemplate.copy() as ObjectGene,
        maxSize = 20,
        elements = (0 until size).map { objTemplate.copy() as ObjectGene }.toMutableList())

    override fun getExpectedChildrenSize(): Int  = size

    override fun getCopyFromTemplate(): Gene = ArrayGene(
        "foo",
        template = objTemplate.copy() as ObjectGene,
        maxSize = 20,
        elements = (0 until copyFromTemplateSize).map {objTemplate.copy() as ObjectGene }.toMutableList())

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is ArrayGene<*>)
        assertEquals(copyFromTemplateSize, (base as ArrayGene<*>).getViewOfElements().size)
        assertChildren(base, 5)
    }
}

class MapGeneIntStructureTest : GeneStructuralElementBaseTest() {
    private val size = 10
    private val copyTemplateSize = 5

    override fun expectedChildrenSizeAfterRandomness(): Int = -1

    override fun getCopyFromTemplate(): Gene = FixedMapGene(
        "foo",
        template = PairGene.createStringPairGene(DoubleGene("foo")),
        maxSize = 20,
        elements = (0 until copyTemplateSize).map { PairGene.createStringPairGene(DoubleGene("foo", it.toDouble())) }.toMutableList())

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is FixedMapGene<*, *>)
        assertEquals(copyTemplateSize, (base as FixedMapGene<*, *>).getAllElements().size)
        assertChildren(base, copyTemplateSize)
    }

    override fun getStructuralElement(): FixedMapGene<StringGene, DoubleGene> = FixedMapGene(
        "foo",
        template = PairGene.createStringPairGene(DoubleGene("foo")),
        maxSize = 20,
        elements = (0 until size).map { PairGene.createStringPairGene(DoubleGene("foo", it.toDouble())) }.toMutableList())

    override fun getExpectedChildrenSize(): Int  = size
}

// date and time

class DateGeneStructureTest: GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): Gene =
        DateGene("2021-06-08", IntegerGene("year", 2021), IntegerGene("month", 6), IntegerGene("day",8))

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is DateGene)
        assertEquals(8, (base as DateGene).day.value)
    }

    override fun getStructuralElement(): DateGene = DateGene("2021-06-3", IntegerGene("year", 2021), IntegerGene("month", 6), IntegerGene("day",3))
    override fun getExpectedChildrenSize(): Int  = 3

    @Test
    fun sameFormat(){
        val template = getStructuralElementAndIdentifyAsRoot() as DateGene
        val copy = template.copy()
        assertEquals(template.format, (copy as DateGene).format)
    }
}

class TimeGeneStructureTest: GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): Gene = TimeGene("23:09:11", IntegerGene("h", 23), IntegerGene("m", 9), IntegerGene("s", 11))

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is TimeGene)
        (base as TimeGene).apply {
            assertEquals(23, hour.value)
            assertEquals(9, minute.value)
            assertEquals(11, second.value)
        }
    }

    override fun getStructuralElement(): TimeGene = TimeGene("22:11:22",
        IntegerGene("h", 22), IntegerGene("m", 11), IntegerGene("s", 22)
    )
    override fun getExpectedChildrenSize(): Int  = 3
}

class DateTimeGeneStructureTest: GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate()= DateTimeGene("2021-06-08 23:13:42",
        date = DateGene("2021-06-08", IntegerGene("year", 2021), IntegerGene("month", 6), IntegerGene("day",8)),
        time = TimeGene("23:13:42", IntegerGene("h", 23), IntegerGene("m", 13), IntegerGene("s", 42))
    )

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is DateTimeGene)
        (base as DateTimeGene).apply {
            assertEquals(8, date.day.value)
            assertEquals(23, time.hour.value)
            assertEquals(13, time.minute.value)
            assertEquals(42, time.second.value)
        }
    }

    override fun getStructuralElement(): DateTimeGene = DateTimeGene("2021-06-3 22:11:22",
            date = DateGene("2021-06-3", IntegerGene("year", 2021), IntegerGene("month", 6), IntegerGene("day",3)),
            time = TimeGene("22:11:22", IntegerGene("h", 22), IntegerGene("m", 11), IntegerGene("s", 22))
    )
    override fun getExpectedChildrenSize(): Int  = 2
}

// object gene

class ObjectGeneStructureTest: GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): Gene = ObjectGene("foo", listOf(
        StringGene("f1", "bar"), IntegerGene("f2", 0), DoubleGene("f3"), ObjectGene("f4", listOf(DateGene("f4_1"), DateTimeGene("f4_2")))
    ))

    override fun assertCopyFrom(base: Gene) {
        val previous = getStructuralElement()
        assertEquals("foo", (previous.fields[0] as StringGene).value)
        assertEquals(42, (previous.fields[1] as IntegerGene).value)

        assertTrue(base is ObjectGene)
        (base as ObjectGene).apply {
            assertEquals("bar", (fields[0] as StringGene).value)
            assertEquals(0, (fields[1] as IntegerGene).value)
        }
    }

    override fun getStructuralElement(): ObjectGene = ObjectGene("foo", listOf(
        StringGene("f1", "foo"), IntegerGene("f2", 42), DoubleGene("f3"), ObjectGene("f4", listOf(DateGene("f4_1"), DateTimeGene("f4_2")))
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

    @Test
    fun testCopyFields(){
        val obj = getStructuralElementAndIdentifyAsRoot() as ObjectGene
        val copy = obj.copy() as ObjectGene
        val copyField = obj.copyFields(copy)

        assertChildren(copyField, -1)

    }
}

class CycleObjectGeneStructureTest: GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): Gene = CycleObjectGene("foo")

    override fun assertCopyFrom(base: Gene) {
        //nothing
    }

    override fun getStructuralElement(): CycleObjectGene = CycleObjectGene("foo")

    override fun getExpectedChildrenSize(): Int  = 0
}

// optional gene
class OptionalGeneStructureTest: GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): Gene = OptionalGene("foo", OptionalGene("foo", IntegerGene("foo", 42)))

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is OptionalGene)
        (base as OptionalGene).apply {
            assertTrue(gene is OptionalGene)
            (gene as OptionalGene).apply {
                assertTrue(gene is IntegerGene)
                assertEquals(42, (gene as IntegerGene).value)
            }
        }
    }

    override fun getStructuralElement(): OptionalGene = OptionalGene("foo", OptionalGene("foo", IntegerGene("foo", 0)))

    override fun getExpectedChildrenSize(): Int  = 1
}

// disruptive gene
class DisruptiveGeneStructureTest: GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): Gene = CustomMutationRateGene("foo", OptionalGene("foo", IntegerGene("foo", 42)), 0.5)

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is CustomMutationRateGene<*>)
        (base as CustomMutationRateGene<*>).apply {
            assertTrue(gene is OptionalGene)
            assertEquals(0.5, probability)
            (gene as OptionalGene).apply {
                assertTrue(gene is IntegerGene)
                assertEquals(42, (gene as IntegerGene).value)
            }
        }
    }

    override fun getStructuralElement(): CustomMutationRateGene<OptionalGene> = CustomMutationRateGene("foo", OptionalGene("foo", IntegerGene("foo", 0)), 1.0)

    override fun getExpectedChildrenSize(): Int  = 1
}

//ImmutableDataHolderGene
class ImmutableDataHolderGeneStructureTest: GeneStructuralElementBaseTest() {
    override fun throwExceptionInCopyFromTest(): Boolean = true
    override fun throwExceptionInRandomnessTest(): Boolean = true

    override fun getCopyFromTemplate(): Gene = ImmutableDataHolderGene("foo", "foo", false)

    override fun assertCopyFrom(base: Gene) {
    }

    override fun getStructuralElement(): ImmutableDataHolderGene = ImmutableDataHolderGene("foo", "foo", false)

    override fun getExpectedChildrenSize(): Int  = 0
}
