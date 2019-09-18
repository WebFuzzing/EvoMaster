package org.evomaster.experiments.stringMutation

import com.google.inject.Inject
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.Randomness
import org.evomaster.experiments.linear.ProblemType

/**
 * created by manzh on 2019-09-16
 */
class StringProblemDefinition {

    @Inject
    lateinit var randomness : Randomness

    var nTargets = 1

    var maxLength = 16

    var specifiedLength = -1

    var optima: MutableList<String> = mutableListOf()

    fun init(n : Int, sLength : Int = -1, maxLength: Int = 16){
        nTargets = n
        specifiedLength = sLength
        this.maxLength = maxLength
        optima.clear()
        (0 until n).forEach { _ ->
            optima.add(randomString())
        }
    }

    fun distance(list: MutableList<StringGene>) : Map<Int, Double>{
        assert(list.size == optima.size)
        val result = mutableMapOf<Int, Double>()
        (0 until nTargets).forEach{ index ->
            result[index] = distance(index, list[index].value )
        }
        return result
    }

    private fun distance(index : Int, value : String) : Double{
        val target = optima[index]
        return DistanceUtils.distance(target, value)
    }

    private fun randomString() : String{
        val length = if (specifiedLength != -1) specifiedLength else randomness.nextInt(1, maxLength)
        var value = randomness.nextWordChar().toString()
        (1 until length).forEach { _ ->
            value += randomness.nextWordChar()
        }
        return value
    }
}