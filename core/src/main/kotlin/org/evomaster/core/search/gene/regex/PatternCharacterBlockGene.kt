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
        return stringBlock.any { c -> flags.isCaseable(c) }
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene {
        val copy = PatternCharacterBlockGene(name, stringBlock, flags)
        copy.caseChoices = caseChoices.copyOf()
        return copy
    }

    override fun setValueWithRawString(value: String) {
        throw IllegalStateException("cannot set value with string ($value) for ${this.javaClass.simpleName}")
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

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
        return getValueAsPrintableString(targetFormat = null) ==
                other.getValueAsPrintableString(targetFormat = null)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        //do nothing
        return containsSameValueAs(other)
    }

}
