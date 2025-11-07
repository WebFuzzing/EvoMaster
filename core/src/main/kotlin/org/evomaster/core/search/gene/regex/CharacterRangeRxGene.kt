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


class CharacterRangeRxGene(
    val negated: Boolean,
    val ranges: List<Pair<Char, Char>>
) : RxAtom, SimpleGene("."){

    companion object{
        private val log = LoggerFactory.getLogger(CharacterRangeRxGene::class.java)
    }

    private var internalRanges = mutableListOf<Pair<Char, Char>>()

    init {
        if(ranges.isEmpty()){
            throw IllegalArgumentException("No defined ranges")
        }

        // this limits the character class complements to 0xffff instead of allowing up to 0x10ffff, but values over
        // 0xffff are not permitted on Char as they need 2 Chars to be represented; to allow this, we would need to
        // use String or Int in every possible step as methods which return a single Char cannot return these characters
        if(negated) internalRanges.add(Pair(Character.MIN_VALUE,Character.MAX_VALUE))
        for (range in ranges) {
            val max = maxOf(range.first, range.second)
            val min = minOf(range.first, range.second)
            if(negated){
                remove(Pair(min, max))
            } else {
                add(Pair(min, max))
            }
        }

        ranges.forEach {
            if(it.first.code > it.second.code){
                LoggingUtil.uniqueWarn(log, "Issue with Regex range, where '${it.first}' is greater than '${it.second}'")
            }
        }
    }

    var value : Char = internalRanges[0].first

    private fun add(toAdd: Pair<Char, Char>) {
        val newInternalRanges = mutableListOf<Pair<Char, Char>>()
        var currentStart = toAdd.first
        var currentEnd = toAdd.second
        var merged = false

        for ((start, end) in internalRanges.sortedBy { it.first }){
            when {
                end < currentStart - 1 -> newInternalRanges += start to end
                start > currentEnd + 1 -> {
                    if (!merged) {
                        newInternalRanges += currentStart to currentEnd
                        merged = true
                    }
                    newInternalRanges += start to end
                }
                else -> {
                    currentStart = minOf(currentStart, start)
                    currentEnd = maxOf(currentEnd, end)
                }
            }
        }

        if (!merged) {
            newInternalRanges += currentStart to currentEnd
        }

        internalRanges = newInternalRanges
    }

    private fun remove(toRemove: Pair<Char, Char>) {
        internalRanges = internalRanges.flatMap { r ->
            when {
                toRemove.second < r.first || toRemove.first > r.second ->
                    listOf(r)
                else -> buildList {
                    if (toRemove.first > r.first) add(Pair(r.first, toRemove.first - 1))
                    if (toRemove.second < r.second) add(Pair(toRemove.second + 1, r.second))
                }
            }
        }.toMutableList()
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return internalRanges.any { value.code >= it.first.code && value.code <= it.second.code }
    }

    override fun isMutable(): Boolean {
        return internalRanges.size > 1 || internalRanges[0].let { it.first != it.second }
    }

    override fun copyContent(): Gene {
        val copy = CharacterRangeRxGene(negated, ranges)
        copy.value = this.value
        return copy
    }

    override fun setValueWithRawString(value: String) {
        val c = value.toCharArray().firstOrNull()
        if (c!= null){
            val prev = this.value
            this.value = c
            if (!isLocallyValid()) this.value = prev
        }
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        val total = internalRanges.sumOf { it.second.code - it.first.code + 1 }
        val sampledValue = randomness.nextInt(total)
        var currentRangeMinValue = 0
        for (r in internalRanges) {
            val currentRangeMaxValue = currentRangeMinValue + r.second.code - r.first.code + 1
            if (sampledValue < currentRangeMaxValue) {
                val codePoint = r.first.code + (sampledValue - currentRangeMinValue)
                // is it necessary to log this?
                log.trace("using Int {} as character selector for character class, resulting in code point: {}, which is: {}", sampledValue, codePoint, codePoint.toChar())
                value = codePoint.toChar()
                return
            }
            currentRangeMinValue = currentRangeMaxValue
        }
        throw IllegalArgumentException("No defined ranges")
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        var t = 0
        for(i in 0 until internalRanges.size){
            val p = internalRanges[i]
            if(value >= p.first && value <= p.second){
                t = i
                break
            }
        }

        val delta = randomness.choose(listOf(1,-1))

        if(value + delta > internalRanges[t].second){
            /*
                going over current max range. check next range
                and take its minimum
             */
            val next = (t+1) % internalRanges.size
            value = internalRanges[next].first

        } else if(value + delta < internalRanges[t].first){

            val previous = (t - 1 + internalRanges.size) % internalRanges.size
            value = internalRanges[previous].second

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