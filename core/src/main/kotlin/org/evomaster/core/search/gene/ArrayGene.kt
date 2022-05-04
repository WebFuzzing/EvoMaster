package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.min


/**
 *  A representation of typical array, for a fixed type T, ie, no mixed types are allowed here.
 */
open class ArrayGene<T>(
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
         *
         *  note that null maxSize means that the maxSize is not restricted,
         *  then we employ the default max size [MAX_SIZE] for handling mutation and randomizing values
         */
        var maxSize: Int? = null,

        var minSize: Int? = null,
        /**
         * The actual elements in the array, based on the template. Ie, usually those elements will be clones
         * of the templated, and then mutated/randomized
         */
        protected var elements: MutableList<T> = mutableListOf()
) : CollectionGene, Gene(name, elements)
        where T : Gene {

    init {
        if(template is CycleObjectGene){
            maxSize = 0
            clearElements()
        }

        if (minSize != null && maxSize != null && minSize!! > maxSize!!){
            throw IllegalArgumentException(
                "ArrayGene "+name+": minSize (${minSize}) is greater than maxSize ($maxSize)") }

        if (maxSize != null && elements.size > maxSize!!) {
            throw IllegalArgumentException(
                "ArrayGene "+name+": More elements (${elements.size}) than allowed ($maxSize)")
        }

        // might not check min size in constructor

        template.identifyAsRoot()
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
        val copy = ArrayGene<T>(name,
                template.copyContent() as T,
                maxSize,
                minSize,
                elements.map { e -> e.copyContent() as T }.toMutableList()
        )
        if (copy.getChildren().size!=this.getChildren().size) {
            throw IllegalStateException("copy and its template have different size of children, e.g., copy (${getChildren().size}) vs. template (${template.getChildren().size})")
        }
        return copy
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is ArrayGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        clearElements()
        // check maxSize
        this.elements = (if(maxSize!= null && other.elements.size > maxSize!!) other.elements.subList(0, maxSize!!) else other.elements).map { e -> e.copyContent() as T }.toMutableList()
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
        return getMaxSizeOrDefault() > 0
                // it is not mutable if the size could not be changed and none of the element is mutable
                && (!(getMinSizeOrDefault() == getMaxSizeOrDefault() && elements.size == getMinSizeOrDefault() && elements.none { it.isMutable() }))
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        if(maxSize == 0){
            //nothing to do
            return
        }

        //maybe not so important here to complicate code to enable forceNewValue
        clearElements()
        log.trace("Randomizing ArrayGene")
        val n = randomness.nextInt(getMinSizeOrDefault(), getMaxSizeUsedInRandomize())
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
        // if min == max, the size is not mutable
        if(getMinSizeOrDefault() == getMaxSizeOrDefault() && elements.size == getMinSizeOrDefault())
            return mutable
        // if mutable is empty, modify size
        if (mutable.isEmpty()) return listOf()

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

        if(elements.size < getMaxSizeOrDefault() && (elements.size == getMinSizeOrDefault() || elements.isEmpty() || randomness.nextBoolean())){
            val gene = template.copy() as T
            gene.randomize(randomness, false)
            addElement(gene)
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
                elements.map { g ->
                    if (GeneUtils.isGraphQLModes(mode)) {
                        if (g is EnumGene<*> || (g is OptionalGene && g.gene is EnumGene<*>))
                            g.getValueAsRawString() else {
                            g.getValueAsPrintableString(previousGenes, mode, targetFormat)
                        }

                    } else {
                        g.getValueAsPrintableString(previousGenes, mode, targetFormat)
                    }
                }.joinToString(", ") +
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

    /**
     * remove an existing element [element] from [elements]
     */
    fun removeExistingElement(element: T){
        //this is a reference heap check, not based on `equalsTo`
        if (elements.contains(element)){
            elements.remove(element)
            element.removeThisFromItsBindingGenes()
        }else{
            log.warn("the specified element (${if (element.isPrintable()) element.getValueAsPrintableString() else "not printable"})) does not exist in this array")
        }
    }

    /**
     * add an element [element] to [elements]
     */
    fun addElement(element: T){
        checkConstraintsForAdd()

        elements.add(element)
        addChild(element)
    }

    /**
     * add an element [element] to [elements],
     *      if the [element] does not conform with the [template], the addition could fail
     *
     * @return whether the element could be added into this elements
     *
     */
    fun addElement(element: Gene) : Boolean{
        element as? T ?: return false
        checkConstraintsForAdd()

        elements.add(element)
        addChild(element)
        return true
    }

    fun getAllElements() = elements

    override fun isEmpty(): Boolean {
        return elements.isEmpty()
    }

    override fun getSpecifiedMaxSize() = maxSize

    override fun getSpecifiedMinSize() = minSize

    override fun getGeneName() = name

    override fun getSizeOfElements(filterMutable: Boolean): Int {
        if (!filterMutable) return elements.size
        return elements.count { it.isMutable() }
    }

    override fun getMaxSizeOrDefault() = maxSize?: getDefaultMaxSize()

    override fun getMinSizeOrDefault() = minSize?: 0

    override fun getDefaultMaxSize() = (if (getMinSizeOrDefault() >= MAX_SIZE) (getMinSizeOrDefault() + MAX_SIZE) else MAX_SIZE)

}