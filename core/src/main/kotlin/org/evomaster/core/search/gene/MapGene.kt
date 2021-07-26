package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneMutator
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
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
        var maxSize: Int = MAX_SIZE,
        private var elements: MutableList<T> = mutableListOf()
) : CollectionGene, Gene(name, elements)
        where T : Gene {

    private var keyCounter = 0

    init {
        if (elements.size > maxSize) {
            throw IllegalArgumentException(
                    "More elements (${elements.size}) than allowed ($maxSize)")
        }
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(MapGene::class.java)
        const val MAX_SIZE = 5
    }

    override fun getChildren(): MutableList<T> {
        return elements
    }

    override fun copyContent(): Gene {
        return MapGene<T>(name,
                template.copyContent() as T,
                maxSize,
                elements.map { e -> e.copyContent() as T }.toMutableList()
        )
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is MapGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        clearElements()
        this.elements = other.elements.map { e -> e.copyContent() as T }.toMutableList()
        addChildren(this.elements)
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
//            gene.parent = this
            gene.randomize(randomness, false)
            gene.name = "key_${keyCounter++}"
            elements.add(gene)
        }
        addChildren(elements)
    }

    override fun isMutable(): Boolean {
        //it wouldn't make much sense to have 0, but let's just be safe here
        return maxSize > 0
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): List<Gene> {
        if(!isMutable()){
            throw IllegalStateException("Cannot mutate a immutable array")
        }
        val mutable = elements.filter { it.isMutable() }
        if ( mutable.isEmpty() || mutable.size > maxSize){
            return listOf()
        }

        val p = probabilityToModifySize(selectionStrategy, additionalGeneMutationInfo?.impact)

        return if (randomness.nextBoolean(p)) listOf() else mutable
    }

    override fun adaptiveSelectSubset(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        /*
            element is dynamically modified, then we do not collect impacts for it now.
            thus for the internal genes, adaptive gene selection for mutation is not applicable
        */
        val s = randomness.choose(internalGenes)
        return listOf(s to additionalGeneMutationInfo.copyFoInnerGene(null, s))
    }

    /**
     * leaf mutation for arrayGene is size mutation, i.e., 'remove' or 'add'
     */
    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{

        if(elements.isEmpty() || (elements.size < maxSize && randomness.nextBoolean())){
            val gene = template.copy() as T
//            gene.parent = this
            gene.randomize(randomness, false)
            gene.name = "key_${keyCounter++}"
            elements.add(gene)
            addChild(gene)
        } else {
            log.trace("Removing gene in mutation")
            val removed = elements.removeAt(randomness.nextInt(elements.size))
            removed.removeThisFromItsBindingGenes()
        }
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
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

    /**
     * 1 is for 'remove' or 'add' element
     */
    override fun mutationWeight(): Double {
        return 1.0 + elements.map { it.mutationWeight() }.sum()
    }

    override fun innerGene(): List<Gene> = elements


    /*
        Note that value binding cannot be performed on the [elements]
     */
    override fun bindValueBasedOn(gene: Gene): Boolean {
        if(gene is MapGene<*> && gene.template::class.java.simpleName == template::class.java.simpleName){
            clearElements()
            elements = gene.elements.mapNotNull { it.copyContent() as? T }.toMutableList()
            addChildren(elements)
            return true
        }
        LoggingUtil.uniqueWarn(log, "cannot bind the MapGene with the template (${template::class.java.simpleName}) with the gene ${gene::class.java.simpleName}")
        return false
    }

    override fun clearElements() {
        elements.forEach { it.removeThisFromItsBindingGenes() }
        elements.clear()
    }

    fun removeElements(element: T){
        elements.remove(element)
        element.removeThisFromItsBindingGenes()
    }

    fun addElements(element: T){
        elements.add(element)
        addChild(element)
    }

    fun getAllElements() = elements
}