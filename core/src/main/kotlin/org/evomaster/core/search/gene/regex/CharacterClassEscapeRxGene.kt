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
import org.slf4j.LoggerFactory

private fun stringToListOfCharPairs(s: String) : List<Pair<Char, Char>> {
    return s.map { it to it }
}

private val digitS = listOf('0' to '9')
private val asciiLetterS = listOf('a' to 'z', 'A' to 'Z')
private val wordS = listOf('_' to '_') + asciiLetterS + digitS
private val spaceS = stringToListOfCharPairs(" \t\r\n\u000C\u000b")
private val horizontalSpaceS = listOf(0x2000.toChar() to 0x200a.toChar()) +
        stringToListOfCharPairs(" \t\u00A0\u1680\u180e\u202f\u205f\u3000")
private val verticalSpaceS = stringToListOfCharPairs("\n\u000B\u000C\r\u0085\u2028\u2029")
private val punctuationS = stringToListOfCharPairs("""!"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~""")

// US-ASCII POSIX character classes (\p{X})
private val posixCharClassCC = mapOf(
    "Lower" to listOf('a' to 'z'),
    "Upper" to listOf('A' to 'Z'),
    "ASCII" to listOf(0.toChar() to 0x7f.toChar()),
    "Alpha" to asciiLetterS,
    "Digit" to digitS,
    "Alnum" to digitS + asciiLetterS,
    "Punct" to punctuationS,
    "Graph" to digitS + asciiLetterS + punctuationS,
    "Print" to digitS + asciiLetterS + punctuationS + stringToListOfCharPairs("\u0020"),
    "Blank" to stringToListOfCharPairs(" \t"),
    "Cntrl" to listOf(0.toChar() to 0x1f.toChar()) + stringToListOfCharPairs("\u007f"),
    "XDigit" to listOf('0' to '9', 'a' to 'f', 'A' to 'F'),
    "Space" to spaceS,
    "Pe" to stringToListOfCharPairs(")]}")
)

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
    }

    var value: String = ""
    var charClassRepr: CharacterRangeRxGene

    init {
        if (!listOf("w", "W", "d", "D", "s", "S", "v", "V", "h", "H").contains(type) && 'p' != type[0]) {
            throw IllegalArgumentException("Invalid type: $type")
        }

        val charSet = when(type[0].lowercaseChar()){
            'd' -> digitS
            'w' -> wordS
            's' -> spaceS
            'v' -> verticalSpaceS
            'h' -> horizontalSpaceS
            'p' -> {
                if (type.substring(2,type.length-1) !in posixCharClassCC){
                    throw IllegalArgumentException("$type invalid/unsupported POSIX character class")
                } else {
                    posixCharClassCC[type.substring(2,type.length-1)]!!
                }
            }
            else ->
                //this should never happen due to check in init
                throw IllegalStateException("Type '\\$type' not supported yet")
        }
        val isNegated = type[0].isUpperCase()

        charClassRepr = CharacterRangeRxGene(isNegated, charSet)
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return value.matches(Regex("\\$type"))
    }

    override fun copyContent(): Gene {
        val copy = CharacterClassEscapeRxGene(type)
        copy.value = this.value
        return copy
    }

    override fun setValueWithRawString(value: String) {
        this.value = value
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        val previous = value

        charClassRepr.randomize(randomness, tryToForceNewValue)

        value = charClassRepr.value.toString()

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
