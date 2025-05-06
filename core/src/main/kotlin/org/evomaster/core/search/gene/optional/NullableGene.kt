package org.evomaster.core.search.gene.optional

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.sql.NullableImpact
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NullableGene(name: String,
                    gene: Gene,
                    isActive: Boolean = true,
                   var nullLabel: String = "null"
) : SelectableWrapperGene(name, gene, isActive) {



    companion object{
        private val log: Logger = LoggerFactory.getLogger(NullableGene::class.java)
        private const val ABSENT = 0.01
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene {
        val g =  NullableGene(name, gene.copy(), isActive, nullLabel)
        g.selectable = this.selectable
        return g
    }


    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ) : Boolean {

        if (!isActive) return true

        //FIXME do impact
//        if (!enableAdaptiveGeneMutation || additionalGeneMutationInfo?.impact == null){
//            return if (randomness.nextBoolean(ABSENT)) emptyList() else listOf(gene)
//        }
//        if (additionalGeneMutationInfo.impact is SqlNullableImpact){
//            //we only set 'active' false from true when the mutated times is more than 5 and its impact times of a falseValue is more than 1.5 times of a trueValue.
//            val inactive = additionalGeneMutationInfo.impact.presentImpact.determinateSelect(
//                minManipulatedTimes = 5,
//                times = 1.5,
//                preferTrue = true,
//                targets = additionalGeneMutationInfo.targets,
//                selector = additionalGeneMutationInfo.archiveGeneSelector
//            )
//
//            return if (inactive)  emptyList() else listOf(gene)
//        }
//        throw IllegalArgumentException("impact is not SqlNullableImpact ${additionalGeneMutationInfo.impact::class.java.simpleName}")
//

        /*
            Here, it means the gene is present. A shallow mutation would put it to null, which we do only with low
            probability
         */
        return randomness.nextBoolean(ABSENT);
    }


    override fun adaptiveSelectSubsetToMutate(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is NullableImpact){
            return listOf(gene to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.geneImpact, gene))
        }
        throw IllegalArgumentException("impact is null or not SqlNullableImpact")
    }


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {

        if (!isActive) {
            return nullLabel
        }

        return gene.getValueAsPrintableString(previousGenes, mode, targetFormat)
    }

    override fun getValueAsRawString(): String {
        //todo double check
        if(!isActive)
            return nullLabel
        return gene.getValueAsRawString()
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is NullableGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            {
                val ok = this.gene.copyValueFrom(other.gene)
                if (ok){
                    this.isActive = other.isActive
                    this.selectable = other.selectable
                    this.nullLabel = other.nullLabel
                }
                ok
            }, false
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is NullableGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.isActive == other.isActive &&
                this.selectable == other.selectable &&
                this.gene.containsSameValueAs(other.gene)
                && this.nullLabel == other.nullLabel
    }



    override fun setValueBasedOn(gene: Gene): Boolean {
        if (gene is NullableGene) isActive = gene.isActive
        return ParamUtil.getValueGene(gene).setValueBasedOn(ParamUtil.getValueGene(gene))
    }

    override fun isChildUsed(child: Gene) : Boolean {
        verifyChild(child)
        return isActive
    }
}