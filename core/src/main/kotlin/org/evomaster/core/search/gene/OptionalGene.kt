package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.impact.impactInfoCollection.value.OptionalGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.geneMutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.evomaster.core.search.service.mutator.geneMutation.IntMutationUpdate
import org.evomaster.core.search.service.mutator.geneMutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A gene that might or might not be active.
 * An example are for query parameters in URLs
 */
class OptionalGene(name: String,
                   val gene: Gene,
                   var isActive: Boolean = true,
                   val activeMutationInfo : IntMutationUpdate = IntMutationUpdate(0, 1))
    : Gene(name) {


    companion object{
        private val log: Logger = LoggerFactory.getLogger(OptionalGene::class.java)
        private const val INACTIVE = 0.01
    }

    /**
     * In some cases, we might want to prevent this gene from being active
     */
    var selectable = true
        private set


    init{
        gene.parent = this
    }


    fun forbidSelection(){
        selectable = false
        isActive = false
    }

    override fun copy(): Gene {
        val copy = OptionalGene(name, gene.copy(), isActive, activeMutationInfo.copy())
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

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): List<Gene> {

        if (!isActive) return emptyList()

        if (!enableAdaptiveGeneMutation){
            return if (randomness.nextBoolean(INACTIVE)) emptyList() else listOf(gene)
        }
        if (additionalGeneMutationInfo?.impact != null && additionalGeneMutationInfo.impact is OptionalGeneImpact){
            //we only set 'active' false from true when the mutated times is more than 5 and its impact times of a falseValue is more than 1.5 times of a trueValue.
            val inactive = additionalGeneMutationInfo.impact.activeImpact.run {
                this.getTimesToManipulate() > 5
                        &&
                        (this.falseValue.getTimesOfImpacts().filter { additionalGeneMutationInfo.targets.contains(it.key) }.map { it.value }.max()?:0) > ((this.trueValue.getTimesOfImpacts().filter { additionalGeneMutationInfo.targets.contains(it.key) }.map { it.value }.max()?:0) * 1.5)
            }
            if (inactive) return emptyList() else listOf(gene)
        }
        throw IllegalArgumentException("impact is null or not OptionalGeneImpact")
    }

    override fun adaptiveSelectSubset(internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneSelectionInfo): List<Pair<Gene, AdditionalGeneSelectionInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is OptionalGeneImpact){
            if (internalGenes.size != 1 || !internalGenes.contains(gene))
                throw IllegalStateException("mismatched input: the internalGenes should only contain gene")
            return listOf(gene to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.geneImpact))
        }
        throw IllegalArgumentException("impact is null or not OptionalGeneImpact")
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?) : Boolean{

        isActive = !isActive
        if (enableAdaptiveGeneMutation){
            //TODO MAN further check
            activeMutationInfo.counter+=1
        }

        return true
    }


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
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

    override fun reachOptimal(targets: Set<Int>): Boolean {
        return (activeMutationInfo.reached && activeMutationInfo.preferMin == 0 && activeMutationInfo.preferMax == 0) ||  gene.reachOptimal(targets)

    }

    override fun archiveMutationUpdate(original: Gene, mutated: Gene, targetsEvaluated: Map<Int, Int>, archiveMutator: ArchiveMutator) {
        if (archiveMutator.enableArchiveGeneMutation()){
            if (original !is OptionalGene){
                log.warn("original ({}) should be OptionalGene", original::class.java.simpleName)
                return
            }
            if (mutated !is OptionalGene){
                log.warn("mutated ({}) should be OptionalGene", mutated::class.java.simpleName)
                return
            }
            if (original.isActive == mutated.isActive && mutated.isActive)
                gene.archiveMutationUpdate(original.gene, mutated.gene, targetsEvaluated, archiveMutator)
            /**
             * may handle Boolean Mutation in the future
             */
        }
    }

    override fun mutationWeight(): Double {
        return 1.0 + gene.mutationWeight()
    }
}