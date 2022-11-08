package org.evomaster.core.search.gene.collection

import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.interfaces.CollectionGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class MapGene<K, V>(
    name: String,
    val template: PairGene<K, V>,
    val maxSize: Int? = null,
    val minSize: Int? = null,
    elements: MutableList<PairGene<K, V>> = mutableListOf()
) : CollectionGene, CompositeGene(name, elements)
        where K : Gene, V: Gene {

    private var keyCounter = 0

    protected val elements : List<PairGene<K, V>>
        get() {
            return getViewOfChildren() as List<PairGene<K, V>>
        }

    init {
        if (minSize != null && maxSize != null && minSize!! > maxSize!!){
            throw IllegalArgumentException(
                "MapGene "+name+": minSize (${minSize}) is greater than maxSize ($maxSize)")
        }
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(MapGene::class.java)
        const val MAX_SIZE = 5

        fun isStringMap(gene: MapGene<*, *>) = gene.template.first is StringGene && gene.template.second is StringGene
    }

    override fun isLocallyValid(): Boolean {
        return (minSize == null || elements.size >= minSize) && (maxSize == null || elements.size <= maxSize)
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        //maybe not so important here to complicate code to enable forceNewValue

        killAllChildren()
        log.trace("Randomizing MapGene")
        val n = randomness.nextInt(getMinSizeOrDefault(), getMaxSizeUsedInRandomize())

        addNElements(n, randomness)

        if (getSizeOfElements(false) < n && getSizeOfElements(false) < getMinSizeOrDefault()){
            addNElements(n-getSizeOfElements(false), randomness)
            if (getSizeOfElements(false)  < getMinSizeOrDefault())
                log.warn("cannot create ${getMinSizeOrDefault()} elements in order to satisfy its ${getMinSizeOrDefault()} min size")
        }
    }

    private fun addNElements(n : Int, randomness: Randomness){
        (0 until n).forEach { _ ->
            // if the key of gene exists, the value would be replaced with the latest one
            addElement(sampleRandomElementToAdd(randomness, false))
        }
    }

    override fun adaptiveSelectSubsetToMutate(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        /*
            element is dynamically modified, then we do not collect impacts for it now.
            thus for the internal genes, adaptive gene selection for mutation is not applicable
        */
        val s = randomness.choose(internalGenes)
        return listOf(s to additionalGeneMutationInfo.copyFoInnerGene(null, s))
    }

    override fun isMutable(): Boolean {
        //it wouldn't make much sense to have 0, but let's just be safe here
        return getMaxSizeOrDefault() > 0
                // it is not mutable if the size could not be changed and none of the element is mutable
                && (!(getMinSizeOrDefault() == getMaxSizeOrDefault() && elements.size == getMinSizeOrDefault() && elements.none { it.isMutable() }))
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

    /**
     * leaf mutation for arrayGene is size mutation, i.e., 'remove' or 'add'
     */
    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{

        if(elements.size < getMaxSizeOrDefault() && (elements.size == getMinSizeOrDefault() || elements.isEmpty() || randomness.nextBoolean())){
            val gene = sampleRandomElementToAdd(randomness, false)
            addElement(gene)
        } else {
            log.trace("Removing gene in mutation")
            val removed = killChildByIndex(randomness.nextInt(elements.size)) as Gene
            removed.removeThisFromItsBindingGenes()
        }
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {

        if(!isPrintable()){
            throw IllegalStateException("Trying to print a Map with unprintable template")
        }

        return "{" +
                elements.filter { f ->
                    isPrintable(f) && !isInactiveOptionalGene(f.first) && !isInactiveOptionalGene(f.second)
                }.joinToString(",") { f ->
                    """
                    ${getKeyValueAsPrintableString(f.first, targetFormat)}:${
                        f.second.getValueAsPrintableString(
                            targetFormat = targetFormat
                        )
                    }
                    """
                } +
                "}"
    }

    private fun isInactiveOptionalGene(gene: Gene): Boolean{
        val optional = gene.getWrappedGene(OptionalGene::class.java)?:return false

        return !optional.isActive
    }

    private fun getKeyValueAsPrintableString(key: Gene, targetFormat: OutputFormat?): String {
        val keyString = key.getValueAsPrintableString(targetFormat = targetFormat)
        if (!keyString.startsWith("\""))
            return "\"$keyString\""
        return keyString
    }

    protected fun isPrintable(pairGene: PairGene<K, V>): Boolean {
        val keyT = ParamUtil.getValueGene(pairGene.first)
        val valueT = pairGene.second
        return (keyT is LongGene || keyT is StringGene || keyT is IntegerGene || keyT is EnumGene<*>) &&
                (valueT !is CycleObjectGene && (valueT !is OptionalGene || valueT.isActive))
    }



    /**
     * 1 is for 'remove' or 'add' element
     */
    override fun mutationWeight(): Double {
        return 1.0 + elements.map { it.mutationWeight() }.sum()
    }


    /**
     * remove an existing element [element] (key to value) from [elements]
     */
    fun removeExistingElement(element: PairGene<K, V>){
        //this is a reference heap check, not based on `equalsTo`
        if (elements.contains(element)){
            killChild(element)
            element.removeThisFromItsBindingGenes()
        }else{
            log.warn("the specified element (${if (element.isPrintable()) element.getValueAsPrintableString() else "not printable"})) does not exist in this map")
        }
    }

    /**
     * add [element] (key to value) to [elements],
     * if the key of [element] exists in [elements],
     * we replace the existing one with [element]
     * @return whether a new element is added
     */
    fun addElement(element: PairGene<K, V>) {
        checkConstraintsForAdd()
        getElementsBy(element).forEach { e->
            removeExistingElement(e)
        }
        addChild(element)
    }

    /**
     * add [element] to [elements]
     * @return if the element is added successfully
     */
    fun addElement(element: Gene) : Boolean{
        element as? PairGene<K, V> ?:return false
        checkConstraintsForAdd()

        getElementsBy(element).forEach { e->
            removeExistingElement(e)
        }
        addChild(element)
        return true
    }

    /**
     * @return all elements
     */
    fun getAllElements() = elements

    /**
     * @return whether the key of [pairGene] exists in [elements] of this map
     */
    fun containsKey(pairGene: PairGene<K, V>): Boolean{
        return getElementsBy(pairGene).isNotEmpty()
    }

    /**
     * @return a list of elements from [elements] which has the same key with [pairGene]
     */
    private fun getElementsBy(pairGene: PairGene<K, V>): List<PairGene<K, V>>{
        val geneValue = ParamUtil.getValueGene(pairGene.first)
        /*
            currently we only support Integer, String, LongGene, Enum
            TODO support other types if needed
         */
        if (geneValue is IntegerGene || geneValue is StringGene || geneValue is LongGene || geneValue is EnumGene<*>){
            return elements.filter { ParamUtil.getValueGene(it.first).containsSameValueAs(geneValue) }
        }
        return listOf()
    }

    private fun sampleRandomElementToAdd(randomness: Randomness, forceNewValue: Boolean) : PairGene<K, V> {
        val keyName = "key_${keyCounter++}"

        val gene = template.copy() as PairGene<K, V>
        gene.randomize(randomness, forceNewValue)

        /*
            the key of template is tainted value, we force the key of newly created element is tainted as well
         */
        if (template.first is StringGene && TaintInputName.isTaintInput(template.first.getPossiblyTaintedValue())){
            (gene.first as StringGene).forceTaintedValue()
        }

        gene.name = keyName
        gene.first.name = keyName
        gene.second.name = keyName

        if (containsKey(gene)){
            // try one more time
            gene.first.randomize(randomness, true)
        }

        gene.markAllAsInitialized()
        return gene
    }

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