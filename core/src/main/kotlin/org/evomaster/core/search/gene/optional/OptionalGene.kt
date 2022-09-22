package org.evomaster.core.search.gene.optional

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.value.OptionalGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
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
                   val gene: Gene,
                   var isActive: Boolean = true,
                   var requestSelection: Boolean = false,
                   val searchPercentageActive: Double = 1.0
) : CompositeFixedGene(name, gene) {


    companion object{
        private val log: Logger = LoggerFactory.getLogger(OptionalGene::class.java)
        private const val INACTIVE = 0.01
    }

    /**
     * In some cases, we might want to prevent this gene from being active
     */
    var selectable = true
        private set

    init {
        if(searchPercentageActive < 0 || searchPercentageActive > 1){
            throw IllegalArgumentException("Invalid searchPercentageActive value: $searchPercentageActive")
        }
    }


    override fun isLocallyValid() : Boolean{
        return getViewOfChildren().all { it.isLocallyValid() }
    }

    fun forbidSelection(){
        selectable = false
        isActive = false
    }

    override fun copyContent(): Gene {
        val copy = OptionalGene(name, gene.copy(), isActive, requestSelection)
        copy.selectable = this.selectable
        return copy
    }

    override fun isMutable(): Boolean {
        return selectable
    }

    override fun <T> getWrappedGene(klass: Class<T>) : T?  where T : Gene{
        if(this.javaClass == klass){
            return this as T
        }
        return gene.getWrappedGene(klass)
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

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        if(!gene.initialized && gene.isMutable()){
            //make sure that, if not initialized, to randomize it, to make sure constraints are satisfied
            gene.randomize(randomness, false)
        }

        if(!selectable){
            return
        }

        if (!tryToForceNewValue) {
            isActive = randomness.nextBoolean()
            if(gene.isMutable()) {
                gene.randomize(randomness, false)
            }
        } else {

            if (randomness.nextBoolean() || !gene.isMutable()) {
                isActive = !isActive
            } else {
                gene.randomize(randomness, true)
            }
        }
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

    override fun mutablePhenotypeChildren(): List<Gene> {

        if (!isActive || !gene.isMutable()) return emptyList()

        return listOf(gene)
    }

    override fun adaptiveSelectSubsetToMutate(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is OptionalGeneImpact){
            if (internalGenes.size != 1 || !internalGenes.contains(gene))
                throw IllegalStateException("mismatched input: the internalGenes should only contain gene")
            return listOf(gene to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.geneImpact, gene = gene))
        }
        throw IllegalArgumentException("impact is null or not OptionalGeneImpact")
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{

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



    override fun mutationWeight(): Double {
        return 1.0 + gene.mutationWeight()
    }

    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is OptionalGene) isActive = gene.isActive
        return ParamUtil.getValueGene(this).bindValueBasedOn(ParamUtil.getValueGene(gene))
    }

    override fun isPrintable(): Boolean {
        return gene.isPrintable()
    }
}