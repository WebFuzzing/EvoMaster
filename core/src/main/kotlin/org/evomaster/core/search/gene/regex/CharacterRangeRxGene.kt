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

data class CharacterRange(val start: Char, val end: Char){
    val size: Int
        get() = end.code - start.code + 1
    operator fun contains(char: Char): Boolean = char in start..end
}

class CharacterRangeRxGene(
    val negated: Boolean,
    val ranges: List<CharacterRange>
) : RxAtom, SimpleGene("."){

    companion object{
        private val log = LoggerFactory.getLogger(CharacterRangeRxGene::class.java)
    }

    private var internalRanges = mutableListOf<CharacterRange>()

    init {
        if(ranges.isEmpty()){
            throw IllegalArgumentException("No defined ranges")
        }

        // this limits the character class complements to 0xffff instead of allowing up to 0x10ffff, but values over
        // 0xffff are not permitted on Char as they need 2 Chars to be represented; to allow this, we would need to
        // use String or Int in every possible step as methods which return a single Char cannot return these characters
        if(negated) internalRanges.add(CharacterRange(Character.MIN_VALUE,Character.MAX_VALUE))
        for (range in ranges) {
            val max = maxOf(range.start, range.end)
            val min = minOf(range.start, range.end)
            if(negated){
                remove(CharacterRange(min, max))
            } else {
                add(CharacterRange(min, max))
            }
        }

        ranges.forEach {
            if(it.start.code > it.end.code){
                LoggingUtil.uniqueWarn(log, "Issue with Regex range, where '${it.start}' is greater than '${it.end}'")
            }
        }
    }

    var value : Char = internalRanges[0].start

    /**
     * Adds a character range to a [org.evomaster.core.search.gene.regex.CharacterRangeRxGene].
     *
     * The range is added to the character class in a way that does not generate repeated elements.
     *
     * @param toAdd The character range to be added to the character class.
     */
    private fun add(toAdd: CharacterRange) {
        val newInternalRanges = mutableListOf<CharacterRange>()
        var currentStart = toAdd.start
        var currentEnd = toAdd.end
        var merged = false

        for ((start, end) in internalRanges.sortedBy { it.start }){
            when {
                end < currentStart - 1 -> newInternalRanges += CharacterRange(start, end)
                start > currentEnd + 1 -> {
                    if (!merged) {
                        newInternalRanges += CharacterRange(currentStart, currentEnd)
                        merged = true
                    }
                    newInternalRanges += CharacterRange(start, end)
                }
                else -> {
                    currentStart = minOf(currentStart, start)
                    currentEnd = maxOf(currentEnd, end)
                }
            }
        }

        if (!merged) {
            newInternalRanges += CharacterRange(currentStart, currentEnd)
        }

        internalRanges = newInternalRanges
    }

    /**
     * Safely removes a character range from a [org.evomaster.core.search.gene.regex.CharacterRangeRxGene].
     *
     * @param toRemove The character range to be removed from the character class.
     */
    private fun remove(toRemove: CharacterRange) {
        internalRanges = internalRanges.flatMap { r ->
            when {
                toRemove.end < r.start || toRemove.start > r.end ->
                    listOf(r)
                else -> buildList {
                    if (toRemove.start > r.start) add(CharacterRange(r.start, toRemove.start - 1))
                    if (toRemove.end < r.end) add(CharacterRange(toRemove.end + 1, r.end))
                }
            }
        }.toMutableList()
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return internalRanges.any { value in it }
    }

    override fun isMutable(): Boolean {
        return internalRanges.size > 1 || internalRanges[0].let { it.start != it.end }
    }

    override fun copyContent(): Gene {
        val copy = CharacterRangeRxGene(negated, ranges)
        copy.value = this.value
        copy.name = this.name //in case name is changed from its default
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
        val total = internalRanges.sumOf { it.size }
        val sampledValue = randomness.nextInt(total)
        var currentRangeMinValue = 0
        for (r in internalRanges) {
            val currentRangeMaxValue = currentRangeMinValue + r.size
            if (sampledValue < currentRangeMaxValue) {
                val codePoint = r.start.code + (sampledValue - currentRangeMinValue)
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
            if(value in p){
                t = i
                break
            }
        }

        val delta = randomness.choose(listOf(1,-1))

        if(value + delta > internalRanges[t].end){
            /*
                going over current max range. check next range
                and take its minimum
             */
            val next = (t+1) % internalRanges.size
            value = internalRanges[next].start

        } else if(value + delta < internalRanges[t].start){

            val previous = (t - 1 + internalRanges.size) % internalRanges.size
            value = internalRanges[previous].end

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


    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is CharacterRangeRxGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {

        val gene = other.getPhenotype()

        if(gene is CharacterRangeRxGene){
            value = gene.value
            return true
        }
        LoggingUtil.uniqueWarn(log,"cannot bind CharacterClassEscapeRxGene with ${gene::class.java.simpleName}")

        return false
    }
}
