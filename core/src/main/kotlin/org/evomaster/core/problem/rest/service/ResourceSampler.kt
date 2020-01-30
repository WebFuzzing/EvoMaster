package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.resource.SamplerSpecification
import org.evomaster.core.search.Action
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.Sampler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * resource-based sampler
 * the sampler handles resource-based rest individual
 */
abstract class ResourceSampler : Sampler<RestIndividual>() {

    companion object {
        val log: Logger = LoggerFactory.getLogger(ResourceSampler::class.java)
    }

    @Inject
    private lateinit var ssc : ResourceSampleMethodController

    @Inject
    private lateinit var rm : ResourceManageService

    @Inject
    private lateinit var dm : ResourceDepManageService

    private val authentications: MutableList<AuthenticationInfo> = mutableListOf()

    private val adHocInitialIndividuals: MutableList<RestIndividual> = mutableListOf()

    private var sqlInsertBuilder: SqlInsertBuilder? = null
    var existingSqlData : List<DbAction> = listOf()
        private set

    protected fun initialize(authenticationsInfo: MutableList<AuthenticationInfo>, actions : MutableMap<String, Action>, sqlBuilder: SqlInsertBuilder?) {

        assert(config.resourceSampleStrategy != EMConfig.ResourceSamplingStrategy.NONE)

        actionCluster.clear()
        actionCluster.putAll(actions)

        authentications.clear()
        authentications.addAll(authenticationsInfo)

        sqlInsertBuilder = sqlBuilder
        if(sqlInsertBuilder != null && config.shouldGenerateSqlData()) {
            existingSqlData = sqlInsertBuilder!!.extractExistingPKs()
        }

        initAbstractResources()
        initAdHocInitialIndividuals()
        ssc.initialize()
    }


    override fun sampleAtRandom() : RestIndividual {
        return sampleAtRandom(randomness.nextInt(1, config.maxTestSize))
    }

    private fun sampleAtRandom(size : Int): RestIndividual {
        assert(size <= config.maxTestSize)
        val restCalls = mutableListOf<RestResourceCalls>()
        val n = randomness.nextInt(1, config.maxTestSize)

        var left = n
        while(left > 0){
            var call = sampleRandomResourceAction(0.05, left)
            left -= call.actions.size
            restCalls.add(call)
        }
        return RestIndividual(restCalls, SampleType.RANDOM)
    }


    private fun sampleRandomResourceAction(noAuthP: Double, left: Int) : RestResourceCalls{
        val r = randomness.choose(rm.getResourceCluster().filter { it.value.isAnyAction() })
        val rc = if (randomness.nextBoolean()) r.sampleOneAction(null, randomness) else r.randomRestResourceCalls(randomness,left)
        rc.actions.forEach {
            if(it is RestCallAction){
                it.auth = getRandomAuth(noAuthP)
            }
        }
        return rc
    }

    private fun getRandomAuth(noAuthP: Double): AuthenticationInfo {
        if (authentications.isEmpty() || randomness.nextBoolean(noAuthP)) {
            return NoAuth()
        } else {
            //if there is auth, should have high probability of using one,
            //as without auth we would do little.
            return randomness.choose(authentications)
        }
    }

    override fun smartSample(): RestIndividual {

        /*
            At the beginning, sampleAll from this set, until it is empty
         */
        if (!adHocInitialIndividuals.isEmpty()) {
            val ind = adHocInitialIndividuals.removeAt(0)
            return ind
        }

        val withDependency = config.probOfEnablingResourceDependencyHeuristics > 0.0
                    && dm.isDependencyNotEmpty()
                    && randomness.nextBoolean(config.probOfEnablingResourceDependencyHeuristics)

        val method = ssc.getSampleStrategy()

        return sampleWithMethodAndDependencyOption(method, withDependency)?:sampleAtRandom()
    }


    fun sampleWithMethodAndDependencyOption(sampleMethod : ResourceSamplingMethod, withDependency: Boolean) : RestIndividual?{
        val restCalls = mutableListOf<RestResourceCalls>()

        when(sampleMethod){
            ResourceSamplingMethod.S1iR -> sampleIndependentAction(restCalls)
            ResourceSamplingMethod.S1dR -> sampleOneResource(restCalls)
            ResourceSamplingMethod.S2dR -> sampleComResource(restCalls, withDependency)
            ResourceSamplingMethod.SMdR -> sampleManyResources(restCalls, withDependency)
        }

        //auth management
        if(authentications.isNotEmpty()){
            val auth = getRandomAuth(0.0)
            restCalls.flatMap { it.actions }.forEach {
                if(it is RestCallAction)
                    it.auth = auth
            }
        }else{
            val auth = NoAuth()
            restCalls.flatMap { it.actions }.forEach {
                if(it is RestCallAction)
                    it.auth = auth
            }
        }

        if (!restCalls.isEmpty()) {
            val individual= if(config.enableTrackIndividual || config.enableTrackEvaluatedIndividual){
                RestIndividual(restCalls, SampleType.SMART_RESOURCE, sampleSpec = SamplerSpecification(sampleMethod.toString(), withDependency), trackOperator = this,
                        traces = if(config.enableTrackIndividual) mutableListOf() else null)
            }else
                RestIndividual(restCalls, SampleType.SMART_RESOURCE, sampleSpec = SamplerSpecification(sampleMethod.toString(), withDependency))

            individual.repairDBActions(sqlInsertBuilder)
            return individual
        }
        return null
    }

    private fun sampleIndependentAction(resourceCalls: MutableList<RestResourceCalls>){
        val key = randomness.choose(rm.getResourceCluster().filter { it.value.hasIndependentAction() }.keys)//selectAResource(randomness)
        rm.sampleCall(key, false, resourceCalls, config.maxTestSize)
    }

    private fun sampleOneResource(resourceCalls: MutableList<RestResourceCalls>){
        val key = selectAIndResourceHasNonInd(randomness)
        rm.sampleCall(key, true, resourceCalls, config.maxTestSize)

    }

    private fun sampleComResource(resourceCalls: MutableList<RestResourceCalls>, withDependency : Boolean){
        if(withDependency){
            dm.sampleRelatedResources(resourceCalls, 2, config.maxTestSize)
        }else{
            sampleRandomComResource(resourceCalls)
        }
    }

    private fun sampleManyResources(resourceCalls: MutableList<RestResourceCalls>, withDependency: Boolean){
        if(withDependency){
            val num = randomness.nextInt(3, config.maxTestSize)
            dm.sampleRelatedResources(resourceCalls, num, config.maxTestSize)
        }else{
            sampleManyResources(resourceCalls)
        }
    }


    private fun selectAIndResourceHasNonInd(randomness: Randomness) : String{
        return randomness.choose(rm.getResourceCluster().filter { r -> r.value.isAnyAction() && !r.value.isIndependent() }.keys)
    }

    private fun initAdHocInitialIndividuals() {
        adHocInitialIndividuals.clear()

        rm.createAdHocIndividuals(NoAuth(),adHocInitialIndividuals)
        authentications.forEach { auth ->
            rm.createAdHocIndividuals(auth, adHocInitialIndividuals)
        }
    }

    private fun initAbstractResources(){
        rm.initResourceNodes(actionCluster, sqlInsertBuilder)
    }

    override fun hasSpecialInit(): Boolean {
        return adHocInitialIndividuals.isNotEmpty() && config.probOfSmartSampling > 0
    }

    override fun resetSpecialInit() {
        initAdHocInitialIndividuals()
    }

    override fun feedback(evi : EvaluatedIndividual<RestIndividual>) {
        if(config.resourceSampleStrategy.requiredArchive && evi.hasImprovement)
            evi.individual.sampleSpec?.let { ssc.reportImprovement(it) }
    }

    private fun sampleRandomComResource(resourceCalls: MutableList<RestResourceCalls>){
        val candidates = rm.getResourceCluster().filter { !it.value.isIndependent() }.keys
        assert(candidates.size > 1)
        val keys = randomness.choose(candidates, 2)
        var size = config.maxTestSize
        keys.forEach {
            rm.sampleCall(it, true, resourceCalls, size)
            size -= resourceCalls.last().actions.size
        }
    }

    private fun sampleManyResources(resourceCalls: MutableList<RestResourceCalls>){
        val executed = mutableListOf<String>()
        val depCand = rm.getResourceCluster().filter { r -> !r.value.isIndependent() }
        var resourceSize = randomness.nextInt(3, 5)
        if(resourceSize > depCand.size) resourceSize = depCand.size + 1

        var size = config.maxTestSize
        val candR = rm.getResourceCluster().filter { r -> r.value.isAnyAction() }
        while(size > 1 && executed.size < resourceSize){
            val key = if(executed.size < resourceSize-1 && size > 2) randomness.choose(depCand.keys.filter { !executed.contains(it) }) else randomness.choose(candR.keys.filter { !executed.contains(it) })
            rm.sampleCall(key, true, resourceCalls, size)
            size -= resourceCalls.last().actions.size
            executed.add(key)
        }
    }
}