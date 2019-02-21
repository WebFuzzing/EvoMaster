package org.evomaster.core.problem.rest.serviceII

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.serviceII.resources.RestAResource
import org.evomaster.core.problem.rest2.resources.ResourceManageService
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchTimeController
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SmartSamplingController {

    @Inject
    private lateinit var time : SearchTimeController

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var config : EMConfig

    @Inject
    private lateinit var rm : ResourceManageService

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SmartSamplingController::class.java)
    }

    private var selected : SampleStrategy = SampleStrategy.S1iR

    fun initialize(){
        initApplicableStrategies()
        initProbability()
        validateProbability()
    }

    private fun initApplicableStrategies(){
        SampleStrategy.S1iR.applicable = true //mutableMap.values.filter { r -> r.hasIndependentAction }.isNotEmpty()
        rm.getResourceCluster().values.filter { r -> !r.independent }?.let {
            SampleStrategy.S1dR.applicable = it.isNotEmpty()
            SampleStrategy.S2dR.applicable = it.size > 1
            SampleStrategy.SMdR.applicable = it.size > 2
        }

        //FIXME Man Zhang
        if(SampleStrategy.values().filter { it.applicable }.size == 1 ) config.maxTestSize = 1
    }

    private fun printApplicableStr(){
        println("Applicable SmartSampleStrategy>>>")
        println( SampleStrategy.values().filter{ it.applicable }.mapNotNull { "$it : ${it.probability}"}.joinToString (" - "))
    }

    private fun printSummaryOfResources(mutableMap: Map<String, RestAResource>){
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
    private fun validateProbability() {
        if(SampleStrategy.values().map { it.probability }.sum() != 1.0){
            log.warn("a sum of probability of applicable strategies is not 1")
        }
    }

    private fun initProbability(){
        printSummaryOfResources(rm.getResourceCluster())
        when(config.sampleControl){
            EMConfig.ResourceSamplingControl.EqualProbability -> initEqualProbability()
            EMConfig.ResourceSamplingControl.Customized -> initProbabilityWithSpecified()
            EMConfig.ResourceSamplingControl.Actions -> initProbabilityWithActions(rm.getResourceCluster())
            EMConfig.ResourceSamplingControl.TimeBudgets -> initEqualProbability()
            EMConfig.ResourceSamplingControl.Archive -> initEqualProbability()
            EMConfig.ResourceSamplingControl.ConArchive -> initEqualProbability()
        }
        printApplicableStr()
        update()
    }

    private fun update(){
        config.S1iR = SampleStrategy.S1iR.probability
        config.S1dR = SampleStrategy.S1dR.probability
        config.S2dR = SampleStrategy.S2dR.probability
        config.SMdR = SampleStrategy.SMdR.probability
    }

    private fun set(){
        SampleStrategy.S1iR.probability = config.S1iR
        SampleStrategy.S1dR.probability = config.S1dR
        SampleStrategy.S2dR.probability = config.S2dR
        SampleStrategy.SMdR.probability = config.SMdR
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
        selected =
            when(config.sampleControl){
                EMConfig.ResourceSamplingControl.EqualProbability -> getStrategyWithItsProbability()
                EMConfig.ResourceSamplingControl.Customized -> getStrategyWithItsProbability()
                EMConfig.ResourceSamplingControl.Actions ->  getStrategyWithItsProbability()
                EMConfig.ResourceSamplingControl.TimeBudgets -> relyOnTB()
                EMConfig.ResourceSamplingControl.Archive -> relyOnArchive()
                EMConfig.ResourceSamplingControl.ConArchive -> relyOnArchive2()
            }
        selected.times += 1
        return selected
    }
    private fun initEqualProbability(){
        SampleStrategy.values().filter { it.applicable }.let {
            l -> l.forEach { s -> s.probability = 1.0.div(l.size) }
        }
        validateProbability()
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
    private fun initProbabilityWithActions(mutableMap: Map<String, RestAResource>){
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
        if(SampleStrategy.values().filter { it.applicable }.size == 1) return getStrategyWithItsProbability()
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
            initEqualProbability()
        }
        update()
        return getStrategyWithItsProbability()
    }

    /**
     * probability is assigned adaptively regarding Archive,
     */
    private fun relyOnArchive() : SampleStrategy{
        if(SampleStrategy.values().filter { it.applicable }.size == 1) return getStrategyWithItsProbability()
        val delta = 0.1
        val applicableSS = SampleStrategy.values().filter { it.applicable }
        val total = Array(applicableSS.size){i -> i+1 }.sum()
        applicableSS.asSequence().sortedBy {  it.improved }.forEachIndexed { index, ss ->
            ss.probability = ss.probability * (1 - delta) + delta * (index + 1)/total
        }
        update()
        return getStrategyWithItsProbability()
    }


    /**
     * probability is assigned adaptively regarding Archive,
     */
    private fun relyOnArchive2() : SampleStrategy{
        if(SampleStrategy.values().filter { it.applicable }.size == 1) return getStrategyWithItsProbability()

        val focusedStrategy = 1.0
        val passed = time.percentageUsedBudget()
        val threshold = config.focusedSearchActivationTime

        val used = passed/threshold
        resetProbability()
        if(used < 0.2){
            val one = SampleStrategy.values().filter { it.applicable && (it == SampleStrategy.S1iR || it == SampleStrategy.S1dR)}.size
            val two = SampleStrategy.values().filter { it.applicable}.size - one
            SampleStrategy.values().filter { it.applicable }.forEach { s->
                when(s){
                    SampleStrategy.S1iR -> s.probability = focusedStrategy/one
                    SampleStrategy.S1dR -> s.probability = focusedStrategy/one
                    else -> s.probability = (1.0 - focusedStrategy)/two
                }
            }
            return getStrategyWithItsProbability()
        }else{
            initEqualProbability()
        }

        val delta = 0.1
        val applicableSS = SampleStrategy.values().filter { it.applicable }
        val total = Array(applicableSS.size){i -> i+1 }.sum()
        applicableSS.asSequence().sortedBy {  it.improved }.forEachIndexed { index, ss ->
            ss.probability = ss.probability * (1 - delta) + delta * (index + 1)/total
        }
        update()
        return getStrategyWithItsProbability()
    }

    private fun getStrategyWithItsProbability(): SampleStrategy{
        validateProbability()
        val result = randomWithProbability(SampleStrategy.values().filter { it.applicable }.toTypedArray(),
                SampleStrategy.values().filter { it.applicable }.map { it.probability }.toTypedArray())
        return result as SampleStrategy
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
    enum class SampleStrategy (var applicable : Boolean = false, var probability : Double = 0.0, var times : Int = 0, var improved : Int = 0){
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
        if(array.size != arrayProb.size) return null
        val maxSample = 1000000
        var result : Any?= null
        while(result == null){
            val num = randomness.nextInt(1, maxSample)
            var min = 0.0
            var max = 0.0
            for (e in 0 until array.size){
                max += maxSample * arrayProb[e]
                if(num > min && num <= max) {
                    result =  array[e]
                }
                min = max
            }
        }
        return result
        //throw IllegalStateException("num does not fail into 1-$maxSample??")
    }

    private fun resetProbability(){
        SampleStrategy.values().forEach {
            it.probability = 0.0
        }
    }

    fun reportImprovement(){
        selected.improved +=1
    }

}