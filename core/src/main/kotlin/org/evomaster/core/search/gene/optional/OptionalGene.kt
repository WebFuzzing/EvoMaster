package org.evomaster.core.search.gene.optional

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.value.OptionalGeneImpact
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A gene that might or might not be active.
 * An example are for query parameters in URLs
 *
 * @param isActive whether the enclosed gene will be expressed in the phenotype
 * @param requestSelection In some cases, we might add new optional genes that are off by default.
 *                         This is the case for we "expand" the genotype of an individual with new
 *                         info coming from the search.
 *                         But, in these cases, to avoid modifying the phenotype, we must leave them off
 *                         by default.
 *                         However, we might want to tell the search that, at the next mutation, we should
 *                         put them on.
 * @param searchPercentageActive for how long the gene can be active. After this percentage [0.0,1.0] of time has passed,
 *                      then we deactivate it and forbid its selection.
 *                      By default, it can be used during whole search (ie, 1.0).
 */
class OptionalGene(name: String,
                   gene: Gene,
                   isActive: Boolean = true,
                   var requestSelection: Boolean = false,
                   var searchPercentageActive: Double = 1.0
) : SelectableWrapperGene(name, gene, isActive) {


    companion object{
        private val log: Logger = LoggerFactory.getLogger(OptionalGene::class.java)
        private const val INACTIVE = 0.01
    }


    init {
        if(searchPercentageActive < 0 || searchPercentageActive > 1){
            throw IllegalArgumentException("Invalid searchPercentageActive value: $searchPercentageActive")
        }
    }


    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }



    override fun copyContent(): Gene {
        val copy = OptionalGene(name, gene.copy(), isActive, requestSelection, searchPercentageActive)
        copy.selectable = this.selectable
        return copy
    }


    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is OptionalGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        return updateValueOnlyIfValid(
            {
                val ok = gene.copyValueFrom(other.gene)
                if (ok){
                    this.isActive = other.isActive
                    this.selectable = other.selectable
                    this.requestSelection = other.requestSelection
                    this.searchPercentageActive = other.searchPercentageActive
                }
                ok
            }, false
        )
    }

    override fun setValueBasedOn(value: String) : Boolean{
        val modified = gene.setValueBasedOn(value)
        if(modified){
            isActive = true
        }
        return modified
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is OptionalGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.isActive == other.isActive
                && this.gene.containsSameValueAs(other.gene)
                && this.selectable == other.selectable
                && this.searchPercentageActive == other.searchPercentageActive
    }



    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ) : Boolean {

        if (!isActive) return true

        if (!enableAdaptiveGeneMutation || additionalGeneMutationInfo?.impact == null){
            return randomness.nextBoolean(INACTIVE)
        }

        if (additionalGeneMutationInfo?.impact is OptionalGeneImpact){
            //we only set 'active' false from true when the mutated times is more than 5 and its impact times of a falseValue is more than 1.5 times of a trueValue.
            val inactive = additionalGeneMutationInfo.impact.activeImpact.determinateSelect(
                minManipulatedTimes = 5,
                times = 1.5,
                preferTrue = true,
                targets = additionalGeneMutationInfo.targets,
                selector = additionalGeneMutationInfo.archiveGeneSelector
            )
            return if (inactive) true else return false
        }
        throw IllegalArgumentException("impact is null or not OptionalGeneImpact ${additionalGeneMutationInfo?.impact}")
    }



    override fun adaptiveSelectSubsetToMutate(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is OptionalGeneImpact){
            if (internalGenes.size != 1 || !internalGenes.contains(gene))
                throw IllegalStateException("mismatched input: the internalGenes should only contain gene")
            return listOf(gene to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.geneImpact, gene = gene))
        }
        throw IllegalArgumentException("impact is null or not OptionalGeneImpact")
    }




    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        if(!isActive)
            return ""

        return gene.getValueAsPrintableString(mode = mode, targetFormat = targetFormat)
    }

    override fun getValueAsRawString(): String {
        if(!isActive)
            return ""
        return gene.getValueAsRawString()
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        if (gene is OptionalGene) isActive = gene.isActive
        return ParamUtil.getValueGene(this).setValueBasedOn(ParamUtil.getValueGene(gene))
    }

    override fun isChildUsed(child: Gene) : Boolean {
        verifyChild(child)
        return isActive
    }
}