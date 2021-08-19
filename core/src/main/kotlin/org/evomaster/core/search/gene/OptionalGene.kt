package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.impact.impactinfocollection.value.OptionalGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A gene that might or might not be active.
 * An example are for query parameters in URLs
 */
class OptionalGene(name: String,
                   val gene: Gene,
                   var isActive: Boolean = true,
                   /**
                    * In some cases, we might add new optional genes that are off by default.
                    * This is the case for we "expand" the genotype of an individual with new
                    * info coming from the search.
                    * But, in these cases, to avoid modifying the phenotype, we must leave them off
                    * by default.
                    * However, we might want to tell the search that, at the next mutation, we should
                    * put them on.
                    */
                   var requestSelection: Boolean = false)
    : Gene(name, mutableListOf(gene)) {


    companion object{
        private val log: Logger = LoggerFactory.getLogger(OptionalGene::class.java)
        private const val INACTIVE = 0.01
    }

    /**
     * In some cases, we might want to prevent this gene from being active
     */
    var selectable = true
        private set


    fun forbidSelection(){
        selectable = false
        isActive = false
    }

    override fun getChildren(): MutableList<Gene> = mutableListOf(gene)

    override fun copyContent(): Gene {
        val copy = OptionalGene(name, gene.copyContent(), isActive, requestSelection)
        copy.selectable = this.selectable
        return copy
    }

    override fun isMutable(): Boolean {
        return selectable
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is OptionalGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.isActive = other.isActive
        this.selectable = other.selectable
        this.requestSelection = other.requestSelection
        this.gene.copyValueFrom(other.gene)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is OptionalGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.isActive == other.isActive
                && this.gene.containsSameValueAs(other.gene)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        if(!selectable){
            return
        }

        if (!forceNewValue) {
            isActive = randomness.nextBoolean()
            gene.randomize(randomness, false, allGenes)
        } else {

            if (randomness.nextBoolean()) {
                isActive = !isActive
            } else {
                gene.randomize(randomness, true, allGenes)
            }
        }
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): List<Gene> {

        if (!isActive || !gene.isMutable()) return emptyList()

        if (!enableAdaptiveGeneMutation || additionalGeneMutationInfo?.impact == null){
            return if (randomness.nextBoolean(INACTIVE)) emptyList() else listOf(gene)
        }
        if (additionalGeneMutationInfo.impact is OptionalGeneImpact){
            //we only set 'active' false from true when the mutated times is more than 5 and its impact times of a falseValue is more than 1.5 times of a trueValue.
            val inactive = additionalGeneMutationInfo.impact.activeImpact.determinateSelect(
                    minManipulatedTimes = 5,
                    times = 1.5,
                    preferTrue = true,
                    targets = additionalGeneMutationInfo.targets,
                    selector = additionalGeneMutationInfo.archiveGeneSelector
            )
            return if (inactive) emptyList() else listOf(gene)
        }
        throw IllegalArgumentException("impact is null or not OptionalGeneImpact ${additionalGeneMutationInfo?.impact}")
    }

    override fun adaptiveSelectSubset(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is OptionalGeneImpact){
            if (internalGenes.size != 1 || !internalGenes.contains(gene))
                throw IllegalStateException("mismatched input: the internalGenes should only contain gene")
            return listOf(gene to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.geneImpact, gene = gene))
        }
        throw IllegalArgumentException("impact is null or not OptionalGeneImpact")
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{

        isActive = !isActive
        if (enableAdaptiveGeneMutation){
            //TODO MAN further check

        }

        return true
    }


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return gene.getValueAsPrintableString(mode = mode, targetFormat = targetFormat)
    }

    override fun getValueAsRawString(): String {
        return gene.getValueAsRawString()
    }

    override fun getVariableName() = gene.getVariableName()


    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(gene.flatView(excludePredicate))
    }

    override fun mutationWeight(): Double {
        return 1.0 + gene.mutationWeight()
    }

    override fun innerGene(): List<Gene> = listOf(gene)

    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is OptionalGene) isActive = gene.isActive
        return ParamUtil.getValueGene(this).bindValueBasedOn(ParamUtil.getValueGene(gene))
    }

}