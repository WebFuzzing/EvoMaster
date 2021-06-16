package org.evomaster.core.search.structuralelement.gene

import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class AnyCharacterRxGeneStructureTest : GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): Gene  = AnyCharacterRxGene().apply { value = 'b' }

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is AnyCharacterRxGene)
        assertEquals('b', (base as AnyCharacterRxGene).value)
    }

    override fun getStructuralElement(): AnyCharacterRxGene = AnyCharacterRxGene()

    override fun getExpectedChildrenSize(): Int  = 0
}


class CharacterClassEscapeRxGeneStructureTest : GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): Gene = CharacterClassEscapeRxGene("s").apply { value="bar" }

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is CharacterClassEscapeRxGene)
        // TODO shall we check the type in copyFrom
        assertEquals("d", (base as CharacterClassEscapeRxGene).type)
        assertEquals("bar", base.value)
    }

    override fun getStructuralElement(): CharacterClassEscapeRxGene = CharacterClassEscapeRxGene("d").apply { value="foo" }

    override fun getExpectedChildrenSize(): Int  = 0
}

class CharacterRangeRxGeneStructureTest : GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): Gene = CharacterRangeRxGene(false, listOf(Pair('0','9'))).apply { value='2'}

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is CharacterRangeRxGene)
        //TODO shall we check the range?
        assertEquals('2', (base as CharacterRangeRxGene).value)
    }

    override fun getStructuralElement(): CharacterRangeRxGene = CharacterRangeRxGene(false, listOf(Pair('a','z'))).apply { value= 'w' }

    override fun getExpectedChildrenSize(): Int  = 0
}

class RegexGeneStructureTest : GeneStructuralElementBaseTest() {

    override fun getCopyFromTemplate(): Gene = RegexHandler.createGeneForEcma262("^ba{2}\\d{2}$")

    // due to PatternCharacterBlock
    override fun throwExceptionInCopyFromTest(): Boolean = true

    override fun assertCopyFrom(base: Gene) {
    }

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

