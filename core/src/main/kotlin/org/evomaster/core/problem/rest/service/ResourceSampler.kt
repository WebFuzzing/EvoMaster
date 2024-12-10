package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.httpws.auth.HttpWsNoAuth
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.resource.SamplerSpecification
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.tracer.Traceable
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

    override fun customizeAdHocInitialIndividuals() {

        rm.initResourceNodes(actionCluster, sqlInsertBuilder)
        rm.initExcludedResourceNode(getExcludedActions())

        adHocInitialIndividuals.clear()

        rm.createAdHocIndividuals(HttpWsNoAuth(),adHocInitialIndividuals, getMaxTestSizeDuringSampler())

        authentications.getOfType(HttpWsAuthenticationInfo::class.java).forEach { auth ->
            rm.createAdHocIndividuals(auth, adHocInitialIndividuals, getMaxTestSizeDuringSampler())
        }
    }

    override fun postInits() {
        ssc.initialize()
    }

    override fun sampleAtRandom() : RestIndividual {
        return sampleAtRandom(randomness.nextInt(1, getMaxTestSizeDuringSampler()))
    }

    private fun sampleAtRandom(size : Int): RestIndividual {
        assert(size <= getMaxTestSizeDuringSampler())
        val restCalls = mutableListOf<RestResourceCalls>()
        val n = randomness.nextInt(1, getMaxTestSizeDuringSampler())

        var left = n
        while(left > 0){
            val call = sampleRandomResourceAction(0.05, left)
            left -= call.seeActionSize(ActionFilter.MAIN_EXECUTABLE)
            restCalls.add(call)
        }

        val ind = RestIndividual(
                resourceCalls = restCalls, sampleType = SampleType.RANDOM, dbInitialization = mutableListOf(), trackOperator = this, index = time.evaluatedIndividuals)
        ind.doGlobalInitialize(searchGlobalState)
//        ind.computeTransitiveBindingGenes()
        return ind
    }


    private fun sampleRandomResourceAction(noAuthP: Double, left: Int) : RestResourceCalls{
        val r = randomness.choose(rm.getResourceCluster().filter { it.value.isAnyAction() })
        val rc = if (randomness.nextBoolean()){
            r.sampleOneAction(null, randomness)
        } else{
            r.randomRestResourceCalls(randomness,left)
        }
        rc.seeActions(ActionFilter.MAIN_EXECUTABLE).forEach {
            (it as RestCallAction).auth = getRandomAuth(noAuthP)
        }
        return rc
    }

    override fun smartSample(): RestIndividual {

        /*
            At the beginning, sampleAll from this set, until it is empty
         */
        val ind = if (adHocInitialIndividuals.isNotEmpty()) {
             adHocInitialIndividuals.removeAt(0)
        } else {

            val withDependency = config.probOfEnablingResourceDependencyHeuristics > 0.0
                    && dm.isDependencyNotEmpty()
                    && randomness.nextBoolean(config.probOfEnablingResourceDependencyHeuristics)

            val method = ssc.getSampleStrategy()

            sampleWithMethodAndDependencyOption(method, withDependency)
                    ?: return sampleAtRandom()
        }
        ind.doGlobalInitialize(searchGlobalState)
        return ind
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
        val auth = if(authentications.isNotEmpty()){
            getRandomAuth(0.0)
        }else{
            HttpWsNoAuth()
        }
        restCalls.flatMap { it.seeActions(ActionFilter.MAIN_EXECUTABLE) }.forEach {
            (it as RestCallAction).auth = auth
        }

        if (restCalls.isNotEmpty()) {
            val individual =  RestIndividual(restCalls, SampleType.SMART_RESOURCE, sampleSpec = SamplerSpecification(sampleMethod.toString(), withDependency),
                    trackOperator = if(config.trackingEnabled()) this else null, index = if (config.trackingEnabled()) time.evaluatedIndividuals else -1)
            if (withDependency)
                dm.sampleResourceWithRelatedDbActions(individual, rm.getMaxNumOfResourceSizeHandling())

            individual.cleanBrokenBindingReference()
//            individual.computeTransitiveBindingGenes()
            return individual
        }
        return null
    }

    private fun sampleIndependentAction(resourceCalls: MutableList<RestResourceCalls>){
        val key = randomness.choose(rm.getResourceCluster().filter { it.value.hasIndependentAction() }.keys)
        rm.sampleCall(key, false, resourceCalls, getMaxTestSizeDuringSampler())
    }

    private fun sampleOneResource(resourceCalls: MutableList<RestResourceCalls>){
        val key = selectAIndResourceHasNonInd(randomness)
        rm.sampleCall(key, true, resourceCalls, getMaxTestSizeDuringSampler())
    }

    private fun sampleComResource(resourceCalls: MutableList<RestResourceCalls>, withDependency : Boolean){
        if(withDependency){
            dm.sampleRelatedResources(resourceCalls, 2, getMaxTestSizeDuringSampler())
        }else{
            sampleRandomComResource(resourceCalls)
        }
    }

    private fun sampleManyResources(resourceCalls: MutableList<RestResourceCalls>, withDependency: Boolean){
        if(withDependency){
            val num = randomness.nextInt(3, getMaxTestSizeDuringSampler())
            dm.sampleRelatedResources(resourceCalls, num, getMaxTestSizeDuringSampler())
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
        var size = getMaxTestSizeDuringSampler()
        keys.forEach {
            rm.sampleCall(it, true, resourceCalls, size)
            size -= resourceCalls.last().seeActionSize(ActionFilter.NO_SQL)
        }
    }

    private fun sampleManyResources(resourceCalls: MutableList<RestResourceCalls>){
        val executed = mutableListOf<String>()
        val depCand = rm.getResourceCluster().filter { r -> !r.value.isIndependent() }
        var resourceSize = randomness.nextInt(3, if(config.maxResourceSize > 0) config.maxResourceSize else 5)
        if(resourceSize > depCand.size) resourceSize = depCand.size + 1

        var size = getMaxTestSizeDuringSampler()
        val candR = rm.getResourceCluster().filter { r -> r.value.isAnyAction() }
        while(size > 1 && executed.size < resourceSize){
            val key = if(executed.size < resourceSize-1 && size > 2)
                randomness.choose(depCand.keys.filter { !executed.contains(it) })
            else if (candR.keys.any { !executed.contains(it) })
                randomness.choose(candR.keys.filter { !executed.contains(it) })
            else
                randomness.choose(candR.keys)

            rm.sampleCall(key, true, resourceCalls, size)
            size -= resourceCalls.last().seeActionSize(ActionFilter.NO_SQL)
            executed.add(key)
        }
    }

    override fun createIndividual(sampleType: SampleType, restCalls: MutableList<RestCallAction>): RestIndividual {

        val resourceCalls = restCalls.map {
            val node = rm.getResourceNodeFromCluster(it.path.toString())
            RestResourceCalls(
                    template = node.getTemplate(it.verb.toString()),
                    node = node,
                    actions = mutableListOf(it),
                    sqlActions = listOf()
            )
        }.toMutableList()
        val ind =  RestIndividual(
                resourceCalls=resourceCalls,
                sampleType = sampleType,
                trackOperator = if (config.trackingEnabled()) this else null,
                index = if (config.trackingEnabled()) time.evaluatedIndividuals else Traceable.DEFAULT_INDEX)
        ind.doGlobalInitialize(searchGlobalState)
//        ind.computeTransitiveBindingGenes()
        return ind
    }
}
