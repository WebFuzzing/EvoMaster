package org.evomaster.core.search.structuralelement.gene

import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.regex.*
import org.evomaster.core.search.structuralelement.StructuralElementBaseTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class AnyCharacterRxGeneStructureTest : StructuralElementBaseTest() {
    override fun getStructuralElement(): AnyCharacterRxGene = AnyCharacterRxGene()

    override fun getExpectedChildrenSize(): Int  = 0
}


class CharacterClassEscapeRxGeneStructureTest : StructuralElementBaseTest() {
    override fun getStructuralElement(): CharacterClassEscapeRxGene = CharacterClassEscapeRxGene("d")

    override fun getExpectedChildrenSize(): Int  = 0
}

class CharacterRangeRxGeneStructureTest : StructuralElementBaseTest() {
    override fun getStructuralElement(): CharacterRangeRxGene = CharacterRangeRxGene(false, listOf(Pair('a','z')))

    override fun getExpectedChildrenSize(): Int  = 0
}

class RegexGeneStructureTest : StructuralElementBaseTest() {
    override fun getStructuralElement(): RegexGene = RegexHandler.createGeneForEcma262("^fo{2}\\d{2}$")

    override fun getExpectedChildrenSize(): Int  = 1

    @Test
    fun testInitRegexGene(){
        val root = getStructuralElement()
        assertEquals(root, root.getRoot())

        root.disjunctions.apply {
            assertEquals(1, disjunctions.size)
            disjunctions.first().apply {
                assertTrue(terms[0] is PatternCharacterBlock) //f
                assertEquals(0, terms[0].getChildren().size)
                assertTrue(terms[1] is QuantifierRxGene) //o
                assertEquals(3, terms[1].getChildren().size) // template + o, o
                assertTrue(terms[2] is QuantifierRxGene) // \d
                assertEquals(1, terms[2].getChildren().size) // template + no atom
            }
        }

    }

}


