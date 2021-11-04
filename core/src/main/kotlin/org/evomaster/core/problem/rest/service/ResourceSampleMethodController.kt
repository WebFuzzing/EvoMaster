package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.resource.RestResourceNode
import org.evomaster.core.problem.rest.resource.SamplerSpecification
import org.evomaster.core.problem.rest.service.ResourceSamplingMethod.*
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchTimeController
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * dynamically determinate resource-based sample method
 */
class ResourceSampleMethodController {

    @Inject
    private lateinit var time : SearchTimeController

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var config : EMConfig

    @Inject
    private lateinit var rm : ResourceManageService

    private val methods : Map<ResourceSamplingMethod, MethodApplicationInfo> = values().map { Pair(it, MethodApplicationInfo()) }.toMap()

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ResourceSampleMethodController::class.java)
        private const val CONARCHIVE_THRESHOLD = 0.2
        private const val TB_THRESHOLD = 0.5

    }

    fun initialize(){
        initApplicableStrategies()
        initProbability()
        validateProbability()
    }

    private fun initApplicableStrategies(){
        methods.getValue(S1iR).applicable = true
        rm.getResourceCluster().values.filter { r -> !r.isIndependent() }.let {
            methods.getValue(S1dR).applicable = it.isNotEmpty()
            methods.getValue(S2dR).applicable = it.size > 1 && config.maxTestSize > 1
            methods.getValue(SMdR).applicable = it.size > 2 && config.maxTestSize > 2
        }

        /**
         * if only S1iR is applicable, we recommend that maxTestSize is 1.
         */
        if(methods.values.filter { it.applicable }.size == 1 ) config.maxTestSize = 1
    }

    private fun validateProbability() {
        if(methods.values.map { it.probability }.sum().run { this > 1.1 && this < 0.9 }){
            log.warn("a sum of probability of applicable strategies should be 1.0 but ${methods.values.map { it.probability }.sum()}")
        }
    }

    private fun initProbability(){
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
        update()
    }

    private fun update(){
        config.S1iR = methods.getValue(S1iR).probability
        config.S1dR = methods.getValue(S1dR).probability
        config.S2dR = methods.getValue(S2dR).probability
        config.SMdR = methods.getValue(SMdR).probability
    }

    fun getSampleStrategy() : ResourceSamplingMethod{
        if(methods.filter { it.value.applicable }.size == 1) return getStrategyWithItsProbability()
        val selected =
            when(config.resourceSampleStrategy){
                EMConfig.ResourceSamplingStrategy.EqualProbability -> getStrategyWithItsProbability()
                EMConfig.ResourceSamplingStrategy.Customized -> getStrategyWithItsProbability()
                EMConfig.ResourceSamplingStrategy.Actions ->  getStrategyWithItsProbability()
                EMConfig.ResourceSamplingStrategy.TimeBudgets -> relyOnTB()
                EMConfig.ResourceSamplingStrategy.Archive -> relyOnArchive()
                EMConfig.ResourceSamplingStrategy.ConArchive -> relyOnConArchive()
                else ->{
                    throw IllegalStateException()
                }
            }
        methods.getValue(selected).times += 1
        return selected
    }
    private fun initEqualProbability(){
        methods.values.filter { it.applicable }.let {
            l -> l.forEach { s -> s.probability = 1.0.div(l.size) }
        }
        validateProbability()
    }

    private fun initProbabilityWithSpecified(){
        methods.getValue(S1iR).probability = config.S1iR
        methods.getValue(S1dR).probability = config.S1dR
        methods.getValue(S2dR).probability = config.S2dR
        methods.getValue(SMdR).probability = config.SMdR
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

        val total = methods.filter { it.value.applicable }.keys.map { s->
            when(s){
                S1iR -> 0
                S1dR -> 3
                S2dR -> 2
                SMdR -> 1
            }
        }.sum()

        methods.filter { it.value.applicable }.forEach { s->
            when(s.key){
                S1iR -> s.value.probability = pInd
                S1dR -> s.value.probability = (1-pInd) * 3 / total
                S2dR -> s.value.probability = (1-pInd) * 2 / total
                SMdR -> s.value.probability = (1-pInd) * 1 / total
            }
        }
    }

    /**
     * probability is assigned adaptively regarding used time budget. in a starting point, it is likely to sample one resource then start multiple resources.
     */
    private fun relyOnTB() : ResourceSamplingMethod{
        val focusedStrategy = 0.8
        val passed = time.percentageUsedBudget()
        val threshold = config.focusedSearchActivationTime

        val used = passed/threshold
        resetProbability()
        if(used < TB_THRESHOLD){
            val one = methods.filter { it.value.applicable && (it.key == S1iR || it.key == S1dR)}.size
            val two = methods.filter { it.value.applicable}.size - one
            methods.filter { it.value.applicable }.forEach { s->
                when(s.key){
                    S1iR -> s.value.probability = focusedStrategy/one
                    S1dR -> s.value.probability = focusedStrategy/one
                    else -> s.value.probability = (1.0 - focusedStrategy)/two
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
        val delta = 0.1
        val applicableSS = methods.filter { it.value.applicable }
        val total = Array(applicableSS.size){i -> i+1 }.sum()
        applicableSS.asSequence().sortedBy {  it.value.improved }.forEachIndexed { index, ss ->
            ss.value.probability = ss.value.probability * (1 - delta) + delta * (index + 1)/total
        }
        update()
        return getStrategyWithItsProbability()
    }

    /**
     * probability is assigned adaptively regarding Archive,
     */
    private fun relyOnConArchive() : ResourceSamplingMethod{

        val focusedStrategy = 1.0
        val passed = time.percentageUsedBudget()
        val threshold = config.focusedSearchActivationTime

        val used = passed/threshold
        resetProbability()
        if(used < CONARCHIVE_THRESHOLD){
            val one = methods.filter { it.value.applicable && (it.key == S1iR || it.key == S1dR)}.size
            val two = methods.count { it.value.applicable } - one
            methods.filter { it.value.applicable }.forEach { s->
                when(s.key){
                    S1iR -> s.value.probability = focusedStrategy/one
                    S1dR -> s.value.probability = focusedStrategy/one
                    else -> s.value.probability = (1.0 - focusedStrategy)/two
                }
            }
            return getStrategyWithItsProbability()
        }else{
            initEqualProbability()
        }

        return relyOnArchive()
    }

    private fun getStrategyWithItsProbability(): ResourceSamplingMethod{
        validateProbability()
        val result = methods.filter { it.value.applicable }.run {
            randomWithProbability(this.keys.toTypedArray(), this.values.map { it.probability }.toTypedArray())
        }
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
        methods.forEach {
            it.value.probability = 0.0
        }
    }

    fun reportImprovement(samplerSpecification: SamplerSpecification){
        //it may be null when executing ad-hoc rest action
        methods.filter { it.key.toString() == samplerSpecification.methodKey }.values.forEach {
            it.improved +=1
        }
    }

    fun getApplicableMethods() = methods.filterValues { it.applicable }.keys

    /**
     * MethodApplicationInfo presents detailed information about how to sample for methods defined [ResourceSamplingMethod],
     * including 1) [applicable] a set of applicable strategies; 2) [probability] a probability to select an applicable strategy; 3) [times] times to select an applicable strategy; and 4) [improved] times to help to improve Archive
     *
     * @property applicable presents whether a strategy is applicable for a system under test, e.g., if all resource of the SUT are independent, then only [S1iR] is applicable
     * @property probability presents a probability to apply the strategy during sampling phase
     * @property times presents how many times the strategy is applied
     * @property improved presents how many times the strategy helps to improve Archive. Note that improved <= times
     *
     * */
    class MethodApplicationInfo(var applicable : Boolean = false, var probability : Double = 0.0, var times : Int = 0, var improved : Int = 0)
}


enum class ResourceSamplingMethod {
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

