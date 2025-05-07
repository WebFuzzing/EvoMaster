package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.regex.RegexGeneImpact
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy

/**
 * A gene representing a regular expression (regex).
 */
class RegexGene(
        name: String,
        val disjunctions: DisjunctionListRxGene,
        val sourceRegex : String
) : CompositeFixedGene(name, disjunctions) {

    companion object{
        const val JAVA_REGEX_PREFIX = "Java:"
        const val DATABASE_REGEX_PREFIX = "Database:"
        const val DATABASE_REGEX_SEPARATOR = "||"
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene {
        return RegexGene(name, disjunctions.copy() as DisjunctionListRxGene, sourceRegex = sourceRegex)
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        disjunctions.randomize(randomness, tryToForceNewValue)
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

    override fun isMutable(): Boolean {
        return disjunctions.isMutable()
    }


    override fun adaptiveSelectSubsetToMutate(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is RegexGeneImpact){
            if (internalGenes.size != 1 || !internalGenes.contains(disjunctions))
                throw IllegalArgumentException("mismatched internal gene")
            return listOf(disjunctions to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.listRxGeneImpact, disjunctions))
        }
        throw IllegalArgumentException("mismatched gene impact")
    }


    override fun getValueAsRawString(): String {
        return disjunctions.getValueAsPrintableString(targetFormat = null)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        val rawValue = getValueAsRawString()
        when {
            // TODO Should refactor since this code block is equivalent to StringGene.getValueAsPrintableString()
            /*(targetFormat == null) -> return "\"$rawValue\""
            targetFormat.isKotlin() -> return "\"$rawValue\""
                    .replace("\\", "\\\\")
                    .replace("$", "\\$")
            else -> return "\"$rawValue\""
                    .replace("\\", "\\\\")
             */
            (targetFormat == null) -> return "\"${rawValue}\""
            //"\"${rawValue.replace("\"", "\\\"")}\""
            (mode != null) -> return "\"${GeneUtils.applyEscapes(rawValue, mode, targetFormat)}\""
            else -> return "\"${GeneUtils.applyEscapes(rawValue, GeneUtils.EscapeMode.TEXT ,targetFormat)}\""
        }
    }


    override fun copyValueFrom(other: Gene): Boolean {
        if(other !is RegexGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            {this.disjunctions.copyValueFrom(other.disjunctions)}, false
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is RegexGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.disjunctions.containsSameValueAs(other.disjunctions)
    }



    /**
     * use mutationweight of [disjunctions]
     */
    override fun mutationWeight(): Double = disjunctions.mutationWeight()


    override fun setValueBasedOn(gene: Gene): Boolean {
        if (gene is RegexGene){
            return disjunctions.setValueBasedOn(gene.disjunctions)
        }
        return false
    }
}