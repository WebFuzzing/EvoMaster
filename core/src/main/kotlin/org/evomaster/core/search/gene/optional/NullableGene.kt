package org.evomaster.core.search.gene.optional

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlWrapperGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.sql.SqlNullableImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException

class NullableGene(name: String,
                   val gene: Gene,
                   var isPresent: Boolean = true,
                   var nullLabel: String = "null"
) : CompositeGene(name, mutableListOf(gene)) {



    companion object{
        private val log: Logger = LoggerFactory.getLogger(NullableGene::class.java)
        private const val ABSENT = 0.01
    }

    override fun <T> getWrappedGene(klass: Class<T>) : T?  where T : Gene{
        if(this.javaClass == klass){
            return this as T
        }
        return gene.getWrappedGene(klass)
    }

    override fun isLocallyValid() : Boolean{
        return getViewOfChildren().all { it.isLocallyValid() }
    }

    override fun copyContent(): Gene {
        return NullableGene(name, gene.copy(), isPresent, nullLabel)
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        isPresent = if (!isPresent && tryToForceNewValue) {
            true
        } else{
            /*
                on sampling, we consider a 50-50 chances to be null.
                then, during mutation, we give less chances to null
             */
            randomness.nextBoolean()
        }


        if(gene.isMutable())
            gene.randomize(randomness, tryToForceNewValue)
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ) : Boolean {

        if (!isPresent) return true

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

    override fun mutablePhenotypeChildren(): List<Gene> {

        if (!isPresent) return emptyList()

        return listOf(gene)
    }

    override fun adaptiveSelectSubsetToMutate(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is SqlNullableImpact){
            return listOf(gene to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.geneImpact, gene))
        }
        throw IllegalArgumentException("impact is null or not SqlNullableImpact")
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{
        isPresent = !isPresent
        return true
    }

    override fun isPrintable(): Boolean {
        return !isPresent || gene.isPrintable()
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {

        if (!isPresent) {
            return nullLabel
        }

        return gene.getValueAsPrintableString(previousGenes, mode, targetFormat)
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is NullableGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.isPresent = other.isPresent
        this.nullLabel = other.nullLabel
        this.gene.copyValueFrom(other.gene)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is NullableGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.isPresent == other.isPresent &&
                this.gene.containsSameValueAs(other.gene)
                && this.nullLabel == other.nullLabel
    }



    override fun mutationWeight(): Double {
        return 1.0 + gene.mutationWeight()
    }


    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is NullableGene) isPresent = gene.isPresent
        return ParamUtil.getValueGene(gene).bindValueBasedOn(ParamUtil.getValueGene(gene))
    }
}