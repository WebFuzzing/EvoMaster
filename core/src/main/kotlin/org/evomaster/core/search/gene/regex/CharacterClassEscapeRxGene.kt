package org.evomaster.core.search.gene.regex

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.evomaster.core.utils.CharacterRange
import org.slf4j.LoggerFactory
import kotlin.collections.contains

/*
\w	Find a word character
\W	Find a non-word character
\d	Find a digit
\D	Find a non-digit character
\s	Find a whitespace character
\S	Find a non-whitespace character
\p{X} Find a character from X POSIX character class (eg:\p{Lower})
 */
class CharacterClassEscapeRxGene(
        val type: String
) : RxAtom, SimpleGene("\\$type") {

    companion object{
        private val log = LoggerFactory.getLogger(CharacterRangeRxGene::class.java)

        private fun stringToListOfCharacterRanges(s: String) : List<CharacterRange> {
            return s.map { CharacterRange(it, it) }
        }

        private val digitSet = listOf(CharacterRange('0', '9'))
        private val asciiLetterSet = listOf(CharacterRange('a', 'z'), CharacterRange('A', 'Z'))
        private val wordSet = listOf(CharacterRange('_', '_')) + asciiLetterSet + digitSet
        private val spaceSet = stringToListOfCharacterRanges(" \t\r\n\u000C\u000b") // u000b, u000c being line
        // tabulation (VT) & form feed (FF, \f) respectively
        private val horizontalSpaceSet = listOf(CharacterRange(0x2000, 0x200a)) +
                stringToListOfCharacterRanges(" \t\u00A0\u1680\u180e\u202f\u205f\u3000")
        private val verticalSpaceSet = stringToListOfCharacterRanges("\n\u000B\u000C\r\u0085\u2028\u2029")
        private val punctuationSet = stringToListOfCharacterRanges("""!"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~""")

        // US-ASCII POSIX character classes (\p{X})
        private val posixSets = mapOf(
            "Lower" to listOf(CharacterRange('a', 'z')),
            "Upper" to listOf(CharacterRange('A', 'Z')),
            "ASCII" to listOf(CharacterRange(0, 0x7f)),
            "Alpha" to asciiLetterSet,
            "Digit" to digitSet,
            "Alnum" to digitSet + asciiLetterSet,
            "Punct" to punctuationSet,
            "Graph" to digitSet + asciiLetterSet + punctuationSet,
            "Print" to digitSet + asciiLetterSet + punctuationSet + stringToListOfCharacterRanges("\u0020"),
            "Blank" to stringToListOfCharacterRanges(" \t"),
            "Cntrl" to listOf(CharacterRange(0, 0x1f)) + stringToListOfCharacterRanges("\u007f"),
            "XDigit" to listOf(CharacterRange('0', '9'), CharacterRange('a', 'f'), CharacterRange('A', 'F')),
            "Space" to spaceSet
        )
    }

    var value: String = ""
    private var charClass: CharacterRangeRxGene

    init {
        if (type[0] !in "wWdDsSvVhHp") {
            throw IllegalArgumentException("Invalid type: $type")
        }

        val charSet = when(type[0]){
            'w', 'W' -> wordSet
            'd', 'D' -> digitSet
            's', 'S' -> spaceSet
            'v', 'V' -> verticalSpaceSet
            'h', 'H' -> horizontalSpaceSet
            'p' ->
                if (type.substring(2, type.length - 1) !in posixSets){
                    throw IllegalArgumentException("$type invalid/unsupported POSIX character class")
                } else {
                    posixSets[type.substring(2, type.length - 1)]!!
                }
            else -> //this should never happen due to check in init
                throw IllegalStateException("Type '\\$type' not supported yet")
        }

        val negated = type[0].isUpperCase()
        charClass = CharacterRangeRxGene(negated, charSet)
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return value.matches(Regex("\\$type"))
    }

    override fun copyContent(): Gene {
        val copy = CharacterClassEscapeRxGene(type)
        copy.value = this.value
        copy.name = this.name //in case name is changed from its default
        return copy
    }

    override fun setValueWithRawString(value: String) {
        this.value = value
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        val previous = value

        charClass.randomize(randomness, tryToForceNewValue)
        value = charClass.value.toString()

        if(tryToForceNewValue && previous == value){
            randomize(randomness, tryToForceNewValue)
        }
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        if (value=="") {
            // if standardMutation was invoked before calling to randomize
            // then we signal an exception
            throw IllegalStateException("Cannot apply mutation on an uninitialized gene")
        }

        if(type == "d"){
            value = ((value.toInt() + randomness.choose(listOf(1,-1)) + 10) % 10).toString()
        } else {
            randomize(randomness, true)
        }

        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
       return value
    }



    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is CharacterClassEscapeRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }


    override fun unsafeCopyValueFrom(other: Gene): Boolean {

        val gene = other.getPhenotype()

        if (gene is CharacterClassEscapeRxGene){
            value = gene.value
            return true
        }
        LoggingUtil.uniqueWarn(log,"cannot bind CharacterClassEscapeRxGene with ${gene::class.java.simpleName}")
        return false
    }

}
