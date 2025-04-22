package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.TupleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TupleGeneTest {


    @Test
    fun copyFromAndcontainsSameValueAsTest() {

        val originalGene = TupleGene(
            "tupleGeneTestOri",
            mutableListOf(
                StringGene("stringGene1", "hello"),
                ObjectGene("objectGene2", listOf(IntegerGene("f1", 3))),
                ArrayGene(
                    "arrayGene3",
                    ObjectGene("ObjectGene1", listOf(FloatGene("f1", 3.43f)))
                )
            )
        )

        val gene = TupleGene(
            "tupleGeneTest",
            mutableListOf(
                StringGene("stringGene1", "foo"),
                ObjectGene("objectGene2", listOf(IntegerGene("f1", 42))),
                ArrayGene(
                    "arrayGene3",
                    ObjectGene("ObjectGene1", listOf(FloatGene("f1", 4.2f)))
                ).apply {
                    addElement(ObjectGene("ObjectGene1", listOf(FloatGene("f1", 4.2f))))
                }
            )
        )

        assertFalse(originalGene.containsSameValueAs(gene))

        originalGene.copyValueFrom(gene)
        assertEquals(1, (originalGene.elements[2] as ArrayGene<*>).getViewOfElements().size)
        assertTrue(originalGene.containsSameValueAs(gene))
    }

    @Test
    fun bindValueBasedOn() {
        val originalGene = TupleGene(
            "tupleGeneTestOri",
            mutableListOf(
                StringGene("stringGene1", "hello"),
                ObjectGene("objectGene2", listOf(IntegerGene("f1", 3))),
                ArrayGene(
                    "arrayGene3",
                    ObjectGene("ObjectGene1", listOf(FloatGene("f1", 3.43f)))
                )
            )
        )

        val gene = TupleGene(
            "tupleGeneTest",
            mutableListOf(
                StringGene("stringGene1", "foo"),
                ObjectGene("objectGene2", listOf(IntegerGene("f1", 42))),
                ArrayGene(
                    "arrayGene3",
                    ObjectGene("ObjectGene1", listOf(FloatGene("f1", 4.2f)))
                ).apply {
                    addElement(ObjectGene("ObjectGene1", listOf(FloatGene("f1", 4.2f))))
                }
            )
        )

        assertTrue(originalGene.setValueBasedOn(gene))

        originalGene.elements.apply {
            assertTrue(this[0] is StringGene)
            assertEquals("foo", (this[0] as StringGene).value)
            assertTrue(this[1] is ObjectGene)
            assertTrue((this[1] as ObjectGene).fields[0] is IntegerGene)
            assertEquals(42, ((this[1] as ObjectGene).fields[0] as IntegerGene).value)
            assertTrue(this[2] is ArrayGene<*>)
            assertEquals(1, (this[2] as ArrayGene<*>).getViewOfElements().size)
        }
    }

    @Test
    fun lastElement(){



    }
}