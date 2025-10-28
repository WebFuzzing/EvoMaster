package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.utils.NumberCalculationUtil
import org.evomaster.core.utils.NumberCalculationUtil.calculateIncrement
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.annotation.PostConstruct


class Randomness {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Randomness::class.java)
    }

    @Inject
    private lateinit var configuration: EMConfig

    private val random = Random()

    init {
        /*
            this is needed just for EM tests. during EM execution, it is taken
            from the seed in EMConfig
         */
        updateSeed(42)
    }

    @PostConstruct
    private fun initialize() {
        updateSeed(configuration.seed)
    }

    private val digitSet = "0123456789"
    private val asciiLetterSet = "abcdefghilmnopqrstuvzjkwxyABCDEFGHILMNOPQRSTUVZJKWXY"

    private val wordSet = "_$digitSet$asciiLetterSet"
    private val spaceSet = " \t\r\n\u000C\u000b"
    private val verticalSpaceSet = "\n\u000B\u000C\r\u0085\u2028\u2029"
    private val horizontalSpaceSet = " \t\u00A0\u1680\u180e\u2000\u2001\u2002\u2003" +
            "\u2004\u2005\u2006\u2007\u2008\u2009\u200a\u202f\u205f\u3000"
    private val punctuationSet = "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}~"

    // TODO this is neither efficient nor complete; this will be modified
    // does not actually include all characters but covers ASCII
    private val allSet = (0x00..0xFF).map { it.toChar() }.joinToString("")

    // used to complement sets as they may have repeated characters
    private fun complementSet(base: String, remove: String): String =
        base.filterNot { it in remove }

    private val nonWordSet = complementSet(allSet, wordSet)
    private val nonDigitSet = complementSet(allSet, digitSet)
    private val nonSpaceSet = complementSet(allSet, spaceSet)
    private val nonVerticalSpaceSet = complementSet(allSet, verticalSpaceSet)
    private val nonHorizontalSpaceSet = complementSet(allSet, horizontalSpaceSet)

    private val posixCharClassSet = mapOf(
        "Lower" to ('a'..'z').joinToString(""),
        "Upper" to ('A'..'Z').joinToString(""),
        "ASCII" to (0x00..0x7F).map { it.toChar() }.joinToString(""),
        "Alpha" to asciiLetterSet,
        "Digit" to digitSet,
        "Alnum" to "$digitSet$asciiLetterSet",
        "Punct" to punctuationSet,
        "Graph" to "$digitSet$asciiLetterSet$punctuationSet",
        "Print" to "$digitSet$asciiLetterSet$punctuationSet\u0020",
        "Blank" to " \t",
        "Cntrl" to (0x00..0x1F).map { it.toChar() }.joinToString("") + 0x7F.toChar(),
        "XDigit" to "0123456789abcdefABCDEF",
        "Space" to spaceSet
    )

    private val wordChars = wordSet.map { it.toInt() }.sorted()

    /**
     * A negative value means the current CPU time clock is used instead
     */
    fun updateSeed(seed: Long) {
        if (seed < 0) {
            random.setSeed(System.currentTimeMillis())
        } else {
            random.setSeed(seed)
        }
    }

    fun nextBoolean(): Boolean {
        val k = random.nextBoolean()
        log.trace("nextBoolean(): {}", k)
        return k
    }

    /**
     * Return true with probability P
     */
    fun nextBoolean(p: Double): Boolean {
        /*
            Do not call methods of 'random' if already know the answer.
            Problem example: adding a check on experimental EMConfig probability with default 0.0
            would otherwise change the sequence of random values, possibly side-effecting
            not-too-stable E2E tests unrelated to the pushed changes
         */
        if(p == 0.0){
            return false
        }
        if(p == 1.0){
            return true
        }
        val k = random.nextDouble() < p
        log.trace("nextBoolean(): {}", k)
        return k
    }

    fun nextInt(): Int {
        val k = random.nextInt()
        log.trace("nextInt(): {}", k)
        return k
    }

    fun nextDouble(): Double {
        val k = random.nextDouble()
        log.trace("nextDouble(): {}", k)
        return k
    }

    fun nextDouble(min: Double, max: Double, exclude: Double): Double{
        if (min == max && max == exclude) {
            throw IllegalArgumentException("Nothing to select, as min/max/exclude are all equal")
        }

        var k = nextDouble(min, max)
        while (k == exclude) {
            k = nextDouble(min, max)
        }
        log.trace("nextDouble(min, max, exclude): {}", k)
        return k
    }

    fun nextDouble(min: Double, max: Double): Double{
        if (min == max) return min
        if (min > max) {
            throw IllegalArgumentException("Min $min is bigger than max $max")
        }

        val k = min + random.nextDouble() * calculateIncrement(min, max)

        log.trace("nextDouble(min {}, max {}): {}", min, max, k)
        return k
    }

    fun nextGaussian(): Double {
        val k = random.nextGaussian()
        log.trace("nextGaussian(): {}", k)
        return k
    }

    fun nextFloat(): Float {
        val k = random.nextFloat()
        log.trace("nextFloat(): {}", k)
        return k
    }

    fun nextInt(bound: Int): Int {
        val k = random.nextInt(bound)
        log.trace("nextInt(bound): {} , {}", k, bound)
        return k
    }


    fun nextInt(min: Int, max: Int, exclude: Int): Int {

        if (min == max && max == exclude) {
            throw IllegalArgumentException("Nothing to select, as min/max/exclude are all equal")
        }

        var k = nextInt(min, max)
        while (k == exclude) {
            k = nextInt(min, max)
        }
        log.trace("nextInt(min,max,exclude): {}", k)
        return k
    }

    /**
     * A random int between "min" and "max", both inclusive
     */
    fun nextInt(min: Int, max: Int): Int {
        if (min == max) {
            return min
        }
        if (min > max) {
            throw IllegalArgumentException("Min $min is bigger than max $max")
        }

        val k = (min.toLong() + random.nextDouble() * (max.toLong() - min + 1)).toInt()
        log.trace("nextInt(min {}, max {}): {}", min, max, k)
        return k
    }


    fun nextLong(): Long {
        val k = random.nextLong()
        log.trace("nextLong(): {}", k)
        return k
    }





    fun nextLong(min: Long, max: Long): Long {

        if (min == max) {
            return min
        }
        if (min > max) {
            throw IllegalArgumentException("Min $min is bigger than max $max")
        }

        val k = min + (random.nextDouble() * calculateIncrement(min, max, minIncrement = 1L)).toLong()

        log.trace("nextLong(min {}, max {}): {}", min, max, k)
        return k
    }

    fun nextLong(min: Long, max: Long, exclude: Long): Long {

        if (min == max && max == exclude) {
            throw IllegalArgumentException("Nothing to select, as min/max/exclude are all equal")
        }

        var k = nextLong(min, max)
        while (k == exclude) {
            k = nextLong(min, max)
        }
        log.trace("nextLong(min, max, exclude): {}", k)
        return k
    }

    fun nextWordString(min: Int = 0, max: Int = 10): String {

        val n = nextInt(min, max)

        val chars = CharArray(n)
        (0 until n).forEach {
            chars[it] = nextWordChar()
        }

        val k = String(chars)
        log.trace("nextWordString(): {}", k)
        return k
    }

    fun randomizeBoundedIntAndLong(value: Long, min: Long, max: Long, forceNewValue: Boolean) : Long{

        if (min == max) return min

        val z = 1000L
        val range = calculateIncrement(min, max, 1L)

        val a: Long
        val b: Long

        if (range > z && nextBoolean(0.95)) {
            //if very large range, might want to sample small values around 0 most of the times
            if (min <= 0 && max >= z) {
                a = 0
                b = z
            } else if (nextBoolean()) {
                a = min
                b = min + z
            } else {
                a = max - z
                b = max
            }
        } else {
            a = min
            b = max
        }


        return if (forceNewValue) {
            nextLong(a, b, value)
        } else {
            nextLong(a, b)
        }
    }

    fun nextLetter(): Char {

        val characters = asciiLetterSet

        val k = characters[random.nextInt(characters.length)]
        log.trace("nextLetter(): {}", k)
        return k
    }

    fun nextFromStringSet(set: String) : Char{
        return set[random.nextInt(set.length)]
    }

    fun nextWordChar(): Char {
        val k = nextFromStringSet(wordSet)
        log.trace("nextWordChar(): {}", k)
        return k
    }

    fun nextNonWordChar() : Char{
        val k = nextFromStringSet(nonWordSet)
        log.trace("nextNonWordChar(): {}", k)
        return k
    }

    fun nextDigitChar(): Char {
        val k = nextFromStringSet(digitSet)
        log.trace("nextDigitChar(): {}", k)
        return k
    }

    fun nextNonDigitChar(): Char {
        val k = nextFromStringSet(nonDigitSet)
        log.trace("nextNonDigitChar(): {}", k)
        return k
    }

    fun nextSpaceChar(): Char {
        val k = nextFromStringSet(spaceSet)
        log.trace("nextSpaceChar(): {}", k)
        return k
    }

    fun nextNonSpaceChar(): Char {
        val k = nextFromStringSet(nonSpaceSet)
        log.trace("nextNonSpaceChar(): {}", k)
        return k
    }

    fun nextVerticalSpaceChar(): Char {
        val k = nextFromStringSet(verticalSpaceSet)
        log.trace("nextVerticalSpaceChar(): {}", k)
        return k
    }

    fun nextNonVerticalSpaceChar(): Char {
        val k = nextFromStringSet(nonVerticalSpaceSet)
        log.trace("nextNonVerticalSpaceChar(): {}", k)
        return k
    }

    fun nextHorizontalSpaceChar(): Char {
        val k = nextFromStringSet(horizontalSpaceSet)
        log.trace("nextHorizontalSpaceChar(): {}", k)
        return k
    }

    fun nextNonHorizontalSpaceChar(): Char {
        val k = nextFromStringSet(nonHorizontalSpaceSet)
        log.trace("nextNonHorizontalSpaceChar(): {}", k)
        return k
    }

    fun nextPosixCharClassChar(type: String): Char {
        if (type.substring(2,type.length-1) !in posixCharClassSet){
            throw IllegalArgumentException("$type invalid/unsupported POSIX character class")
        }
        val k = nextFromStringSet(posixCharClassSet[type.substring(2,type.length-1)]!!)
        log.trace("nextPosixCharClassChar({}): {}", type, k)
        return k
    }

    fun wordCharPool() = wordChars

    fun validNextWordChars(min: Int, max: Int): List<Int> {
        return wordChars.filter { it in min..max }
    }

    fun nextChar(min: Int = Char.MIN_VALUE.toInt(), max: Int = Char.MAX_VALUE.toInt()): Char {
        if (min < Char.MIN_VALUE.toInt())
            throw IllegalArgumentException("$min is less than MIN_VALUE of Char")
        if (max > Char.MAX_VALUE.toInt())
            throw IllegalArgumentException("$max is more than MAX_VALUE of Char")
        if (min > max)
            throw IllegalArgumentException("$min should be less than $max")
        if (min == max)
            return min.toChar()
        return choose((min..max).toList()).toChar()
    }

    fun nextChar(start: Char, endInclusive: Char): Char {

        if (start > endInclusive) {
            throw IllegalArgumentException("Start '$start' is after end '$endInclusive'")
        }

        if (start == endInclusive) {
            return start
        }

        return nextInt(start.toInt(), endInclusive.toInt()).toChar()
    }

    /**
     * Choose a key from the [map] based on the associated probabilities.
     * The highest the associated probability, the *more* chances to be selected.
     * If an element [K] is not present in the map, then
     * its probability is 0.
     *
     * Note: as [K] is used as a key, make sure that [equals] and [hashCode]
     * are well defined for it (eg, no problem if it is a [Int] or a [String])
     */
    fun <K> chooseByProbability(map: Map<K, Double>): K {

        val randFl = random.nextDouble() * map.values.sum()
        var temp = 0.0
        var found = map.keys.first()

        for ((k, v) in map) {
            if (randFl <= (v + temp)) {
                found = k
                break
            }
            temp += v
        }

        log.trace("Chosen: {}", found)

        return found
    }

    /**
     * Choose a value from the [list] based on its weight in the [weights] map.
     * The highest the weight, the *less* chances to be selected.
     * If an element [K] is not present in the map [weights], then
     * its weight is 0.
     * Note: as [K] is used as a key, make sure that [equals] and [hashCode]
     * are well defined for it (eg, no problem if it is a [Int] or a [String])
     */
    fun <K, V> chooseProportionally(list: List<K>, weights: Map<K, V>): K {

        throw IllegalStateException("Not implemented yet")
    }

    /**
     * Choose a random element from the [list], up to the element in the index position (exclusive).
     * Ie, consider only the first [index] elements
     */
    fun <T> chooseUpTo(list: List<T>, index: Int): T {
        if (index <= 0 || index > list.size) {
            throw IllegalArgumentException("Invalid index $index in list of size ${list.size}")
        }

        val index = random.nextInt(index)
        return list[index]
    }


    fun choose(range: IntRange) : Int{
        return nextInt(range.first, range.last)
    }

    fun <T> choose(list: List<T>): T {
        if (list.isEmpty()) {
            throw IllegalArgumentException("Empty list to choose from")
        }
        return chooseUpTo(list, list.size)
    }

    /**
     * Randomly choose (without replacement) up to [n] values from the [list]
     */
    fun <T> choose(list: List<T>, n: Int): List<T> {
        if (list.size <= n) {
            return list
        }

        val selection: MutableList<T> = mutableListOf()
        selection.addAll(list)
        selection.shuffle(random)

        val k =  selection.subList(0, n)

        //printing actual values here lead to non-deterministic behavior is toString() is non-deterministic,
        //which is the typical case for custom objects that do not override it, as output string will have
        // a @ reference number to the heap
        log.trace("Chosen {} elements from list", n)

        return k
    }

    /**
     * Randomly choose (without replacement) up to [n] values from the [set]
     */
    fun <T> choose(set: Set<T>, n: Int): Set<T> {
        if (set.size <= n) {
            return set
        }

        val selection: MutableList<T> = mutableListOf()
        selection.addAll(set)
        selection.shuffle(random)

        val k = selection.subList(0, n).toSet()

        log.trace("Chosen {} elements from set", n)

        return k
    }


    fun <K, V> choose(map: Map<K, V>): V = choose(map.values)


    /**
     * Randomly choose one element
     */
    fun <V> choose(collection: Collection<V>): V {
        if (collection.isEmpty()) {
            throw IllegalArgumentException("Empty map to choose from")
        }
        val index = random.nextInt(collection.size)
        var i = 0

        val iter = collection.iterator()
        while (iter.hasNext() && i < index) {
            iter.next()
            i++
        }

        val k = iter.next()
        log.trace("Chosen index: {}", i)

        return k
    }

    fun randomIPBit() : Int {
        val k = random.nextInt( 255)
        log.trace("Random IP bit: {}", k)
        return k
    }

    fun shuffle(list: MutableList<*>) {
        list.shuffle(random)
    }
}