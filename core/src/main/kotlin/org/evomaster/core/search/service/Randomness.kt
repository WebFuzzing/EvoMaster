package org.evomaster.core.search.service

import java.util.*


class Randomness {

    private val random = Random()

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

    fun nextBoolean(p: Double) = random.nextDouble() < p

    fun nextInt() = random.nextInt()

    fun nexInt(bound: Int) = random.nextInt(bound)

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

    fun nextInt(min: Int, max: Int): Int {
        if (min == max) {
            return min
        }
        if (min > max) {
            throw IllegalArgumentException("Min $min is bigger than max $max")
        }

        return (min.toLong() + Math.random() * (max.toLong() - min + 1)).toInt()
    }


    fun <T> choose(list: List<T>): T {
        if (list.isEmpty()) {
            throw IllegalArgumentException("Empty list to choose from")
        }
        val index = random.nextInt(list.size)
        return list[index]
    }

    /**
     * Randomly choose (without replacement) up to n values from the list
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

    fun <T> choose(set: Set<T>, n: Int): Set<T>{
        if(set.size <= n){
            return set
        }

        val selection: MutableList<T> = mutableListOf()
        selection.addAll(set)
        Collections.shuffle(selection, random)

        return selection.subList(0, n).toSet()
    }


    fun <K,V> choose(map: Map<K,V>): V {
        if (map.isEmpty()) {
            throw IllegalArgumentException("Empty map to choose from")
        }
        val index = random.nextInt(map.size)
        var i = 0

        val iter = map.values.iterator()
        while(iter.hasNext() && i<index){
            iter.next()
            i++
        }
        return iter.next()
    }
}