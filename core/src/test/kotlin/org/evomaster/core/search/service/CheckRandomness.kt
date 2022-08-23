package org.evomaster.core.search.service

class CheckRandomness {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val rand = Randomness()
            rand.updateSeed(42)

            for (i in 0..10) {
                println(rand.nextInt(0, 1000))
            }
        }
    }
}