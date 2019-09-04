package org.evomaster.core.problem.util

import java.util.ArrayList

/**
 * created by manzh on 2019-08-31
 */
object StringSimilarityComparator {

    const val SimilarityThreshold = 0.6

    fun isSimilar(str1: String, str2: String, algorithm: SimilarityAlgorithm = SimilarityAlgorithm.Trigrams, threshold : Double = SimilarityThreshold) : Boolean{
        return stringSimilarityScore(str1, str2, algorithm) >= threshold
    }

    /**
     * TODO Man: need to improve
     */
    fun stringSimilarityScore(str1 : String, str2 : String, algorithm : SimilarityAlgorithm =SimilarityAlgorithm.Trigrams): Double{
        return when(algorithm){
            SimilarityAlgorithm.Trigrams -> trigrams(bigram(str1.toLowerCase()), bigram(str2.toLowerCase()))
            //else-> 0.0
        }
    }

    private fun trigrams(bigram1: MutableList<CharArray>, bigram2 : MutableList<CharArray>) : Double{
        val copy = ArrayList(bigram2)
        var matches = 0
        var i = bigram1.size
        while (--i >= 0) {
            val bigram = bigram1[i]
            var j = copy.size
            while (--j >= 0) {
                val toMatch = copy[j]
                if (bigram[0] == toMatch[0] && bigram[1] == toMatch[1]) {
                    copy.removeAt(j)
                    matches += 2
                    break
                }
            }
        }
        return matches.toDouble() / (bigram1.size + bigram2.size)
    }

    private fun bigram(input: String): MutableList<CharArray> {
        val bigram = mutableListOf<CharArray>()
        for (i in 0 until input.length - 1) {
            val chars = CharArray(2)
            chars[0] = input[i]
            chars[1] = input[i + 1]
            bigram.add(chars)
        }
        return bigram
    }
}

enum class SimilarityAlgorithm{
    Trigrams
}
