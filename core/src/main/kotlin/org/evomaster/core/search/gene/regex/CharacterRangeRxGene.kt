package org.evomaster.core.search.gene.regex

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.LoggerFactory


class CharacterRangeRxGene(
        val negated: Boolean,
        val ranges: List<Pair<Char,Char>>
) : RxAtom(".", listOf()){

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
    }

    var value : Char = ranges[0].first

    override fun getChildren(): List<Gene> = listOf()

    override fun isMutable(): Boolean {
        return ranges.size > 1 || ranges[0].let { it.first != it.second }
    }

    override fun copyContent(): Gene {
        val copy = CharacterRangeRxGene(negated, ranges)
        copy.value = this.value
        return copy
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        /*
            TODO current is very simple, biased implementation.
            Should rather have uniform sampling among all valid chars
         */
        val range = randomness.choose(ranges)

        value = randomness.nextChar(range.first, range.second)
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {

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

    override fun copyValueFrom(other: Gene) {
        if(other !is CharacterRangeRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is CharacterRangeRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }

    override fun innerGene(): List<Gene> = listOf()

    override fun bindValueBasedOn(gene: Gene): Boolean {
        if(gene is CharacterRangeRxGene){
            value = gene.value
            return true
        }
        LoggingUtil.uniqueWarn(log,"cannot bind CharacterClassEscapeRxGene with ${gene::class.java.simpleName}")

        return false
    }
}