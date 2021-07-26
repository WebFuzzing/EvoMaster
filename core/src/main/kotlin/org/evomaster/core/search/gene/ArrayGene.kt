package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneMutator
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException


/**
 *  A representation of typical array, for a fixed type T, ie, no mixed types are allowed here.
 */
class ArrayGene<T>(
        /**
         * The name of this gene
         */
        name: String,
        /**
         * The type for this array. Every time we create a new element to add, it has to be based
         * on this template
         */
        val template: T,
        /**
         *  How max elements to have in this array. Usually arrays are unbound, till the maximum int size (ie, 2 billion
         *  elements on the JVM). But, for search reasons, too large arrays are impractical
         */
        var maxSize: Int = MAX_SIZE,
        /**
         * The actual elements in the array, based on the template. Ie, usually those elements will be clones
         * of the templated, and then mutated/randomized
         */
        private var elements: MutableList<T> = mutableListOf()
) : CollectionGene, Gene(name, elements)
        where T : Gene {

    init {
        if(template is CycleObjectGene){
            maxSize = 0
            clearElements()
        }

        if (elements.size > maxSize) {
            throw IllegalArgumentException(
                    "More elements (${elements.size}) than allowed ($maxSize)")
        }
    }

    companion object{
        val log : Logger = LoggerFactory.getLogger(ArrayGene::class.java)
        const val MAX_SIZE = 5
    }


    fun forceToOnlyEmpty(){
        maxSize = 0
        clearElements()
    }

    override fun getChildren(): MutableList<T> = elements

    override fun copyContent(): Gene {
        return ArrayGene<T>(name,
                template.copyContent() as T,
                maxSize,
                elements.map { e -> e.copyContent() as T }.toMutableList()
        )
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is ArrayGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        clearElements()
        this.elements = other.elements.map { e -> e.copyContent() as T }.toMutableList()
        // build parents for [element]
        addChildren(this.elements)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is ArrayGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.elements.zip(other.elements) { thisElem, otherElem ->
            thisElem.containsSameValueAs(otherElem)
        }.all { it }
    }


    override fun isMutable(): Boolean {
        /*
            if maxSize is 0, then array cannot be mutated, as it will always be empty.
            If it is greater than 0, it can always be mutated, regardless of whether the
            elements can be mutated: we can mutate between empty and 1-element arrays
         */
        return maxSize > 0
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        if(maxSize == 0){
            //nothing to do
            return
        }

        //maybe not so important here to complicate code to enable forceNewValue
        clearElements()
        log.trace("Randomizing ArrayGene")
        val n = randomness.nextInt(maxSize)
        (0 until n).forEach {
            val gene = template.copy() as T
//            gene.parent = this
            gene.randomize(randomness, false)
            elements.add(gene)
            addChild(gene)
        }
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
        /*
            TODO impact for an element in ArrayGene
         */
        return listOf(s to additionalGeneMutationInfo.copyFoInnerGene(ImpactUtils.createGeneImpact(s, s.name), s))
    }

    /**
     * leaf mutation for arrayGene is size mutation, i.e., 'remove' or 'add'
     */
    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{

        if(elements.isEmpty() || (elements.size < maxSize && randomness.nextBoolean())){
            val gene = template.copy() as T
//            gene.parent = this
            gene.randomize(randomness, false)
            elements.add(gene)
            addChild(gene)
        }else{
            log.trace("Remvoving gene in mutation")
            val removed = elements.removeAt(randomness.nextInt(elements.size))
            // remove binding if any other bound with
            removed.removeThisFromItsBindingGenes()
        }
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return "[" +
                elements.map { g -> g.getValueAsPrintableString(previousGenes, mode, targetFormat) }.joinToString(", ") +
                "]"
    }


    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(elements.flatMap { g -> g.flatView(excludePredicate) })
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

        TODO might bind based on value instead of replacing them
     */
    override fun bindValueBasedOn(gene: Gene): Boolean {
        if(gene is ArrayGene<*> && gene.template::class.java.simpleName == template::class.java.simpleName){
            clearElements()
            elements = gene.elements.mapNotNull { it.copyContent() as? T}.toMutableList()
            addChildren(elements)
            return true
        }
        LoggingUtil.uniqueWarn(log, "cannot bind ArrayGene with the template (${template::class.java.simpleName}) with ${gene::class.java.simpleName}")
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