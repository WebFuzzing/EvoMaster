package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import java.util.*
import javax.annotation.PostConstruct


class Randomness {

    @Inject
    private lateinit var configuration: EMConfig

    private val random = Random()

    @PostConstruct
    private fun initialize(){
        updateSeed(configuration.seed)
    }

    /**
     * A negative value means the current CPU time clock is used instead
     */
    fun updateSeed(seed: Long) {
        if(seed < 0 ){
            random.setSeed(System.currentTimeMillis())
        } else {
            random.setSeed(seed)
        }
    }

    fun nextBoolean() = random.nextBoolean()

    /**
     * Return true with probability P
     */
    fun nextBoolean(p: Double) = random.nextDouble() < p

    fun nextInt() = random.nextInt()

    fun nextDouble() = random.nextDouble()

    fun nextGaussian() = random.nextGaussian()

    fun nextFloat() = random.nextFloat()

    fun nextInt(bound: Int) = random.nextInt(bound)


    fun nextInt(min: Int, max: Int, exclude: Int): Int {

        if(min == max && max == exclude){
            throw IllegalArgumentException("Nothing to select, as min/max/exclude are all equal")
        }

        var k = nextInt(min, max)
        while(k == exclude){
            k = nextInt(min, max)
        }
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

        return (min.toLong() + Math.random() * (max.toLong() - min + 1)).toInt()
    }


    fun nextLong() = random.nextLong()


    fun nextWordString(min: Int = 0, max: Int = 10) : String {

        val n = nextInt(min, max)

        val chars = CharArray(n)
        (0..n-1).forEach {
            chars[it] = nextWordChar()
        }

        return kotlin.text.String(chars)
    }

    fun nextWordChar() : Char{

        val characters =
                "_0123456789abcdefghilmnopqrstuvzjkwxyABCDEFGHILMNOPQRSTUVZJKWXY"
        return characters[random.nextInt(characters.length)]
    }


    /**
     * Choose a value from the [list] based on its weight in the [weights] map.
     * The highest the weight, the *less* chances to be selected.
     * If an element [K] is not present in the map [weights], then
     * its weight is 0.
     * Note: as [K] is used as a key, make sure that [equals] and [hashCode]
     * are well defined for it (eg, no problem if it is a [Int] or a [String])
     */
    fun <K,V> chooseProportionally(list: List<K>, weights: Map<K,V>): K {

        throw IllegalStateException("Not implemented yet")
    }

    /**
     * Choose a random element from the [list], up to the element in the index position (exclusive).
     * Ie, consider only the first [index] elements
     */
    fun <T> chooseUpTo(list: List<T>, index: Int): T {
        if(index <= 0 || index > list.size){
            throw IllegalArgumentException("Invalid index $index in list of size ${list.size}")
        }

        val index = random.nextInt(index)
        return list[index]
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
    fun <T> choose(list: List<T>, n: Int): List<T>{
        if(list.size <= n){
            return list
        }

        val selection: MutableList<T> = mutableListOf()
        selection.addAll(list)
        Collections.shuffle(selection, random)

        return selection.subList(0, n)
    }

    /**
     * Randomly choose (without replacement) up to [n] values from the [set]
     */
    fun <T> choose(set: Set<T>, n: Int): Set<T>{
        if(set.size <= n){
            return set
        }

        val selection: MutableList<T> = mutableListOf()
        selection.addAll(set)
        Collections.shuffle(selection, random)

        return selection.subList(0, n).toSet()
    }



    fun <K,V> choose(map: Map<K,V>): V = choose(map.values)


    /**
     * Randomly choose one element
     */
    fun <V> choose(collection: Collection<V>) : V{
        if (collection.isEmpty()) {
            throw IllegalArgumentException("Empty map to choose from")
        }
        val index = random.nextInt(collection.size)
        var i = 0

        val iter = collection.iterator()
        while(iter.hasNext() && i<index){
            iter.next()
            i++
        }
        return iter.next()
    }
}