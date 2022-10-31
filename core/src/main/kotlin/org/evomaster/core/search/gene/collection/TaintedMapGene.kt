package org.evomaster.core.search.gene.collection

import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.interfaces.TaintableGene
import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy

class TaintedMapGene (

    name: String,

    var taintedValue : String,

    var isActive : Boolean = false,

    mapGene:  MapGene<*, *>? = null

) : TaintableGene, CompositeGene(name, if(mapGene == null) mutableListOf() else mutableListOf(mapGene)) {

    init {
        if(! TaintInputName.isTaintInput(taintedValue)){
            throw IllegalArgumentException("Invalid taint: $taintedValue")
        }
    }

    private val mapGene : MapGene<*, *>?
        get(){ return if(children.isNotEmpty()) children[0] as MapGene<*, *> else null}

    fun isResolved() = mapGene != null

    fun resolveTaint(gene: MapGene<*, *>) {
        if(isResolved()){
            throw IllegalArgumentException("TaintedMap '$name' has already been resolved")
        }
        addChild(gene.copy())
    }

    fun activate(){
        if(!isResolved()){
            throw IllegalStateException("Cannot activate not resolved tainted map gene")
        }
        isActive = true
    }

    override fun copyContent(): Gene {
        return TaintedMapGene(name, taintedValue, isActive, mapGene?.copy() as MapGene<*, *>? )
    }

    override fun isLocallyValid(): Boolean {
        return TaintInputName.isTaintInput(taintedValue) && (mapGene == null || mapGene!!.isLocallyValid())
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        mapGene?.randomize(randomness,tryToForceNewValue)
    }

    override fun isMutable(): Boolean {
        return mapGene!=null && mapGene!!.isMutable()
    }

    override fun isPrintable(): Boolean {
        return mapGene == null || !isActive || mapGene!!.isPrintable()
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        if(mapGene == null || !isActive) {
            return "[\"$taintedValue\"]"
        }
        return mapGene!!.getValueAsPrintableString(previousGenes,mode,targetFormat,extraCheck)
    }

    override fun copyValueFrom(other: Gene) {
        if(other !is TaintedMapGene){
            throw IllegalArgumentException("Other is not a TaintedMap: ${other::class.java}")
        }
        this.taintedValue = other.taintedValue
        this.isActive = other.isActive
        this.mapGene?.copyValueFrom(other.mapGene!!)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is TaintedMapGene){
            throw IllegalArgumentException("Other is not a TaintedMap: ${other::class.java}")
        }

        if(this.isActive != other.isActive){
            return  false
        }

        if(mapGene != null) {
            if(other.mapGene == null){
                return false
            }
            return mapGene!!.containsSameValueAs(other.mapGene!!)
        }

        if(other.mapGene != null){
            return false
        }

        return this.taintedValue == other.taintedValue
    }

    override fun bindValueBasedOn(gene: Gene): Boolean {
        if(gene !is TaintedMapGene){
            throw IllegalArgumentException("Other is not a TaintedMap: ${gene::class.java}")
        }

        if(mapGene != null && gene.mapGene != null){
            return mapGene!!.bindValueBasedOn(gene.mapGene!!)
        }

        return false
    }

    override fun getPossiblyTaintedValue(): String {
        if(isResolved()){
            return ""
        }
        return taintedValue
    }

    fun isStringMap() = isResolved() && MapGene.isStringMap(mapGene!!)

}