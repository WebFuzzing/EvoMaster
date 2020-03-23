package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import org.evomaster.core.search.impact.value.collection.MapGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
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

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {

        if(elements.isEmpty() || (elements.size < maxSize && randomness.nextBoolean(MODIFY_SIZE))){
            val gene = template.copy() as T
            gene.parent = this
            gene.randomize(randomness, false)
            gene.name = "key_${keyCounter++}"
            elements.add(gene)
        } else if(elements.size > 0 && randomness.nextBoolean(MODIFY_SIZE)){
            log.trace("Removing gene in mutation")
            elements.removeAt(randomness.nextInt(elements.size))
        } else {
            val gene = randomness.choose(elements)
            gene.standardMutation(randomness, apc, allGenes)
        }
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

    override fun archiveMutation(randomness: Randomness, allGenes: List<Gene>, apc: AdaptiveParameterControl, selection: GeneMutationSelectionMethod, impact: GeneImpact?, geneReference: String, archiveMutator: ArchiveMutator, evi: EvaluatedIndividual<*>, targets: Set<Int>) {
        var add = elements.isEmpty()
        var delete = (elements.size == maxSize)

        val fmodifySize = if (add || delete) false
        else if (archiveMutator.applyArchiveSelection()
                && impact != null
                && impact is MapGeneImpact
                && impact.sizeImpact.noImprovement.any { it.value < 2 } //if there is recent improvement by manipulating size
        ){
            randomness.nextBoolean(0.3)
        }else {
            randomness.nextBoolean(MODIFY_SIZE)
        }
        if (fmodifySize){
            val p = randomness.nextBoolean()
            add = add || p
            delete = delete || !p
        }

        if (add && (add == delete))
            log.warn("add and delete an element cannot happen in a mutation, and size of elements: {} and maxSize: {}", elements.size, maxSize)

        when{
            add ->{
                val gene = template.copy() as T
                gene.randomize(randomness, false)
                gene.name = "key_${keyCounter++}"
                elements.add(gene)
                return
            }
            delete ->{
                log.trace("Deleting gene")
                elements.removeAt(randomness.nextInt(elements.size))
                return
            }
            else -> {
                val gene = randomness.choose(elements)
                gene.archiveMutation(randomness, allGenes, apc, selection, null, geneReference, archiveMutator, evi, targets)
            }
        }
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
}