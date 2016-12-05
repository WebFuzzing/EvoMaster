package org.evomaster.core.search


class Randomness {

    private val random = java.util.Random()

    constructor(seed: Long) {
        random.setSeed(seed)
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

        return (min as Long + Math.random() * (max as Long - min + 1)) as Int
    }

    fun <T> choose(list: List<T>): T {
        if (list.isEmpty()) {
            throw IllegalArgumentException("Empty list to choose from")
        }
        val index = random.nextInt(list.size)
        return list[index]
    }
}