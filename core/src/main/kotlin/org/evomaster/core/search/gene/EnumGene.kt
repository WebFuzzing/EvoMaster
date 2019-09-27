package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.IntMutationUpdate


class EnumGene<T>(
        name: String,
        val values: List<T>,
        var index: Int = 0,
        val optionMutationUpdate : IntMutationUpdate = IntMutationUpdate(0, values.size -1)
) : Gene(name), GeneIndependenceInfo {

    init {
        if (values.isEmpty()) {
            throw IllegalArgumentException("Empty list of values")
        }
        if (index < 0 || index >= values.size) {
            throw IllegalArgumentException("Invalid index: $index")
        }
    }

    override fun isMutable(): Boolean {
        return values.size > 1
    }

    override fun copy(): Gene {
        //recall: "values" is immutable
        val copy = EnumGene<T>(name, values, index, optionMutationUpdate.copy()).also {
            it.degreeOfIndependence = this.degreeOfIndependence
            it.resetTimes = this.resetTimes
            it.mutatedtimes = this .mutatedtimes
        }
        return copy
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        val k = if (forceNewValue) {
            randomness.nextInt(0, values.size - 1, index)
        } else {
            randomness.nextInt(0, values.size - 1)
        }

        index = k
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {

        val next = (index+1) % values.size
        index = next
    }

    //override

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {

        val res = values[index]
        if(res is String){
            return "\"$res\""
        } else {
            return res.toString()
        }
    }

    override fun getValueAsRawString(): String {
        return values[index].toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is EnumGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.index = other.index
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is EnumGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.index == other.index
    }

    override fun reachOptimal(): Boolean {
        return optionMutationUpdate.reached
    }
}