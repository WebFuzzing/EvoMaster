package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.evomaster.core.utils.RegexFlags

/**
 * This class is immutable unless the regular expression has the CASE_INSENSITIVE flag on and at least one caseable
 * character, which may depend on the state of UNICODE_CASE flag as well. If the flag CASE_INSENSITIVE is on and there
 * is at least one caseable character, the class only allows case mutations for the caseable characters present.
 * @see org.evomaster.core.utils.RegexFlags.isCaseable
 */
class PatternCharacterBlockGene(
    name: String,
    val stringBlock: String,
    val flags: RegexFlags = RegexFlags()
) : RxAtom, SimpleGene(name) {

    /**
     * Per-character case choice: true = uppercase, false = lowercase.
     * Only meaningful when flags.caseInsensitive is true.
     */
    var caseChoices: BooleanArray = BooleanArray(stringBlock.length) { false }

    override fun isMutable(): Boolean {
        // check if there are any caseable characters (this also checks the CASE_INSENSITIVE flag value)
        return stringBlock.any { c -> flags.isCaseable(c) }
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene {
        val copy = PatternCharacterBlockGene(name, stringBlock, flags)
        copy.caseChoices = caseChoices.copyOf() //copy the current casings too
        return copy
    }

    override fun setValueWithRawString(value: String) {
        throw IllegalStateException("cannot set value with string ($value) for ${this.javaClass.simpleName}")
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        /* This class is mutable only if the CASE_INSENSITIVE flag is on and at least one caseable character is present.
         If this is the case we can randomize the casings for those characters. */
        if (!isMutable()) {
            throw IllegalStateException("Not supposed to mutate immutable ${this.javaClass.simpleName}")
        }

        val previous = caseChoices.copyOf()

        caseChoices = BooleanArray(stringBlock.length) { i ->
            if (flags.isCaseable(stringBlock[i])) {
                randomness.nextBoolean()
            }
            else {
                false
            }
        }

        if(tryToForceNewValue && caseChoices.contentEquals(previous)){
            randomize(randomness, tryToForceNewValue)
        }
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        /* This class is mutable only if the CASE_INSENSITIVE flag is on and at least one caseable character is present.
         If this is the case we can mutate by randomly flipping the casing for one of those characters. */
        if (!isMutable()) {
            throw IllegalStateException("Not supposed to mutate immutable ${this.javaClass.simpleName}")
        }
        val caseableIndices = stringBlock.indices.filter { i ->
            flags.isCaseable(stringBlock[i])
        }
        val i = randomness.choose(caseableIndices)
        caseChoices[i] = !caseChoices[i]
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
    // We apply the case selected for each character (for the caseable characters)
        if (!isMutable()) {
            return stringBlock
        }
        return stringBlock.mapIndexed { i, c ->
            when {
                !flags.isCaseable(c) -> c
                caseChoices[i] -> c.uppercaseChar()
                else -> c.lowercaseChar()
            }
        }.joinToString("")
    }



    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is PatternCharacterBlockGene) {
            return false
        }
        // we need to consider casings too, therefore we can just compare the strings using getValueAsPrintableString
        return getValueAsPrintableString(targetFormat = null) ==
                other.getValueAsPrintableString(targetFormat = null)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        //do nothing
        return containsSameValueAs(other)
    }

}
