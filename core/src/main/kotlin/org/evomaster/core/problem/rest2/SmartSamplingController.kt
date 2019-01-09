package org.evomaster.core.problem.rest.serviceII

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.serviceII.resources.RestAResource
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchTimeController

class SmartSamplingController {

    @Inject
    private lateinit var time : SearchTimeController

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var config : EMConfig


    fun initApplicableStrategies(mutableMap: MutableMap<String, RestAResource>){
        SampleStrategy.S1iR.applicable = mutableMap.values.filter { r -> r.hasIndependentAction }.isNotEmpty()
        mutableMap.values.filter { r -> !r.independent }?.let {
            SampleStrategy.S1dR.applicable = it.isNotEmpty()
            SampleStrategy.S2dR.applicable = it.size > 1
            SampleStrategy.SMdR.applicable = it.size > 2
        }
    }

    private fun printApplicableStr(){
        println("Applicable SmartSampleStrategy>>>")
        println( SampleStrategy.values().filterNot { it.applicable }.mapNotNull { "$it : ${it.probability}"}.joinToString (" - "))
    }

    private fun printSummaryOfResources(mutableMap: MutableMap<String, RestAResource>){
        println("Summary of abstract resources and actions>>>")
        val message ="""
            #Rs ${mutableMap.size}
            #IndRs ${mutableMap.values.filter { it.independent }.size}
            #hasIndActionRs ${mutableMap.values.filter { it.hasIndependentAction }.size}
            #DepRs ${mutableMap.values.filterNot { it.independent }.size}

            #Actions ${mutableMap.values.map { it.actions.size }.sum()}
            #IndActions ${mutableMap.values.map { it.templates.filter { t -> t.value.independent }.size }.sum()}
            #depActions ${mutableMap.values.map { it.templates.filter { t -> !t.value.independent }.size}.sum()}
            #depComActions ${mutableMap.values.map { it.templates.filter { t -> !t.value.independent }.size * (it.templates.filter { !it.value.independent }.size -1) }.sum()}
            """
        println(message)
    }

    fun initProbability(mutableMap: MutableMap<String, RestAResource>){
        printSummaryOfResources(mutableMap)
        when(config.sampleControl){
            EMConfig.ResourceSamplingControl.EqualProbability -> initEqualProbability()
            EMConfig.ResourceSamplingControl.SpecifiedProbability -> initProbabilityWithSpecified()
            EMConfig.ResourceSamplingControl.BasedOnActions -> initProbabilityWithActions(mutableMap)
            EMConfig.ResourceSamplingControl.BasedOnTimeBudgets -> TODO()
            EMConfig.ResourceSamplingControl.BasedOnArchive -> TODO()
        }

        printApplicableStr()
    }

    private fun printCounters(){
        println(SampleStrategy.values().map { it.times }.joinToString("-"))
    }

    private fun printImproved(){
        println("improvement with selected strategy: "+SampleStrategy.values().map { it.improved }.joinToString("-"))
    }

    private fun printImprovedPercentage(){
        println(SampleStrategy.values().map { it.improved * 1.0/it.times }.joinToString("-"))
    }

    fun getSampleStrategy() : SampleStrategy{
        val select =
            when(config.sampleControl){
                EMConfig.ResourceSamplingControl.EqualProbability -> getStrategyWithItsProbability()
                EMConfig.ResourceSamplingControl.SpecifiedProbability -> getStrategyWithItsProbability()
                EMConfig.ResourceSamplingControl.BasedOnActions ->  getStrategyWithItsProbability()
                EMConfig.ResourceSamplingControl.BasedOnTimeBudgets -> relyOnTB()
                EMConfig.ResourceSamplingControl.BasedOnArchive -> relyOnArchive()
            }
        select.times += 1
        return select
    }
    private fun initEqualProbability(){
        SampleStrategy.values().filter { it.applicable }.let {
            l -> l.forEach { s -> s.probability = 1.0/l.size }
        }
    }

    private fun initProbabilityWithSpecified(){
        val specified = arrayOf(0.2, 0.4, 0.4, 0.0)
        SampleStrategy.values().forEachIndexed { index, sampleStrategy ->
            sampleStrategy.probability = specified[index]
        }
    }

    private fun random() : SampleStrategy{
        return randomness.choose(SampleStrategy.values().filter { it.applicable }.toList())
    }

    /**
     * probability is assigned based on percentage of actions that are dependent or independent regarding resources
     */
    private fun initProbabilityWithActions(mutableMap: MutableMap<String, RestAResource>){
        val numOfDepActions = mutableMap.values.map { it.numOfDepTemplate() }.sum()
        val num = mutableMap.values.map { it.numOfTemplates() }.sum()
        /**
         * weightOfDep presents a weight of sampling dependent resources
         */
        val weightOfDep = 2
        /**
         * probability of sampling independent resource
         */
        val pInd = (num - numOfDepActions ) * 1.0 / (num + numOfDepActions * (weightOfDep - 1))

        val total = SampleStrategy.values().filter { it.applicable }.map { s->
            when(s){
                SampleStrategy.S1iR -> 0
                SampleStrategy.S1dR -> 3
                SampleStrategy.S2dR -> 2
                SampleStrategy.SMdR -> 1
            }
        }.sum()

        SampleStrategy.values().filter { it.applicable }.forEach { s->
            when(s){
                SampleStrategy.S1iR -> s.probability = pInd
                SampleStrategy.S1dR -> s.probability = (1-pInd) * 3 / total
                SampleStrategy.S2dR -> s.probability = (1-pInd) * 2 / total
                SampleStrategy.SMdR -> s.probability = (1-pInd) * 1 / total
            }
        }
    }

    /**
     * probability is assigned adaptively regarding used time budget. in a starting point, it is likely to sample one resource then start multiple resources.
     */
    private fun relyOnTB() : SampleStrategy{
        val focusedStrategy = 0.8

        val passed = time.percentageUsedBudget()
        val threshold = config.focusedSearchActivationTime

        val used = passed/threshold
        resetProbability()
        if(used < 0.5){
            val one = SampleStrategy.values().filter { it.applicable && (it == SampleStrategy.S1iR || it == SampleStrategy.S1dR)}.size
            val two = SampleStrategy.values().filter { it.applicable}.size - one
            SampleStrategy.values().filter { it.applicable }.forEach { s->
                when(s){
                    SampleStrategy.S1iR -> s.probability = focusedStrategy/one
                    SampleStrategy.S1dR -> s.probability = focusedStrategy/one
                    else -> s.probability = (1.0 - focusedStrategy)/two
                }
            }
        }else{
            random()
        }
        return getStrategyWithItsProbability()
    }

    /**
     * probability is assigned adaptively regarding Archive,
     */
    private fun relyOnArchive() : SampleStrategy{
        val delta = 0.1
        val applicableSS = SampleStrategy.values().filter { it.applicable }
        val total = Array(applicableSS.size){i -> i+1 }.sum()
        applicableSS.asSequence().sortedBy {  it.improved }.forEachIndexed { index, ss ->
            ss.probability == ss.probability * (1 - delta) + delta * (index + 1)/total
        }
        return getStrategyWithItsProbability()
    }

    private fun getStrategyWithItsProbability(): SampleStrategy{
        return randomWithProbability(SampleStrategy.values().filter { it.applicable }.toTypedArray(), SampleStrategy.values().filter { it.applicable }.map { it.probability }.toTypedArray()) as SampleStrategy
    }

    /**
     * SampleStrategy presents detailed information about how to sample,
     * including 1) [applicable] a set of applicable strategies; 2) [probability] a probability to select an applicable strategy; 3) [times] times to select an applicable strategy; and 4) [improved] times to help to improve Archive
     *
     * @property applicable presents whether a strategy is applicable for a system under test, e.g., if all resource of the SUT are independent, then only [S1iR] is applicable
     * @property probability presents a probability to apply the strategy during sampling phase
     * @property times presents how many times the strategy is applied
     * @property improved presents how many times the strategy helps to improve Archive. Note that improved <= times
     *
     * */
    enum class SampleStrategy (var applicable : Boolean = false, var probability : Double = 0.25, var times : Int = 0, var improved : Int = 0){
        /**
         * Sample 1 independent Resource, with an aim of exploiting diverse instances of resources
         */
        S1iR,
        /**
         * Sample 1 dependent Resource, with an aim of exploiting diverse instances of resources
         */
        S1dR,
        /**
         * Sample 2 dependent Resources, with an aim of exploiting diverse 1) relationship of resources, 2) instances of resources
         */
        S2dR,
        /**
         * Sample More than two Dependent Resources, with an aim of exploiting diverse 1) relationship of resources, 2) instances of resources
         */
        SMdR,
    }

    /**
     * An implementation of randomness with specified probability. It is required a further modification
     */
    private fun randomWithProbability(array: Array<*>, arrayProb: Array<Double>) : Any?{
        if(array.size != arrayProb.size || arrayProb.sum() != 1.0) return null
        val maxSample = 1000000
        val num = randomness.nextInt(1, maxSample)
        var min = 0.0
        var max = 0.0
        for (e in 0 until array.size){
            max += maxSample * arrayProb[e]
            if(num > min && num <= max) return array[e]
            min = max
        }
        throw IllegalStateException("num does not fail into 1-$maxSample??")
    }

    private fun resetProbability(){
        SampleStrategy.values().forEach {
            it.probability = 0.0
        }
    }
}