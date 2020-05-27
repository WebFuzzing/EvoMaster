package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.impact.value.collection.MapGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.geneMutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.evomaster.core.search.service.mutator.geneMutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * FIXME: this needs to be refactored, as at the moment the
 * keys are strings that are fixed.
 * Keys should be of any basic type, and should be modifiable.
 *
 */
class MapGene<T>(
        name: String,
        val template: T,
        val maxSize: Int = 5,
        var elements: MutableList<T> = mutableListOf()
) : Gene(name)
        where T : Gene {

    private var keyCounter = 0

    init {
        if (elements.size > maxSize) {
            throw IllegalArgumentException(
                    "More elements (${elements.size}) than allowed ($maxSize)")
        }

        for(e in elements){
            e.parent = this
        }
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(MapGene::class.java)
        private const val MODIFY_SIZE = 0.1
    }

    override fun copy(): Gene {
        return MapGene<T>(name,
                template.copy() as T,
                maxSize,
                elements.map { e -> e.copy() as T }.toMutableList()
        )
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is MapGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.elements = other.elements.map { e -> e.copy() as T }.toMutableList()
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is MapGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.elements.size == other.elements.size
                && this.elements.zip(other.elements) { thisElem, otherElem ->
            thisElem.containsSameValueAs(otherElem)
        }.all { it }
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        //maybe not so important here to complicate code to enable forceNewValue

        elements.clear()
        log.trace("Randomizing MapGene")
        val n = randomness.nextInt(maxSize)
        (0 until n).forEach {
            val gene = template.copy() as T
            gene.parent = this
            gene.randomize(randomness, false)
            gene.name = "key_${keyCounter++}"
            elements.add(gene)
        }
    }

    override fun isMutable(): Boolean {
        //it wouldn't make much sense to have 0, but let's just be safe here
        return maxSize > 0
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): List<Gene> {
        if(!isMutable()){
            throw IllegalStateException("Cannot mutate a immutable array")
        }
        if ( elements.isEmpty() || elements.size > maxSize){
            return listOf()
        }
        val p = when(selectionStrategy){
            SubsetGeneSelectionStrategy.ADAPTIVE_WEIGHT -> {
                if(additionalGeneMutationInfo?.impact != null
                        && additionalGeneMutationInfo.impact is MapGeneImpact
                        && additionalGeneMutationInfo.impact.sizeImpact.noImprovement.any { it.value < 2 } //if there is recent improvement by manipulating size
                ){
                    0.3 // increase probability to mutate size
                }else MODIFY_SIZE
            }
            else ->{
                MODIFY_SIZE

            }
        }
        return if (randomness.nextBoolean(p)) listOf() else elements
    }

    /**
     * leaf mutation for arrayGene is size mutation, i.e., 'remove' or 'add'
     */
    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?) : Boolean{

        if(elements.isEmpty() || (elements.size < maxSize && randomness.nextBoolean())){
            val gene = template.copy() as T
            gene.parent = this
            gene.randomize(randomness, false)
            gene.name = "key_${keyCounter++}"
            elements.add(gene)
        } else {
            log.trace("Removing gene in mutation")
            elements.removeAt(randomness.nextInt(elements.size))
        }
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        return "{" +
                elements.filter { f ->
                    f !is CycleObjectGene &&
                            (f !is OptionalGene || f.isActive)
                }.map { f ->
                    """
                    "${f.name}":${f.getValueAsPrintableString(targetFormat = targetFormat)}
                    """
                }.joinToString(",") +
                "}"
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if (excludePredicate(this)) listOf(this)
        else listOf(this).plus(elements.flatMap { g -> g.flatView(excludePredicate) })
    }

    override fun archiveMutationUpdate(original: Gene, mutated: Gene, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator) {
        if (archiveMutator.enableArchiveGeneMutation()){
            if (original !is MapGene<*>){
                log.warn("original ({}) should be MapGene", original::class.java.simpleName)
                return
            }
            if (mutated !is MapGene<*>){
                log.warn("mutated ({}) should be MapGene", mutated::class.java.simpleName)
                return
            }
            if (original.elements.size != mutated.elements.size) return
            val mutatedElements = mutated.elements.filterIndexed { index, gene ->
                !gene.containsSameValueAs(original.elements[index])
            }
            if (mutatedElements.size > 1){
                log.warn("size of mutated elements is more than 1, i.e.,{}", mutatedElements.size)
                return
            }
            val index = mutated.elements.indexOf(mutatedElements.first())
            if (index > elements.size - 1){
                log.warn("cannot find element at index {}", index)
                return
            }
            elements[index].archiveMutationUpdate(original.elements[index], mutated.elements[index], doesCurrentBetter, archiveMutator)
        }
    }

    /**
     * 1 is for 'remove' or 'add' element
     */
    override fun mutationWeight(): Int {
        return 1 + elements.sumBy { it.mutationWeight() }
    }
}