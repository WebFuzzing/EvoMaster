package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import org.evomaster.core.search.impact.value.OptionalGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.evomaster.core.search.service.mutator.geneMutation.IntMutationUpdate
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

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {

        if(!selectable){
            return
        }

        if (!isActive) {
            isActive = true
        } else {

            if (randomness.nextBoolean(INACTIVE) || !gene.isMutable()) {
                /*
                    the idea here is that we deactivate an element only with small
                    probability. however, if a gene is not mutable, the only mutation
                    we can do is to switch from active to inactive, and vice-versa
                 */
                isActive = false
            } else {
                gene.standardMutation(randomness, apc, allGenes)
            }
        }
    }

    override fun archiveMutation(randomness: Randomness, allGenes: List<Gene>, apc: AdaptiveParameterControl, selection: GeneMutationSelectionMethod, impact: GeneImpact?, geneReference: String, archiveMutator: ArchiveMutator, evi: EvaluatedIndividual<*>, targets: Set<Int>) {

        if(!selectable){
            return
        }

        if(!archiveMutator.enableArchiveMutation()){
            standardMutation(randomness, apc, allGenes)
            return
        }

        val preferActive = if (!archiveMutator.applyArchiveSelection() || impact == null || impact !is OptionalGeneImpact) true
        else {
            !impact.activeImpact.run {
                //we only set 'active' false from true when the mutated times is more than 5 and its impact times of a falseValue is more than 1.5 times of a trueValue.
                this.timesToManipulate > 5
                        &&
                        (this.falseValue.timesOfImpact.filter { targets.contains(it.key) }.map { it.value }.max()?:0) > ((this.trueValue.timesOfImpact.filter { targets.contains(it.key) }.map { it.value }.max()?:0) * 1.5)
            }
        }

        if (preferActive){
            if (!isActive){
                isActive = true
                activeMutationInfo.counter+=1
                return
            }
            if (randomness.nextBoolean(INACTIVE)){
                isActive = false
                activeMutationInfo.counter+=1
                return
            }
        }else{
            //if preferPresent is false, it is not necessary to mutate the gene
            activeMutationInfo.reached = archiveMutator.withinNormal()
            if (activeMutationInfo.reached){
                activeMutationInfo.preferMin = 0
                activeMutationInfo.preferMax = 0
            }

            if (isActive){
                isActive = false
                activeMutationInfo.counter+=1
                return
            }
            if (randomness.nextBoolean(INACTIVE)){
                isActive = true
                activeMutationInfo.counter+=1
                return
            }
        }
        gene.archiveMutation(randomness, allGenes, apc, selection, if (impact == null || impact !is OptionalGeneImpact) null else impact.geneImpact, geneReference, archiveMutator, evi, targets)

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

    override fun reachOptimal(): Boolean {
        return (activeMutationInfo.reached && activeMutationInfo.preferMin == 0 && activeMutationInfo.preferMax == 0) ||  gene.reachOptimal()

    }

    override fun archiveMutationUpdate(original: Gene, mutated: Gene, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator) {
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
                gene.archiveMutationUpdate(original.gene, mutated.gene, doesCurrentBetter, archiveMutator)
            /**
             * may handle Boolean Mutation in the future
             */
        }
    }
}