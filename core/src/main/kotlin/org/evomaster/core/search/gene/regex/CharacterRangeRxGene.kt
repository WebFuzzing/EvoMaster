package org.evomaster.core.search.gene.regex

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.LoggerFactory
import kotlin.math.max
import kotlin.math.min


class CharacterRangeRxGene(
        val negated: Boolean,
        ranges: List<Pair<Char,Char>>
) : RxAtom, SimpleGene("."){

    companion object{
        private val log = LoggerFactory.getLogger(CharacterRangeRxGene::class.java)
    }

    init {
        //TODO this will need to be supported
        if(negated){
            throw IllegalArgumentException("Negated ranges are not supported yet")
        }

        if(ranges.isEmpty()){
            throw IllegalArgumentException("No defined ranges")
        }

        ranges.forEach {
            if(it.first.code > it.second.code){
                LoggingUtil.uniqueWarn(log, "Issue with Regex range, where '${it.first}' is greater than '${it.second}'")
            }
        }
    }

    var value : Char = ranges[0].first

    /**
     * As inputs might be unsorted, we make sure first <= second
     */
    val ranges = ranges.map { Pair(min(it.first.code,it.second.code).toChar(), max(it.first.code, it.second.code).toChar()) }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        //TODO negated
        return ranges.any { value.code >= it.first.code && value.code <= it.second.code }
    }

    override fun isMutable(): Boolean {
        return ranges.size > 1 || ranges[0].let { it.first != it.second }
    }

    override fun copyContent(): Gene {
        val copy = CharacterRangeRxGene(negated, ranges)
        copy.value = this.value
        return copy
    }

    override fun setValueWithRawString(value: String) {
        // need to check
        val c = value.toCharArray().firstOrNull()
        if (c!= null)
            this.value = c
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        /*
            TODO current is very simple, biased implementation.
            Should rather have uniform sampling among all valid chars
         */
        val range = randomness.choose(ranges)

        value = randomness.nextChar(range.first, range.second)
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {

        var t = 0
        for(i in 0 until ranges.size){
            val p = ranges[i]
            if(value >= p.first && value <= p.second){
                t = i
                break
            }
        }

        val delta = randomness.choose(listOf(1,-1))

        if(value + delta > ranges[t].second){
            /*
                going over current max range. check next range
                and take its minimum
             */
            val next = (t+1) % ranges.size
            value = ranges[next].first

        } else if(value + delta < ranges[t].first){

            val previous = (t - 1 + ranges.size) % ranges.size
            value = ranges[previous].second

        } else {
            value += delta
        }

        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        /*
            TODO should \ be handled specially?
            In any case, would have same handling as AnyCharacterRxGene
         */
        return value.toString()
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if(other !is CharacterRangeRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        val current = this.value
        this.value = other.value
        if (!isLocallyValid()){
            this.value = current
            return false
        }

        return true
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is CharacterRangeRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        if(gene is CharacterRangeRxGene){
            value = gene.value
            return true
        }
        LoggingUtil.uniqueWarn(log,"cannot bind CharacterClassEscapeRxGene with ${gene::class.java.simpleName}")

        return false
    }
}