package org.evomaster.core.search.gene.collection

import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.Lazy
import org.evomaster.core.StaticCounter
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.interfaces.TaintableGene
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.gene.optional.FlexibleGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.LoggerFactory

/**
 * Representing a tainted map, ie, a Map object was fields and their type is discovered at runtime through
 * taint analysis.
 */
class TaintedMapGene(
    name: String,
    taintId : String,
    elements: MutableList<PairGene<StringGene, Gene>> = mutableListOf(),
    private val learnedKeys: MutableSet<String> = mutableSetOf(),
    private val learnedTypes: MutableMap<String,String> = mutableMapOf()
) : TaintableGene, MapGene<StringGene, Gene>(
    name,
    PairGene("TaintedMapTemplate", StringGene("key"), FlexibleGene("value", StringGene("value"), null), false),
    elements = elements
){

    var taintId: String = taintId
        private set

    companion object{
        private val log = LoggerFactory.getLogger(TaintedMapGene::class.java)
    }

    init {
        if(elements.isEmpty()){
            val key = StringGene(TaintInputName.TAINTED_MAP_EM_LABEL_IDENTIFIER,
                value = TaintInputName.TAINTED_MAP_EM_LABEL_IDENTIFIER)
            key.doInitialize() // do not want to get its value modified during map initialization

            val value = CustomMutationRateGene(
                TaintInputName.TAINTED_MAP_EM_LABEL_IDENTIFIER,
                StringGene(TaintInputName.TAINTED_MAP_EM_LABEL_IDENTIFIER, taintId),
                0.0)

            val idGene = PairGene(
                TaintInputName.TAINTED_MAP_EM_LABEL_IDENTIFIER,
                key,
                value,
                false
            )
            //always having at least 1 element to identify the map as tainted
            addElement(idGene)

        } else if(elements.none { it.name == TaintInputName.TAINTED_MAP_EM_LABEL_IDENTIFIER }){
            throw IllegalArgumentException("No taint id gene in input")
        }

        val alreadyKnown = learnedKeys.filter { hasKeyByName(it) }
        if(alreadyKnown.isNotEmpty()){
            throw IllegalArgumentException("Already known keys: ${alreadyKnown.joinToString(", ")}")
        }
        val missingKeys = learnedTypes.keys.filter { !hasKeyByName(it) }
        if(missingKeys.isNotEmpty()){
            throw IllegalArgumentException("Missing keys for defined types: ${missingKeys.joinToString(", ")}")
        }
    }

    fun evolve(){
        learnedKeys.forEach { addNewKey(it) }
        learnedKeys.clear()
        learnedTypes.entries.forEach { specifyValueTypeForKey(it.key, it.value) }
        learnedTypes.clear()
    }

    fun registerKey(key: String){
        learnedKeys.add(key)
    }

    /**
     * Create and add a new key-value pair.
     * The value will be a tainted string, so that at next step of the search
     * we can infer its type based on how it is cast
     */
    private fun addNewKey(key: String) {
        if(hasKeyByName(key)){
            throw IllegalArgumentException("Key with name $key already exists")
        }

        val keyGene = StringGene(key, key)
        val valueGene = FlexibleGene(key, StringGene(key).apply { forceTaintedValue() }, null)
        val element = PairGene(key, keyGene, valueGene, false).apply { doInitialize() }

        Lazy.assert { element.isLocallyValid() }

        addElement(element)
    }

    fun registerNewType(key: String, valueType: String){
        learnedTypes[key] = valueType
    }

    /**
     * for the given [key], replace its value gene with a new valid gene with given [valueType].
     *
     * @param valueType in same format as what used in CHECKCAST
     */
    private fun specifyValueTypeForKey(key: String, valueType: String){
        if(!hasKeyByName(key)){
            throw IllegalArgumentException("Key with name $key does not exist")
        }
        if(key == TaintInputName.TAINTED_MAP_EM_LABEL_IDENTIFIER){
            throw IllegalArgumentException("Cannot specify value type for tainted id key with name $key")
        }

        val gene = inferGeneFromValueType(valueType, key)
        if(gene is StringGene){
            //nothing to do, already a string
            return
        }
        gene.doInitialize(getSearchGlobalState()?.randomness)

        val entry = elements.first { it.name == key }
        (entry.second as FlexibleGene).replaceGeneTo(gene)
    }


    private fun inferGeneFromValueType(valueType: String, name: String) : Gene{

        val gene = GeneUtils.getBasicGeneBasedOnBytecodeType(valueType, name)
        if(gene == null){
            log.warn("Cannot handle $valueType in tainted map $taintId for field $name")
            return StringGene(name)
        }
        return gene
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean {

        //among the pairs key-value, there must be only one related to taint id
        val taints = elements.filter { it.name == TaintInputName.TAINTED_MAP_EM_LABEL_IDENTIFIER }
        if(taints.size != 1){
            return false
        }

        // the key must have a specific name that must never be changed
        val key = taints[0].first
        if(key.name != TaintInputName.TAINTED_MAP_EM_LABEL_IDENTIFIER
            || key.value != TaintInputName.TAINTED_MAP_EM_LABEL_IDENTIFIER){
            return false
        }

        //the "value" element of the key-value pair must be equal to taint id
        val x = taints[0].second.getWrappedGene(StringGene::class.java)?.value
            ?: return false
        if(x != taintId){
            return false
        }

        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        /*
            Adding and removing keys in the map is done ONLY with taint analysis
         */
        getViewOfChildren()
            .filter { it.isMutable() }
            .forEach { it.randomize(randomness, tryToForceNewValue) }
    }

    override fun adaptiveSelectSubsetToMutate(
        randomness: Randomness,
        internalGenes: List<Gene>,
        mwc: MutationWeightControl,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo
    ): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        return super.adaptiveSelectSubsetToMutate(randomness, internalGenes, mwc, additionalGeneMutationInfo)
    }

    override fun isMutable(): Boolean {
        return getViewOfChildren().any { it.isMutable() }
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        //there is no internal state to mutate besides the children
        return false
    }

    override fun shallowMutate(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        mwc: MutationWeightControl,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }



    override fun copyContent(): Gene {
        return TaintedMapGene(
            name,
            taintId,
            elements.map { it.copy() as PairGene<StringGene, Gene>}.toMutableList(),
            learnedKeys.toMutableSet(),
            learnedTypes.toMutableMap()
        )
    }

    override fun copyValueFrom(other: Gene): Boolean {
        //TODO

        return false
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        //TODO

        return false
    }

    override fun setValueBasedOn(gene: Gene): Boolean {
        //TODO

        return false
    }

    override fun getPossiblyTaintedValue(): String {
        return taintId
    }

    override fun hasDormantGenes(): Boolean {
        return learnedKeys.isNotEmpty() || learnedTypes.isNotEmpty()
    }

    override fun forceNewTaintId() {

        taintId = TaintInputName.getTaintName(StaticCounter.getAndIncrease())

        val idGene = elements.first { it.name == TaintInputName.TAINTED_MAP_EM_LABEL_IDENTIFIER }
        idGene.second.getWrappedGene(StringGene::class.java)?.value = taintId
    }


}