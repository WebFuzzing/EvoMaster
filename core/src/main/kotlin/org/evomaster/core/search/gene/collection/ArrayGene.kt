package org.evomaster.core.search.gene.collection

import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.interfaces.CollectionGene
import org.evomaster.core.search.gene.interfaces.TaintableGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.placeholder.LimitObjectGene
import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
         * on this template.
         *
         * Note: here the template cannot be a KClass, because we might need to specify constraints on
         * the template (eg ranges for numbers)
         */
        val template: T,

        /**
         * specify whether the elements should be unique
         */
        val uniqueElements : Boolean = false,

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
         *
         * Man: change var to val to maintain list reference as its children
         *
         */
        elements: MutableList<T> = mutableListOf(),
        private var openingTag : String = "[",
        private var closingTag : String = "]",
        private var separatorTag : String = ", "
) : CollectionGene, CompositeGene(name, elements)
        where T : Gene {

    protected val elements : List<T>
        get() =  children as List<T>

    init {
        if(template is CycleObjectGene || template is LimitObjectGene){
            minSize = 0
            maxSize = 0
            killAllChildren()
        }

        if (minSize != null && maxSize != null && minSize!! > maxSize!!){
            throw IllegalArgumentException(
                "ArrayGene "+name+": minSize (${minSize}) is greater than maxSize ($maxSize)") }

        if (maxSize != null && elements.size > maxSize!!) {
            throw IllegalArgumentException(
                "ArrayGene "+name+": More elements (${elements.size}) than allowed ($maxSize)")
        }
        if(initialized && elements.any { !it.initialized }){
            throw IllegalArgumentException("Creating array marked as initialized but at least one child is not")
        }
    }

    companion object{
        val log : Logger = LoggerFactory.getLogger(ArrayGene::class.java)
        const val MAX_SIZE = 5
    }

    /**
     * Should only be called when an action is built, and not during mutation, as it
     * only affect how the gene is going to be represented as a string
     */
    fun modifyPrinting(open: String, close: String, sep: String){
        openingTag = open
        closingTag = close
        separatorTag = sep
    }

    fun forceToOnlyEmpty(){
        maxSize = 0
        killAllChildren()
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return elements.size >= (minSize ?: 0) && elements.size <= (maxSize ?: Int.MAX_VALUE)
    }

    override fun copyContent(): Gene {
        val copy = ArrayGene(
            name,
            template.copy() as T,
            uniqueElements,
            maxSize,
            minSize,
            elements.map { e -> e.copy() as T }.toMutableList(),
            openingTag = openingTag,
            closingTag = closingTag,
            separatorTag = separatorTag
        )
        if (copy.children.size!=this.children.size) {
            throw IllegalStateException("copy and its template have different size of children, e.g., copy (${copy.children.size}) vs. template (${this.children.size})")
        }
        return copy
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is ArrayGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        if (this.template::class.simpleName != other.template::class.simpleName) return false

        return updateValueOnlyIfValid(
            {
                killAllChildren()
                // check maxSize
                val elements = (if(maxSize!= null && other.elements.size > maxSize!!)
                    other.elements.subList(0, maxSize!!) else other.elements).map { e -> e.copy() as T }.toMutableList()
                // build parents for [element]
                addChildren(elements)
                true
            },
            false
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is ArrayGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        if (this.template::class.simpleName != other.template::class.simpleName) return false

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

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        if(maxSize == 0){
            return
        }

        //maybe not so important here to complicate code to enable forceNewValue
        killAllChildren()
        log.trace("Randomizing ArrayGene")
        val n = randomness.nextInt(getMinSizeOrDefault(), getMaxSizeUsedInRandomize())
        repeat(n) {
            val gene = createRandomElement(randomness)
            if (gene != null)
                addElement(gene)
        }
        assert(minSize==null || (minSize!! <= elements.size))
        assert(maxSize==null || (elements.size <= maxSize!!))
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ) : Boolean {

        //shallow mutation changes the size

        // if min == max, the size is not mutable
        if(getMinSizeOrDefault() == getMaxSizeOrDefault() && elements.size == getMinSizeOrDefault()){
            return false
        }

        val p = probabilityToModifySize(selectionStrategy, additionalGeneMutationInfo?.impact)
        return randomness.nextBoolean(p)
    }

    override fun adaptiveSelectSubsetToMutate(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
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
    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{

        if(elements.size < getMaxSizeOrDefault() && (elements.size == getMinSizeOrDefault() || elements.isEmpty() || randomness.nextBoolean())){
            val gene = createRandomElement(randomness) ?: return false
            addElement(gene)
        }else{
            log.trace("Removing gene in mutation")
            killChildByIndex(randomness.nextInt(elements.size)) as T
        }
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return openingTag +
                elements.map { g ->
                    if (GeneUtils.isGraphQLModes(mode)) {
                        if ((g.getWrappedGene(EnumGene::class.java)!=null))
                            g.getValueAsRawString() else {
                            g.getValueAsPrintableString(previousGenes, mode, targetFormat)
                        }

                    } else {
                        g.getValueAsPrintableString(previousGenes, mode, targetFormat)
                    }
                }.joinToString(separatorTag) +
                closingTag
    }


    /**
     * 1 is for 'remove' or 'add' element
     */
    override fun mutationWeight(): Double {
        return 1.0 + elements.map { it.mutationWeight() }.sum()
    }


    /*
        Note that value binding cannot be performed on the [elements]

        TODO might bind based on value instead of replacing them
     */
    override fun setValueBasedOn(gene: Gene): Boolean {
        if(gene is ArrayGene<*> && gene.template::class.java.simpleName == template::class.java.simpleName){
            killAllChildren()
            val elements = gene.elements.mapNotNull { it.copy() as? T}.toMutableList()
            elements.forEach { it.resetLocalIdRecursively() }
            if (!uniqueElements || gene.uniqueElements || !isElementApplicableToUniqueCheck(ParamUtil.getValueGene(template)))
                addChildren(elements)
            else{
                val unique = elements.filterIndexed { index, t ->
                    index == elements.indexOfLast { l-> ParamUtil.getValueGene(l).containsSameValueAs(ParamUtil.getValueGene(t)) }
                }
                Lazy.assert {
                    unique.isNotEmpty()
                }
                addChildren(unique)
            }
            return true
        }
        LoggingUtil.uniqueWarn(
            log,
            "cannot bind ArrayGene with the template (${template::class.java.simpleName}) with ${gene::class.java.simpleName}"
        )
        return false
    }

    /**
     * remove an existing element [element] from [elements]
     */
    fun removeExistingElement(element: T){
        //this is a reference heap check, not based on `equalsTo`
        if (elements.contains(element)){
            killChild(element)
        }else{
            log.warn("the specified element (${if (element.isPrintable()) element.getValueAsPrintableString() else "not printable"})) does not exist in this array")
        }
    }

    private fun createRandomElement(randomness: Randomness) : T? {
        val gene = template.copy() as T
        if(this.initialized){
            gene.doInitialize(randomness)
        } else if(gene.isMutable()) {
            gene.randomize(randomness, false)
        }

        gene.getWrappedGene(TaintableGene::class.java)?.forceNewTaintId()

        if (uniqueElements && doesExist(gene)){
            gene.randomize(randomness, true)
        }

        if (uniqueElements && doesExist(gene)){
            log.warn("tried twice, but still cannot create unique element for the gene")
            return null
        }

        return gene
    }

    /**
     * @return if the [gene] does exist in [elements]
     */
    fun doesExist(gene: T): Boolean{
        if (!isElementApplicableToUniqueCheck(ParamUtil.getValueGene(gene))) return false
        return elements.any { ParamUtil.getValueGene(it).containsSameValueAs(ParamUtil.getValueGene(gene)) }
    }

    /**
     * add an element [element] to [elements]
     */
    fun addElement(element: T){
        checkConstraintsForAdd()
        if (uniqueElements && doesExist(element))
            throw IllegalArgumentException("when uniqueElements is true, cannot add element which exists")

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
        if (uniqueElements && doesExist(element))
            throw IllegalArgumentException("when uniqueElements is true, cannot add element which exists")
        addChild(element)
        return true
    }

    fun getViewOfElements() = elements.toList()

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

    override fun isPrintable(): Boolean {
        return getViewOfChildren().all { it.isPrintable() }
    }
}