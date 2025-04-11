package org.evomaster.core.search.gene.string

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.interfaces.ComparableGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.NumericStringGeneImpact
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import java.math.BigDecimal

/**
 * A string to represent a number in all aspects,
 * but in its phenotype it is going to be outputted as a string
 */
class NumericStringGene(
    name: String,
    /**
     * inclusive
     *
     * note that precision would represent maxLength
     *
     * TODO
     * update min and max if the minLength is not 0
     */
    val minLength: Int,
    val number : BigDecimalGene
) : ComparableGene, CompositeFixedGene(name, number) {

    constructor(name: String,
                minLength: Int = 0,
                value: BigDecimal? = null,
                min : BigDecimal? = null,
                max : BigDecimal? = null,
                minInclusive : Boolean = true,
                maxInclusive : Boolean = true,
                floatingPointMode : Boolean = true,
                precision : Int? = null,
                scale : Int? = null) : this(name, minLength,
        BigDecimalGene(
            name,
            value,
            min,
            max,
            minInclusive,
            maxInclusive,
            floatingPointMode,
            precision ?: if (scale == 0) 20 else 15,
            scale
        )
    )


    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        //TODO minLength does not seem to be used...
        return true
    }

    override fun copyContent(): Gene {
        return NumericStringGene(name, minLength, number.copy() as BigDecimalGene)
    }

    override fun isMutable(): Boolean {
        return number.isMutable()
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is NumericStringGene)
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
       return updateValueOnlyIfValid({this.number.copyValueFrom(other.number)}, false)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is NumericStringGene)
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        return this.number.containsSameValueAs(other.number)
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        number.randomize(randomness, tryToForceNewValue)
    }


    override fun adaptiveSelectSubsetToMutate(
        randomness: Randomness,
        internalGenes: List<Gene>,
        mwc: MutationWeightControl,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo
    ): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is NumericStringGeneImpact){
            return listOf(number to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.numberGeneImpact, gene = number))
        }
        throw IllegalArgumentException("impact is null or not OptionalGeneImpact")
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        // avoid scientific representation if the number is string
        return "\"" + number.value.toPlainString() + "\""
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        return when(gene){
            is NumericStringGene -> number.setValueBasedOn(gene.number)
            else-> number.setValueBasedOn(gene)
        }
    }

    override fun compareTo(other: ComparableGene): Int {
        if (other !is NumericStringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return number.compareTo(other.number)
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

}