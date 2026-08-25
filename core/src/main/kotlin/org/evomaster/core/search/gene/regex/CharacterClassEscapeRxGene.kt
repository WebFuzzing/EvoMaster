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
import org.evomaster.core.utils.MultiCharacterRange
import org.evomaster.core.utils.RegexFlags
import org.evomaster.core.utils.UnicodeCache
import org.slf4j.LoggerFactory

private const val FIRST_ASCII_CHAR = 0
private const val LAST_ASCII_CHAR = 0x7f
private const val FIRST_LOWER_CASE_CHAR = 'a'
private const val LAST_LOWER_CASE_CHAR = 'z'
private const val FIRST_UPPER_CASE_CHAR = 'A'
private const val LAST_UPPER_CASE_CHAR = 'Z'
private const val LAST_LOWER_CASE_HEX_CHAR = 'f'
private const val LAST_UPPER_CASE_HEX_CHAR = 'F'
private const val SPACE_CHAR = ' '
private const val DELETE_CHAR = '\u007f'

private const val WORD = 'w'
private const val NON_WORD = 'W'
private const val DIGIT = 'd'
private const val NON_DIGIT = 'D'
private const val SPACE = 's'
private const val NON_SPACE = 'S'
private const val VERTICAL_SPACE = 'v'
private const val NON_VERTICAL_SPACE = 'V'
private const val HORIZONTAL_SPACE = 'h'
private const val NON_HORIZONTAL_SPACE = 'H'
private const val POSIX_LOWER_PREFIX = 'p'
private const val POSIX_UPPER_PREFIX = 'P'

private val unicodeCharClassModePredefinedEscapes = setOf(WORD, NON_WORD, DIGIT, NON_DIGIT, SPACE, NON_SPACE)
private val posixEscapePrefix = setOf(POSIX_LOWER_PREFIX, POSIX_UPPER_PREFIX)
private val validClassEscapeChars = setOf(
    WORD,
    NON_WORD,
    DIGIT,
    NON_DIGIT,
    SPACE,
    NON_SPACE,
    VERTICAL_SPACE,
    NON_VERTICAL_SPACE,
    HORIZONTAL_SPACE,
    NON_HORIZONTAL_SPACE,
    POSIX_LOWER_PREFIX,
    POSIX_UPPER_PREFIX
)

/**
 * @property pLabel the label used inside the `\p{...}` / `\P{...}` POSIX character class regex
 * syntax (e.g. "Lower" for `\p{Lower}`).
 */
enum class PosixClass(val pLabel: String) {
    LOWER("Lower"),
    UPPER("Upper"),
    ASCII("ASCII"),
    ALPHA("Alpha"),
    DIGIT("Digit"),
    ALNUM("Alnum"),
    PUNCT("Punct"),
    GRAPH("Graph"),
    PRINT("Print"),
    BLANK("Blank"),
    CNTRL("Cntrl"),
    XDIGIT("XDigit"),
    SPACE("Space");

    companion object {
        private val exact = entries.associateBy { it.pLabel }
        private val ignoreCase = entries.associateBy { it.pLabel.lowercase() }

        fun fromPLabel(pLabel: String): PosixClass? =
            exact[pLabel]

        fun fromPLabelIgnoreCase(pLabel: String): PosixClass? =
            ignoreCase[pLabel.lowercase()]
    }
}


private data class PosixMultiCharacterRanges(
    val normal: MultiCharacterRange,
    val negated: MultiCharacterRange
)

/*
\w	Find a word character
\W	Find a non-word character
\d	Find a digit
\D	Find a non-digit character
\s	Find a whitespace character
\S	Find a non-whitespace character
\h	Find a horizontal space character
\H	Find a non-horizontal space character
\v	Find a vertical space character
\V	Find a non-vertical space character
\p{X} Find a character from X POSIX character class (eg:\p{Lower})
 */
class CharacterClassEscapeRxGene(
    val type: String,
    val flags: RegexFlags = RegexFlags()
) : RxAtom, SimpleGene("\\$type") {

    companion object{
        private val log = LoggerFactory.getLogger(CharacterRangeRxGene::class.java)

        private fun Char.swapCase(): Char =
            if (this.isUpperCase()) {
                this.lowercaseChar()
            } else {
                this.uppercaseChar()
            }

        private val digitSet = listOf(CharacterRange('0', '9'))
        private val asciiLetterSet = listOf(CharacterRange(FIRST_LOWER_CASE_CHAR, LAST_LOWER_CASE_CHAR),
            CharacterRange(FIRST_UPPER_CASE_CHAR, LAST_UPPER_CASE_CHAR))
        private val wordSet = listOf(CharacterRange('_', '_')) + asciiLetterSet + digitSet
        private val spaceSet = listOf(SPACE_CHAR, '\t', '\r', '\n', '\u000b', '\u000c').map { CharacterRange(it) }
        private val horizontalSpaceSet = listOf(CharacterRange(0x2000, 0x200a)) +
                listOf(
                    SPACE_CHAR, '\t',
                    '\u00A0', '\u1680', '\u180e',
                    '\u202f', '\u205f', '\u3000'
                    ).map{ CharacterRange(it) }
        private val verticalSpaceSet = listOf(
            '\n', '\u000B', '\u000C',
            '\r', '\u0085', '\u2028', '\u2029'
        ).map{ CharacterRange(it) }
        private val punctuationSet = listOf(
            '!', '"', '#', '$', '%', '&', '\'', '(',
            ')', '*', '+', ',', '-', '.', '/', ':',
            ';', '<', '=', '>', '?', '@', '[', '\\',
            ']', '^', '_', '`', '{', '|', '}', '~'
        ).map{ CharacterRange(it) }

        private val digitMultiCharRange = MultiCharacterRange(false, digitSet)
        private val wordMultiCharRange = MultiCharacterRange(false, wordSet)
        private val spaceMultiCharRange = MultiCharacterRange(false, spaceSet)
        private val horizontalSpaceMultiCharRange = MultiCharacterRange(false, horizontalSpaceSet)
        private val verticalSpaceMultiCharRange = MultiCharacterRange(false, verticalSpaceSet)

        private val nonDigitMultiCharRange = MultiCharacterRange(true, digitSet)
        private val nonWordMultiCharRange = MultiCharacterRange(true, wordSet)
        private val nonSpaceMultiCharRange = MultiCharacterRange(true, spaceSet)
        private val nonHorizontalSpaceMultiCharRange = MultiCharacterRange(true, horizontalSpaceSet)
        private val nonVerticalSpaceMultiCharRange = MultiCharacterRange(true, verticalSpaceSet)

        private val posixAsciiMultiCharRange: Map<PosixClass, PosixMultiCharacterRanges> =
            mapOf(
                PosixClass.LOWER  to listOf(CharacterRange(FIRST_LOWER_CASE_CHAR, LAST_LOWER_CASE_CHAR)),
                PosixClass.UPPER  to listOf(CharacterRange(FIRST_UPPER_CASE_CHAR, LAST_UPPER_CASE_CHAR)),
                PosixClass.ASCII  to listOf(CharacterRange(FIRST_ASCII_CHAR, LAST_ASCII_CHAR)),
                PosixClass.ALPHA  to asciiLetterSet,
                PosixClass.DIGIT  to digitSet,
                PosixClass.ALNUM  to digitSet + asciiLetterSet,
                PosixClass.PUNCT  to punctuationSet,
                PosixClass.GRAPH  to digitSet + asciiLetterSet + punctuationSet,
                PosixClass.PRINT  to digitSet + asciiLetterSet + punctuationSet + CharacterRange(SPACE_CHAR),
                PosixClass.BLANK  to listOf(SPACE_CHAR, '\t').map{ CharacterRange(it) },
                PosixClass.CNTRL  to listOf(CharacterRange(0, 0x1f)) + CharacterRange(DELETE_CHAR),
                PosixClass.XDIGIT to digitSet + listOf(
                    CharacterRange(FIRST_LOWER_CASE_CHAR, LAST_LOWER_CASE_HEX_CHAR),
                    CharacterRange(FIRST_UPPER_CASE_CHAR, LAST_UPPER_CASE_HEX_CHAR)
                ),
                PosixClass.SPACE  to spaceSet,
            ).mapValues { (_, ranges) ->
                PosixMultiCharacterRanges(
                    normal = MultiCharacterRange(false, ranges),
                    negated = MultiCharacterRange(true, ranges)
                )
            }

        private val unicodeCache = UnicodeCache()
    }

    var value: String = ""
    var multiCharRange: MultiCharacterRange

    /**
     * Whether to output the sampled character in uppercase.
     * Only meaningful when flags.caseInsensitive is true.
     */
    var useUpperCase: Boolean = false

    init {
        if (type[0] !in validClassEscapeChars) {
            throw IllegalArgumentException("Invalid type: $type")
        }

        val pLabel = if (type[0] in posixEscapePrefix) {
            type.substring(2, type.length - 1)
        } else {
            null
        }
        val negated = type[0].isUpperCase()

        multiCharRange = if (
            flags.unicodeCharacterClass &&
            (
                type[0] in unicodeCharClassModePredefinedEscapes ||
                    (pLabel != null && PosixClass.fromPLabelIgnoreCase(pLabel) != null)
            )
        ) {
            // UNICODE_CHARACTER_CLASSES flag is on, so these should now be in conformance with the recommendation of
            // Annex C: Compatibility Properties of Unicode Regular Expression, see:
            // https://www.unicode.org/reports/tr18/#Compatibility_Properties
            val cacheLabel = when (type[0]) {
                DIGIT, NON_DIGIT, SPACE, NON_SPACE, WORD, NON_WORD -> type.lowercase()
                POSIX_LOWER_PREFIX, POSIX_UPPER_PREFIX -> pLabel!!
                else -> //this should never happen due to check in init
                    throw IllegalStateException("Type '\\$type' not supported yet")
            }
            unicodeCache.getRanges(cacheLabel, negated)
        } else {
            // regular predefined character classes
            when(type[0]){
                WORD -> wordMultiCharRange
                NON_WORD -> nonWordMultiCharRange
                DIGIT -> digitMultiCharRange
                NON_DIGIT -> nonDigitMultiCharRange
                SPACE -> spaceMultiCharRange
                NON_SPACE -> nonSpaceMultiCharRange
                VERTICAL_SPACE -> verticalSpaceMultiCharRange
                NON_VERTICAL_SPACE -> nonVerticalSpaceMultiCharRange
                HORIZONTAL_SPACE -> horizontalSpaceMultiCharRange
                NON_HORIZONTAL_SPACE -> nonHorizontalSpaceMultiCharRange
                POSIX_LOWER_PREFIX, POSIX_UPPER_PREFIX -> {
                    val posixClass = PosixClass.fromPLabel(pLabel!!)
                    if (posixClass != null) {
                        val ranges = posixAsciiMultiCharRange[posixClass]!!
                        if (negated) ranges.negated else ranges.normal
                    } else if (PosixClass.fromPLabelIgnoreCase(pLabel) != null) {
                        throw IllegalStateException("This escape (\\$type) is only valid when the \"U\" flag is on.")
                    } else {
                        unicodeCache.getRanges(pLabel, negated)
                    }
                }
                else -> //this should never happen due to check in init
                    throw IllegalStateException("Type '\\$type' not supported yet")
            }
        }
    }

    override fun isUnsatisfiable(): Boolean = multiCharRange.isEmpty

    override fun isMutable(): Boolean {
        return !isUnsatisfiable()
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        // we pass the same embedded flags to the regex to accurately match the expected behavior
        return value.matches(Regex("${flags.getScopeString()}\\$type"))
    }

    override fun copyContent(): Gene {
        val copy = CharacterClassEscapeRxGene(type, flags)
        copy.value = this.value
        copy.name = this.name //in case name is changed from its default
        copy.useUpperCase = this.useUpperCase //copy the current casing
        return copy
    }

    override fun setValueWithRawString(value: String) {
        this.value = value
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        /*
        Besides randomizing by sampling from the MultiCharacterRange, we can also randomize the casings for the sampled
        characters (if applicable), this is needed to allow things like "\p{Lower}" sampling things like "A" when the
        CASE_INSENSITIVE flag is on, etc.
         */
        val previous = value
        val previousUpper = useUpperCase

        value = multiCharRange.sample(randomness).toString()
        useUpperCase = if (flags.isCaseable(value[0])) {
            randomness.nextBoolean()
        } else {
            false
        }

        if(tryToForceNewValue && previous == value && previousUpper == useUpperCase){
            randomize(randomness, tryToForceNewValue)
        }
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        if (value=="") {
            // if standardMutation was invoked before calling to randomize
            // then we signal an exception
            throw IllegalStateException("Cannot apply mutation on an uninitialized gene")
        }

        // flip case if applicable
        if (flags.isCaseable(value[0]) && randomness.nextBoolean()) {
            useUpperCase = !useUpperCase
            return true
        }

        if(type == "d"){
            value = ((value.toInt() + randomness.choose(listOf(1,-1)) + 10) % 10).toString()
        } else {
            randomize(randomness, true)
        }

        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        if (isUnsatisfiable()) {
            throw IllegalStateException("Cannot get value from empty CharacterClassEscape")
        }
        return if (!flags.isCaseable(value[0])) {
            value[0].toString()
        }
        // We apply the case selected for each character (for the caseable characters)
        else if (useUpperCase) {
            value[0].uppercaseChar().toString()
        }
        else {
            value[0].lowercaseChar().toString()
        }
    }



    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is CharacterClassEscapeRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        // we need to consider casings too, therefore we can just compare the strings using getValueAsPrintableString
        return getValueAsPrintableString(targetFormat = null) ==
                other.getValueAsPrintableString(targetFormat = null)
    }


    override fun unsafeCopyValueFrom(other: Gene): Boolean {

        val gene = other.getPhenotype()

        if (gene is CharacterClassEscapeRxGene){
            value = gene.value
            useUpperCase = gene.useUpperCase //copy current casing
            return true
        }
        LoggingUtil.uniqueWarn(log,"cannot bind CharacterClassEscapeRxGene with ${gene::class.java.simpleName}")
        return false
    }

    /**
     * 1 if [value]'s first character (or its case-swapped counterpart, when caseable) is
     * within [multiCharRange], else 0.
     * @see [RxAbsorbable.absorbableCount]
     */
    override fun absorbableCount(value: String): Int {
        if (value.isEmpty()) {
            return 0
        }
        val c = value[0]
        if (multiCharRange.contains(c)) {
            return 1
        }
        if (flags.isCaseable(c) && multiCharRange.contains(c.swapCase())) {
            return 1
        }
        return 0
    }

    /**
     * Always false: a character-class escape always renders exactly one character.
     * @see [RxAbsorbable.canBeZeroWidth]
     */
    override val canBeZeroWidth: Boolean = false

    /**
     * Forces [value]'s first character onto this gene, swapping case if that's what
     * matched; mirrors [absorbableCount].
     * @see [RxAbsorbable.tryForce]
     */
    override fun tryForce(value: String): Int {
        require(value.isNotEmpty())
        val n = absorbableCount(value)
        require(n == 0 || n == 1)
        if (n == 1) {
            val c = value[0]
            this.value = (if (multiCharRange.contains(c)) {
                    c
                } else {
                    c.swapCase()
                }).toString()
            this.useUpperCase = c.isUpperCase()
        }
        return n
    }
}
