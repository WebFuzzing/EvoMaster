package org.evomaster.core.search.gene


import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TupleGeneTest {


    @Test
    fun copyValueFromTest() {
        val originalGene = TupleGene(
            "tupleGeneTestOri",
            mutableListOf(
                StringGene("stringGeneTestOri", "hello"),
                ObjectGene("objectGeneTestOri", listOf(IntegerGene("integerGeneTestOri", 3))),
                ArrayGene(
                    "arrayGeneTestOri",
                    ObjectGene("ObjectGeneTestOri", listOf(FloatGene("floatGeneTestOri", 3.43f)))
                )
            )
        )
        val gene = TupleGene(
            "tupleGene",
            mutableListOf(
                StringGene("stringGeneTest", "hello")
            )
        )
        Assertions.assertEquals(gene.elements.size, 1)
        gene.copyValueFrom(originalGene)
        Assertions.assertEquals(gene.elements.size, 3)
        Assertions.assertFalse(gene.elements.any { it is StringGene && it.name == "stringGeneTest" })
        Assertions.assertTrue(gene.elements.any { it is StringGene && it.name == "stringGeneTestOri" })
        Assertions.assertTrue(gene.elements.any { it is ObjectGene && it.name == "objectGeneTestOri" })
        Assertions.assertTrue(gene.elements.any { it is ArrayGene<*> && it.name == "arrayGeneTestOri" })
    }

    @Test
    fun containsSameValueAsTest() {

        val originalGene = TupleGene(
            "tupleGeneTestOri",
            mutableListOf(
                StringGene("stringGeneTestOri", "hello"),
                ObjectGene("objectGeneTestOri", listOf(IntegerGene("integerGeneTestOri", 3))),
                ArrayGene(
                    "arrayGeneTestOri",
                    ObjectGene("ObjectGeneTestOri", listOf(FloatGene("floatGeneTestOri", 3.43f)))
                )
            )
        )

        val gene = TupleGene(
            "tupleGeneTest",
            mutableListOf(
                StringGene("stringGeneTest", "hello"),
                ObjectGene("objectGeneTest", listOf(IntegerGene("integerGeneTest", 3))),
                ArrayGene(
                    "arrayGeneTest",
                    ObjectGene("ObjectGeneTest", listOf(FloatGene("floatGeneTest", 3.43f)))
                )
            )
        )
        Assertions.assertTrue(originalGene.containsSameValueAs(gene))
    }

    @Test
    fun bindValueBasedOn() {
        val originalGene = TupleGene(
            "tupleGeneTestOri",
            mutableListOf(
                StringGene("stringGeneTestOri", "hello"),
                ObjectGene("objectGeneTestOri", listOf(IntegerGene("integerGeneTestOri", 3))),
                ArrayGene(
                    "arrayGeneTestOri",
                    ObjectGene("ObjectGeneTestOri", listOf(FloatGene("floatGeneTestOri", 3.43f)))
                )
            )
        )

        val gene = TupleGene(
            "tupleGeneTest",
            mutableListOf(
                StringGene("stringGeneTest", "hello"),
                ObjectGene("objectGeneTest", listOf(IntegerGene("integerGeneTest", 3))),
                ArrayGene(
                    "arrayGeneTest",
                    ObjectGene("ObjectGeneTest", listOf(FloatGene("floatGeneTest", 3.43f)))
                )
            )
        )

        Assertions.assertTrue(originalGene.bindValueBasedOn(gene))
        Assertions.assertTrue(originalGene.elements.any { it is StringGene && it.name == "stringGeneTest" })
        Assertions.assertTrue(originalGene.elements.any { it is ObjectGene && it.name == "objectGeneTest" })
        Assertions.assertTrue(originalGene.elements.any { it is ArrayGene<*> && it.name == "arrayGeneTest" })
    }
}