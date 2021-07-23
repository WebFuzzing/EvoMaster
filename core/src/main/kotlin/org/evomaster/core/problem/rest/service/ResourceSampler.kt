package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.httpws.service.auth.NoAuth
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.resource.SamplerSpecification
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * resource-based sampler
 * the sampler handles resource-based rest individual
 */
open class ResourceSampler : AbstractRestSampler() {

    companion object {
        val log: Logger = LoggerFactory.getLogger(ResourceSampler::class.java)
    }

    @Inject
    private lateinit var ssc : ResourceSampleMethodController

    @Inject
    private lateinit var rm : ResourceManageService

    @Inject
    private lateinit var dm : ResourceDepManageService

    override fun initSqlInfo(infoDto: SutInfoDto) {
        //when ResourceDependency is enabled, SQL info is required to identify dependency
        if (infoDto.sqlSchemaDto != null && (configuration.shouldGenerateSqlData() || config.isEnabledResourceDependency())) {

            sqlInsertBuilder = SqlInsertBuilder(infoDto.sqlSchemaDto, rc)
            existingSqlData = sqlInsertBuilder!!.extractExistingPKs()
        }
    }

    override fun initAdHocInitialIndividuals() {

        rm.initResourceNodes(actionCluster, sqlInsertBuilder)

        adHocInitialIndividuals.clear()

        rm.createAdHocIndividuals(NoAuth(),adHocInitialIndividuals)

        authentications.forEach { auth ->
            rm.createAdHocIndividuals(auth, adHocInitialIndividuals)
        }
    }

    override fun postInits() {
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
            val call = sampleRandomResourceAction(0.05, left)
            left -= call.seeActionSize(ActionFilter.NO_SQL)
            restCalls.add(call)
        }

        val ind = RestIndividual(
                resourceCalls = restCalls, sampleType = SampleType.RANDOM, dbInitialization = mutableListOf(), trackOperator = this, index = time.evaluatedIndividuals)
        return ind
    }


    private fun sampleRandomResourceAction(noAuthP: Double, left: Int) : RestResourceCalls{
        val r = randomness.choose(rm.getResourceCluster().filter { it.value.isAnyAction() })
        val rc = if (randomness.nextBoolean()) r.sampleOneAction(null, randomness) else r.randomRestResourceCalls(randomness,left)
        rc.seeActions(ActionFilter.NO_SQL).forEach {
            if(it is RestCallAction){
                it.auth = getRandomAuth(noAuthP)
            }
        }
        return rc
    }

    override fun smartSample(): RestIndividual {

        /*
            At the beginning, sampleAll from this set, until it is empty
         */
        if (adHocInitialIndividuals.isNotEmpty()) {
            return adHocInitialIndividuals.removeAt(0)
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
            restCalls.flatMap { it.seeActions(ActionFilter.NO_SQL) }.forEach {
                if(it is RestCallAction)
                    it.auth = auth
            }
        }else{
            val auth = NoAuth()
            restCalls.flatMap { it.seeActions(ActionFilter.NO_SQL) }.forEach {
                if(it is RestCallAction)
                    it.auth = auth
            }
        }

        if (restCalls.isNotEmpty()) {
            val individual =  RestIndividual(restCalls, SampleType.SMART_RESOURCE, sampleSpec = SamplerSpecification(sampleMethod.toString(), withDependency),
                    trackOperator = if(config.trackingEnabled()) this else null, index = if (config.trackingEnabled()) time.evaluatedIndividuals else -1)
            if (withDependency)
                dm.sampleResourceWithRelatedDbActions(individual, rm.getSqlMaxNumOfResource())

            individual.cleanBrokenBindingReference()
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
            size -= resourceCalls.last().seeActionSize(ActionFilter.NO_SQL)
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
            size -= resourceCalls.last().seeActionSize(ActionFilter.NO_SQL)
            executed.add(key)
        }
    }
}