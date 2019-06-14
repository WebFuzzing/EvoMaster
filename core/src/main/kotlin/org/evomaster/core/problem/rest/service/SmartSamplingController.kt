package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.resource.RestResourceNode
import org.evomaster.core.problem.rest.resource.SamplerSpecification
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchTimeController
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * dynamically determinate resource-based sample method
 */
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
        private const val CONARCHIVE_THRESHOLD = 0.2
        private const val TB_THRESHOLD = 0.5

    }

    fun initialize(){
        initApplicableStrategies()
        initProbability()
        validateProbability()
    }

    private fun initApplicableStrategies(){
        ResourceSamplingMethod.S1iR.applicable = true //mutableMap.values.filter { r -> r.hasIndependentAction }.isNotEmpty()
        rm.getResourceCluster().values.filter { r -> !r.isIndependent() }.let {
            ResourceSamplingMethod.S1dR.applicable = it.isNotEmpty()
            ResourceSamplingMethod.S2dR.applicable = it.size > 1
            ResourceSamplingMethod.SMdR.applicable = it.size > 2
        }

        //FIXME Man Zhang
        if(ResourceSamplingMethod.values().filter { it.applicable }.size == 1 ) config.maxTestSize = 1
    }

    private fun printApplicableStr(){
        println("Applicable SmartSampleStrategy>>>")
        println( ResourceSamplingMethod.values().filter{ it.applicable }.mapNotNull { "$it : ${it.probability}"}.joinToString (" - "))
    }

    private fun printSummaryOfResources(mutableMap: Map<String, RestResourceNode>){
        println("Summary of abstract resources and actions>>>")
        val message ="""
            #Rs ${mutableMap.size}
            #IndRs ${mutableMap.values.filter { it.isIndependent() }.size}
            #hasIndActionRs ${mutableMap.values.filter { it.hasIndependentAction() }.size}
            #DepRs ${mutableMap.values.filterNot { it.isIndependent() }.size}

            #Actions ${mutableMap.values.map { it.actions.size }.sum()}
            #IndActions ${mutableMap.values.map { it.templates.filter { t -> t.value.independent }.size }.sum()}
            #depActions ${mutableMap.values.map { it.templates.filter { t -> !t.value.independent }.size}.sum()}
            #depComActions ${mutableMap.values.map { it.templates.filter { t -> !t.value.independent }.size * (it.templates.filter { !it.value.independent }.size -1) }.sum()}
            """
        println(message)
    }
    private fun validateProbability() {
        if(ResourceSamplingMethod.values().map { it.probability }.sum() != 1.0){
            log.warn("a sum of probability of applicable strategies is not 1")
        }
    }

    private fun initProbability(){
        //printSummaryOfResources(rm.getResourceCluster())
        when(config.resourceSampleStrategy){
            EMConfig.ResourceSamplingStrategy.EqualProbability -> initEqualProbability()
            EMConfig.ResourceSamplingStrategy.Customized -> initProbabilityWithSpecified()
            EMConfig.ResourceSamplingStrategy.Actions -> initProbabilityWithActions(rm.getResourceCluster())
            EMConfig.ResourceSamplingStrategy.TimeBudgets -> initEqualProbability()
            EMConfig.ResourceSamplingStrategy.Archive -> initEqualProbability()
            EMConfig.ResourceSamplingStrategy.ConArchive -> initEqualProbability()
            else->{
                throw IllegalArgumentException("wrong invocation of SmartSamplingController!")
            }
        }
        //printApplicableStr()
        update()
    }

    private fun update(){
        config.S1iR = ResourceSamplingMethod.S1iR.probability
        config.S1dR = ResourceSamplingMethod.S1dR.probability
        config.S2dR = ResourceSamplingMethod.S2dR.probability
        config.SMdR = ResourceSamplingMethod.SMdR.probability
    }

    private fun set(){
        ResourceSamplingMethod.S1iR.probability = config.S1iR
        ResourceSamplingMethod.S1dR.probability = config.S1dR
        ResourceSamplingMethod.S2dR.probability = config.S2dR
        ResourceSamplingMethod.SMdR.probability = config.SMdR
    }

    private fun printCounters(){
        println(ResourceSamplingMethod.values().map { it.times }.joinToString("-"))
    }

    private fun printImproved(){
        println("improvement with selected strategy: "+ResourceSamplingMethod.values().map { it.improved }.joinToString("-"))
    }

    private fun printImprovedPercentage(){
        println(ResourceSamplingMethod.values().map { it.improved * 1.0/it.times }.joinToString("-"))
    }

    fun getSampleStrategy() : ResourceSamplingMethod{
        val selected =
            when(config.resourceSampleStrategy){
                EMConfig.ResourceSamplingStrategy.EqualProbability -> getStrategyWithItsProbability()
                EMConfig.ResourceSamplingStrategy.Customized -> getStrategyWithItsProbability()
                EMConfig.ResourceSamplingStrategy.Actions ->  getStrategyWithItsProbability()
                EMConfig.ResourceSamplingStrategy.TimeBudgets -> relyOnTB()
                EMConfig.ResourceSamplingStrategy.Archive -> relyOnArchive()
                EMConfig.ResourceSamplingStrategy.ConArchive -> relyOnConArchive()
                else ->{
                    null
                }
            }
        assert(selected != null)
        selected!!.times += 1
        return selected!!
    }
    private fun initEqualProbability(){
        ResourceSamplingMethod.values().filter { it.applicable }.let {
            l -> l.forEach { s -> s.probability = 1.0.div(l.size) }
        }
        validateProbability()
    }

    private fun initProbabilityWithSpecified(){
        val specified = arrayOf(0.2, 0.4, 0.4, 0.0)
        ResourceSamplingMethod.values().forEachIndexed { index, sampleStrategy ->
            sampleStrategy.probability = specified[index]
        }
    }

    private fun random() : ResourceSamplingMethod{
        return randomness.choose(ResourceSamplingMethod.values().filter { it.applicable }.toList())
    }

    /**
     * probability is assigned based on percentage of actions that are dependent or independent regarding resources
     */
    private fun initProbabilityWithActions(mutableMap: Map<String, RestResourceNode>){
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

        val total = ResourceSamplingMethod.values().filter { it.applicable }.map { s->
            when(s){
                ResourceSamplingMethod.S1iR -> 0
                ResourceSamplingMethod.S1dR -> 3
                ResourceSamplingMethod.S2dR -> 2
                ResourceSamplingMethod.SMdR -> 1
            }
        }.sum()

        ResourceSamplingMethod.values().filter { it.applicable }.forEach { s->
            when(s){
                ResourceSamplingMethod.S1iR -> s.probability = pInd
                ResourceSamplingMethod.S1dR -> s.probability = (1-pInd) * 3 / total
                ResourceSamplingMethod.S2dR -> s.probability = (1-pInd) * 2 / total
                ResourceSamplingMethod.SMdR -> s.probability = (1-pInd) * 1 / total
            }
        }
    }

    /**
     * probability is assigned adaptively regarding used time budget. in a starting point, it is likely to sample one resource then start multiple resources.
     */
    private fun relyOnTB() : ResourceSamplingMethod{
        if(ResourceSamplingMethod.values().filter { it.applicable }.size == 1) return getStrategyWithItsProbability()
        val focusedStrategy = 0.8
        val passed = time.percentageUsedBudget()
        val threshold = config.focusedSearchActivationTime

        val used = passed/threshold
        resetProbability()
        if(used < TB_THRESHOLD){
            val one = ResourceSamplingMethod.values().filter { it.applicable && (it == ResourceSamplingMethod.S1iR || it == ResourceSamplingMethod.S1dR)}.size
            val two = ResourceSamplingMethod.values().filter { it.applicable}.size - one
            ResourceSamplingMethod.values().filter { it.applicable }.forEach { s->
                when(s){
                    ResourceSamplingMethod.S1iR -> s.probability = focusedStrategy/one
                    ResourceSamplingMethod.S1dR -> s.probability = focusedStrategy/one
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
    private fun relyOnArchive() : ResourceSamplingMethod{
        if(ResourceSamplingMethod.values().filter { it.applicable }.size == 1) return getStrategyWithItsProbability()
        val delta = 0.1
        val applicableSS = ResourceSamplingMethod.values().filter { it.applicable }
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
    private fun relyOnConArchive() : ResourceSamplingMethod{
        if(ResourceSamplingMethod.values().filter { it.applicable }.size == 1) return getStrategyWithItsProbability()

        val focusedStrategy = 1.0
        val passed = time.percentageUsedBudget()
        val threshold = config.focusedSearchActivationTime

        val used = passed/threshold
        resetProbability()
        if(used < CONARCHIVE_THRESHOLD){
            val one = ResourceSamplingMethod.values().filter { it.applicable && (it == ResourceSamplingMethod.S1iR || it == ResourceSamplingMethod.S1dR)}.size
            val two = ResourceSamplingMethod.values().filter { it.applicable}.size - one
            ResourceSamplingMethod.values().filter { it.applicable }.forEach { s->
                when(s){
                    ResourceSamplingMethod.S1iR -> s.probability = focusedStrategy/one
                    ResourceSamplingMethod.S1dR -> s.probability = focusedStrategy/one
                    else -> s.probability = (1.0 - focusedStrategy)/two
                }
            }
            return getStrategyWithItsProbability()
        }else{
            initEqualProbability()
        }

        val delta = 0.1
        val applicableSS = ResourceSamplingMethod.values().filter { it.applicable }
        val total = Array(applicableSS.size){i -> i+1 }.sum()
        applicableSS.asSequence().sortedBy {  it.improved }.forEachIndexed { index, ss ->
            ss.probability = ss.probability * (1 - delta) + delta * (index + 1)/total
        }
        update()
        return getStrategyWithItsProbability()
    }

    private fun getStrategyWithItsProbability(): ResourceSamplingMethod{
        validateProbability()
        val result = randomWithProbability(ResourceSamplingMethod.values().filter { it.applicable }.toTypedArray(),
                ResourceSamplingMethod.values().filter { it.applicable }.map { it.probability }.toTypedArray())
        return result as ResourceSamplingMethod
    }

    /**
     * An implementation of randomness with specified probability.
     * TODO It is required a further modification
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
    }

    private fun resetProbability(){
        ResourceSamplingMethod.values().forEach {
            it.probability = 0.0
        }
    }

    fun reportImprovement(samplerSpecification: SamplerSpecification){
        //it may be null when executing ad-hoc rest action
        ResourceSamplingMethod.values().find { it.name == samplerSpecification.methodKey }?.let {
            it.improved +=1
        }
    }

}

/**
 * ResourceSamplingMethod presents detailed information about how to sample,
 * including 1) [applicable] a set of applicable strategies; 2) [probability] a probability to select an applicable strategy; 3) [times] times to select an applicable strategy; and 4) [improved] times to help to improve Archive
 *
 * @property applicable presents whether a strategy is applicable for a system under test, e.g., if all resource of the SUT are independent, then only [S1iR] is applicable
 * @property probability presents a probability to apply the strategy during sampling phase
 * @property times presents how many times the strategy is applied
 * @property improved presents how many times the strategy helps to improve Archive. Note that improved <= times
 *
 * */
enum class ResourceSamplingMethod (var applicable : Boolean = false, var probability : Double = 0.0, var times : Int = 0, var improved : Int = 0){
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
