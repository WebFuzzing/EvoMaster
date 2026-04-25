package org.evomaster.core.utils

import org.evomaster.core.search.service.Randomness
import org.slf4j.LoggerFactory

class MultiCharacterRange internal constructor(val ranges: List<CharacterRange>) {

    companion object {
        private val log = LoggerFactory.getLogger(MultiCharacterRange::class.java)

        operator fun invoke(negated: Boolean, ranges: List<CharacterRange>): MultiCharacterRange {
            if (ranges.isEmpty()) {
                throw IllegalArgumentException("No defined ranges")
            }

            var internalRanges = mutableListOf<CharacterRange>()

            if (negated) {
                internalRanges.add(CharacterRange(Character.MIN_VALUE, Character.MAX_VALUE))
            }
            for (range in ranges) {
                internalRanges = if (negated) {
                    remove(internalRanges, CharacterRange(range.start, range.end))
                } else {
                    add(internalRanges, CharacterRange(range.start, range.end))
                }
            }

            if (internalRanges.isEmpty()) {
                throw IllegalArgumentException("No defined ranges")
            }

            return MultiCharacterRange(internalRanges)
        }

        /**
         * Adds a character range to the given list of ranges without generating overlaps.
         *
         * @param internalRanges The current list of character ranges.
         * @param toAdd The character range to add.
         * @return A new list of character ranges with [toAdd] merged in.
         */
        private fun add(internalRanges: MutableList<CharacterRange>, toAdd: CharacterRange): MutableList<CharacterRange> {
            val newInternalRanges = mutableListOf<CharacterRange>()
            var currentStart = toAdd.start
            var currentEnd = toAdd.end
            var merged = false

            for ((start, end) in internalRanges.sortedBy { it.start }) {
                when {
                    end.code < currentStart.code - 1 -> newInternalRanges += CharacterRange(start, end)
                    start.code > currentEnd.code + 1 -> {
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

            return newInternalRanges
        }

        /**
         * Removes a character range from the given list of ranges, splitting existing ranges if necessary.
         *
         * @param internalRanges The current list of character ranges.
         * @param toRemove The character range to remove.
         * @return A new list of character ranges with [toRemove] excluded.
         */
        private fun remove(internalRanges: MutableList<CharacterRange>, toRemove: CharacterRange): MutableList<CharacterRange> {
            return internalRanges.flatMap { r ->
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
    }

    /**
     * Uniformly samples a random character from the valid characters in a MultiCharacterRange.
     *
     * @param randomness The randomness source used to perform the uniform sampling.
     * @return The sampled character.
     */
    fun sample(randomness: Randomness): Char {
        val total = ranges.sumOf { it.size }
        val sampledValue = randomness.nextInt(total)
        var currentRangeMinValue = 0
        for (r in ranges) {
            val currentRangeMaxValue = currentRangeMinValue + r.size
            if (sampledValue < currentRangeMaxValue) {
                val codePoint = r.start.code + (sampledValue - currentRangeMinValue)
                // is it necessary to log this?
                log.trace("using Int {} as character selector for character class, resulting in code point: {}, which is: {}", sampledValue, codePoint, codePoint.toChar())
                return codePoint.toChar()
            }
            currentRangeMinValue = currentRangeMaxValue
        }
        assert(false) // internal ranges being empty should never happen
        return '0'
    }

    val size: Int get() = ranges.size
    operator fun get(index: Int): CharacterRange = ranges[index]
    fun any(predicate: (CharacterRange) -> Boolean): Boolean = ranges.any(predicate)
}