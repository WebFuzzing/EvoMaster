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
    private lateinit var sampler: RestSamplerII

    @Inject
    protected lateinit var randomness: Randomness

    @Inject
    protected lateinit var config : EMConfig

    private var select: SampleStrategy = SampleStrategy.S1iR

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
        println( SampleStrategy.values().filter { it.applicable }.map { "$it : ${it.probability}"}.joinToString (" - "))
    }

    private fun printSummaryOfResources(mutableMap: MutableMap<String, RestAResource>){
        println("Summary of abstract resources and actions>>>")
        val message ="""
            #Rs ${mutableMap.size}
            #IndRs ${mutableMap.values.filter { it.independent }.size}
            #hasIndActionRs ${mutableMap.values.filter { it.hasIndependentAction }.size}
            #DepRs ${mutableMap.values.filterNot { it.independent }.size}

            #Actions ${mutableMap.values.map { it.actions.size }.sum()}
            #IndActions ${mutableMap.values.map { it.templates.filter { it.value.independent }.size }.sum()}
            #depActions ${mutableMap.values.map { it.templates.filter { !it.value.independent }.size}.sum()}
            #depComActions ${mutableMap.values.map { it.templates.filter { !it.value.independent }.size * (it.templates.filter { !it.value.independent }.size -1) }.sum()}
            """
        println(message)
    }

    fun initProbability(mutableMap: MutableMap<String, RestAResource>){
        printSummaryOfResources(mutableMap)
        when(config.sampleControl){
            EMConfig.ResourceSamplingControl.RANDOM -> SampleStrategy.values().filter { it.applicable }.let {
                l -> l.forEach { s -> s.probability = 1.0/l.size }
            }
            EMConfig.ResourceSamplingControl.BasedOnSpecified -> initProbabilityWithSpecified()
            EMConfig.ResourceSamplingControl.BasedOnActions -> initProbabilityWithActions(mutableMap)
            EMConfig.ResourceSamplingControl.BasedOnTimeBudgets -> TODO()
            EMConfig.ResourceSamplingControl.BasedOnArchive -> TODO()
        }

        printApplicableStr()
    }

    fun printCounters(){
        println(SampleStrategy.values().map { it.times }.joinToString("-"))
    }

    fun selectImproved(){
        if(SampleStrategy.values().map { it.times }.sum() != 0) {
            select.improved += 1
        }
    }

    fun printImproved(){
        println("improvement with selected strategy: "+SampleStrategy.values().map { it.improved }.joinToString("-"))
    }

    fun printImprovedPercentage(){
        println(SampleStrategy.values().map { it.improved * 1.0/it.times }.joinToString("-"))
    }

    fun getSampleStrategy() : SampleStrategy{
        when(config.sampleControl){
            EMConfig.ResourceSamplingControl.RANDOM -> select = random()
            EMConfig.ResourceSamplingControl.BasedOnSpecified -> select =  relyOnSpecified()
            EMConfig.ResourceSamplingControl.BasedOnActions -> select =  relyOnActions()
            EMConfig.ResourceSamplingControl.BasedOnTimeBudgets -> select =  relyOnTB()
            EMConfig.ResourceSamplingControl.BasedOnArchive -> select =  relyOnArchive()
        }
        select!!.times += 1
        return select!!
    }
    fun initProbabilityWithSpecified(){
        val specified = arrayOf(0.2, 0.4, 0.4, 0.0)
        SampleStrategy.values().forEachIndexed { index, sampleStrategy ->
            sampleStrategy.probability = specified[index]
        }
    }

    //FIXME it needs to further modify
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

    private fun random() : SampleStrategy{
        return randomness.choose(SampleStrategy.values().filter { it.applicable }.toList())
    }

    private fun initProbabilityWithActions(mutableMap: MutableMap<String, RestAResource>){
        val times = 2
        val numOfDepActions = mutableMap.values.map { it.numOfDepTemplate() }.sum()
        val num = mutableMap.values.map { it.numOfTemplates() }.sum()
        val pInd = (num - numOfDepActions ) * 1.0 / (num + numOfDepActions * (times - 1))

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

    private fun relyOnSpecified() : SampleStrategy{
        return randomWithProbability(SampleStrategy.values().filter { it.applicable }.toTypedArray(), SampleStrategy.values().filter { it.applicable }.map { it.probability }.toTypedArray()) as SampleStrategy
    }
    private fun relyOnActions() : SampleStrategy{
        return randomWithProbability(SampleStrategy.values().filter { it.applicable }.toTypedArray(), SampleStrategy.values().filter { it.applicable }.map { it.probability }.toTypedArray()) as SampleStrategy
    }

    private fun relyOnTB() : SampleStrategy{
        TODO()
    }

    private fun relyOnArchive() : SampleStrategy{
        TODO()
    }

    enum class SampleStrategy (var applicable : Boolean = false, var probability : Double = 0.25, var times : Int = 0, var improved : Int = 0){
        /*
        * in OneAction sampleStrategy, one post is ignored. In other word, we only handle one action with a randomized (nonexistent) resource
        * */
        S1iR,
        /*
        * in OneResource sampleStrategy, we handle actions starting with a post, but we only execute post if post failed to create resources.
        * Thus, we do not need to handle one post in one action sampleStrategy.
        *
        * OneResource allows to apply when all action are executed
        * */
        S1dR,
        /*
       * in TwoResources sampleStrategy, we handle more than one resources (which also means resources must exist).
       * first resource must exist, second resource
       *
       * TwoResources allows to apply when OneResource with all templates
       * */
        S2dR,

        SMdR,
    }
}