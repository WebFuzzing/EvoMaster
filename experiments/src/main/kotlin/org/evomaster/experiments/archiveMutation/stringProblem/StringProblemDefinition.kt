package org.evomaster.experiments.archiveMutation.stringProblem

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper
import org.evomaster.core.search.service.mutator.geneMutation.CharPool
import org.evomaster.experiments.archiveMutation.ArchiveProblemDefinition

/**
 * created by manzh on 2019-09-23
 */
abstract class StringProblemDefinition : ArchiveProblemDefinition<StringIndividual>() {

    var maxLength = 16

    var specifiedLength = -1

    val charPool : CharPool = CharPool.ALL

    abstract fun init(n : Int, sLength : Int = -1, maxLength: Int = 16)

    fun leftDistance(target : String, value: String) : Double{
        val distance = DistanceHelper.getLeftAlignmentDistance(target, value)
        return 1.0/(1.0 + distance)
    }

    fun randomString() : String{
        val length = if (specifiedLength != -1) specifiedLength else randomness.nextInt(1, maxLength)
        var value = when(charPool){
            CharPool.WORD -> randomness.nextWordChar().toString()
            CharPool.ALL -> randomness.nextChar().toString()
            else -> {
                TODO()
            }
        }

        (1 until length).forEach { _ ->
            value += when(charPool){
                CharPool.WORD -> randomness.nextWordChar().toString()
                CharPool.ALL -> randomness.nextChar().toString()
                else -> {
                    TODO()
                }
            }
        }
        return value
    }
}