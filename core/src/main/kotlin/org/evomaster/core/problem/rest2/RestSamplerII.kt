package org.evomaster.core.problem.rest.serviceII

import com.google.inject.Inject
import io.swagger.models.Swagger
import io.swagger.parser.SwaggerParser
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.auth.AuthenticationHeader
import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.serviceII.resources.RestAResource
import org.evomaster.core.problem.rest.serviceII.resources.RestResourceCalls
import org.evomaster.core.problem.rest2.resources.ResourceManageService
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.Sampler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ConnectException
import javax.annotation.PostConstruct
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


class RestSamplerII : Sampler<RestIndividualII>() {

    companion object {
        val log: Logger = LoggerFactory.getLogger(RestSamplerII::class.java)
    }
    @Inject
    private lateinit var rc: RemoteController

    @Inject
    private lateinit var configuration: EMConfig

    @Inject
    private lateinit var ssc : SmartSamplingController

    @Inject
    private lateinit var rm : ResourceManageService


    private val authentications: MutableList<AuthenticationInfo> = mutableListOf()

    private val adHocInitialIndividuals: MutableList<RestIndividualII> = mutableListOf()

    var sqlInsertBuilder: SqlInsertBuilder? = null
    var existingSqlData : List<DbAction> = listOf()
        private set

    private val samplingResourceCounter : MutableMap<String , Int> = mutableMapOf()
    private val samplingComResourceCounter : MutableMap<String , Int> = mutableMapOf()
    private val separatorResources = "::"

    @PostConstruct
    private fun initialize() {

        log.debug("Initializing {}", RestSamplerII::class.simpleName)

        rc.checkConnection()

        val started = rc.startSUT()
        if (!started) {
            throw SutProblemException("Failed to start the system under test")
        }

        val infoDto = rc.getSutInfo()
                ?: throw SutProblemException("Failed to retrieve the info about the system under test")

        val swagger = getSwagger(infoDto)
        if (swagger.paths == null) {
            throw SutProblemException("There is no endpoint definition in the retrieved Swagger file")
        }
        actionCluster.clear()

        RestActionBuilder.addActionsFromSwagger(swagger, actionCluster, infoDto.restProblem?.endpointsToSkip ?: listOf())

        setupAuthentication(infoDto)

        if (infoDto.sqlSchemaDto != null && configuration.shouldGenerateSqlData()) {

            sqlInsertBuilder = SqlInsertBuilder(infoDto.sqlSchemaDto, rc)
            existingSqlData = sqlInsertBuilder!!.extractExistingPKs()
        }

        if(config.smartSamplingStrategy == EMConfig.SmartSamplingStrategy.RESOURCES){
            if(config.allowDataFromDB){
                if(infoDto.sqlSchemaDto != null && sqlInsertBuilder == null)
                    sqlInsertBuilder = SqlInsertBuilder(infoDto.sqlSchemaDto, rc)
            }

            initAbstractResources()
            initAdHocInitialIndividuals()
            ssc.initialize()
        }

        log.debug("Done initializing {}", RestSamplerII::class.simpleName)
        /*
            FIXME MAN
         */
        RestAResource.CONFIG_MAX_TEST_SIZE = config.maxTestSize
    }



    private fun setupAuthentication(infoDto: SutInfoDto) {

        val info = infoDto.infoForAuthentication ?: return

        info.forEach { i ->
            if (i.name == null || i.name.isBlank()) {
                log.warn("Missing name in authentication info")
                return@forEach
            }

            val headers: MutableList<AuthenticationHeader> = mutableListOf()

            i.headers.forEach loop@{ h ->
                val name = h.name?.trim()
                val value = h.value?.trim()
                if (name == null || value == null) {
                    log.warn("Invalid header in ${i.name}")
                    return@loop
                }

                headers.add(AuthenticationHeader(name, value))
            }

            val auth = AuthenticationInfo(i.name.trim(), headers)

            authentications.add(auth)
        }
    }

    private fun getSwagger(infoDto: SutInfoDto): Swagger {

        val swaggerURL = infoDto?.restProblem?.swaggerJsonUrl ?: throw IllegalStateException("Missing information about the Swagger URL")

        val response = connectToSwagger(swaggerURL, 30)

        if (!response.statusInfo.family.equals(Response.Status.Family.SUCCESSFUL)) {
            throw SutProblemException("Cannot retrieve Swagger JSON data from $swaggerURL , status=${response.status}")
        }

        val json = response.readEntity(String::class.java)

        val swagger = try {
            SwaggerParser().parse(json)
        } catch (e: Exception) {
            throw SutProblemException("Failed to parse Swagger JSON data: $e")
        }

        return swagger
    }

    private fun connectToSwagger(swaggerURL: String, attempts: Int): Response {

        for (i in 0 until attempts) {
            try {
                return ClientBuilder.newClient()
                        .target(swaggerURL)
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get()
            } catch (e: Exception) {

                if (e.cause is ConnectException) {
                    /*
                        Even if SUT is running, Swagger service might not be ready
                        yet. So let's just wait a bit, and then retry
                    */
                    Thread.sleep(1_000)
                } else {
                    throw IllegalStateException("Failed to connect to $swaggerURL: ${e.message}")
                }
            }
        }

        throw IllegalStateException("Failed to connect to $swaggerURL")
    }


    //FIXME random RestIndividualII
    override fun sampleAtRandom() : RestIndividualII{
        return sampleAtRandom(randomness.nextInt(1, config.maxTestSize))
    }

    private fun sampleAtRandom(size : Int): RestIndividualII {
        assert(size <= config.maxTestSize)
        val restCalls = mutableListOf<RestResourceCalls>()
        val n = randomness.nextInt(1, config.maxTestSize)

        var left = n
        while(left > 0){
            var call = sampleRandomResourceAction(0.05, left)
            left -= call.actions.size
            restCalls.add(call)
        }
        return RestIndividualII(restCalls, SampleType.RANDOM)
    }

//    private fun sampleRandomResourceAction(noAuthP: Double, left: Int) : RestResourceCalls{
//        val r = randomness.choose(rm.getResourceCluster())
//        val rc = if (randomness.nextBoolean()) r.sampleOneAction(null, randomness, left) else r.randomRestResourceCalls(randomness,left)
//        rc.actions.forEach {
//            if(it is RestCallAction){
//                it.auth = getRandomAuth(noAuthP)
//            }
//        }
//        return rc
//    }

    private fun sampleRandomResourceAction(noAuthP: Double, left: Int) : RestResourceCalls{
        val r = randomness.choose(rm.getResourceCluster().filter { it.value.isAnyAction() })
        val rc = if (randomness.nextBoolean()) r.sampleOneAction(null, randomness, left) else r.randomRestResourceCalls(randomness,left)
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

    override fun smartSample(): RestIndividualII {

        /*
            At the beginning, sampleAll from this set, until it is empty
         */
        if (!adHocInitialIndividuals.isEmpty()) {
            val ind = adHocInitialIndividuals.removeAt(0)
            return ind
        }

        val restCalls = mutableListOf<RestResourceCalls>()

        val withDependency = config.probOfEnablingResourceDependencyHeuristics > 0.0
                    && rm.isDependencyNotEmpty()
                    && randomness.nextBoolean(config.probOfEnablingResourceDependencyHeuristics)

        val strategy = ssc.getSampleStrategy()
        when(strategy){
            SmartSamplingController.SampleStrategy.S1iR -> sampleIndependentAction(restCalls)
            SmartSamplingController.SampleStrategy.S1dR -> sampleOneResource(restCalls)
            SmartSamplingController.SampleStrategy.S2dR -> sampleComResource(restCalls, withDependency)
            SmartSamplingController.SampleStrategy.SMdR -> sampleManyResources(restCalls, withDependency)
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

        val dependencies = if(withDependency && restCalls.size > 1)
                                restCalls.map { it.resource.ar.path.toString() }.toHashSet()
                        else mutableSetOf<String>()

        if (!restCalls.isEmpty()) {
            return if(config.enableTrackIndividual || config.enableTrackEvaluatedIndividual){
                RestIndividualII(restCalls,SampleType.SMART_RESOURCE.also { it.description = strategy.name },  dependencies, appendMethod(strategy.name), mutableListOf())
            }else RestIndividualII(restCalls,SampleType.SMART_RESOURCE.also { it.description = strategy.name }, dependencies)
        }

        return sampleAtRandom()
    }


    private fun sampleIndependentAction(resourceCalls: MutableList<RestResourceCalls>){
        val key = selectAResource(randomness)
        rm.sampleCall(key, false, resourceCalls, config.maxTestSize)
    }

    private fun sampleOneResource(resourceCalls: MutableList<RestResourceCalls>){
        val key = selectAIndResourceHasNonInd(randomness)
        rm.sampleCall(key, true, resourceCalls, config.maxTestSize)

    }

    private fun sampleComResource(resourceCalls: MutableList<RestResourceCalls>, withDependency : Boolean){
        if(withDependency){
            rm.sampleRelatedResources(resourceCalls, 2, config.maxTestSize)
        }else{
            sampleRandomComResource(resourceCalls)
        }

    }

    private fun sampleManyResources(resourceCalls: MutableList<RestResourceCalls>, withDependency: Boolean){
        if(withDependency){
            val num = randomness.nextInt(3, config.maxTestSize)
            rm.sampleRelatedResources(resourceCalls, num, config.maxTestSize)
        }else{
            sampleManyResources(resourceCalls)
        }
    }

    private fun selectAResource(randomness: Randomness) : String{
        return randomness.choose(rm.getResourceCluster().filter { r -> r.value.isAnyAction() }.keys)
    }

    private fun selectAIndResourceHasNonInd(randomness: Randomness) : String{
        return randomness.choose(rm.getResourceCluster().filter { r -> r.value.isAnyAction() && !r.value.isIndependent() }.keys)
    }

    private fun selectAComResource(randomness: Randomness) : String{
        val skiped = rm.getResourceCluster().filter { r -> !r.value.isAnyAction() }.keys
        val candidates = samplingComResourceCounter.filterNot{ r-> r.key.split(separatorResources).any{ir-> skiped.contains(ir)} }
        if(candidates.isEmpty())
            throw java.lang.IllegalStateException("there is no any com-resource available")
        return randomness.choose(candidates.keys)

    }

    private fun initAdHocInitialIndividuals() {
        adHocInitialIndividuals.clear()

        rm.createAdHocIndividuals(NoAuth(),adHocInitialIndividuals)
        authentications.forEach { auth ->
            rm.createAdHocIndividuals(auth, adHocInitialIndividuals)
        }
    }

    private fun initAbstractResources(){
        rm.initAbstractResources(actionCluster)

        rm.getResourceCluster().keys.forEach { k->
            samplingResourceCounter.getOrPut(k){0}
        }

        rm.getResourceCluster().filter { !it.value.isIndependent() }.keys.forEach { k ->
            rm.getResourceCluster().filter { !it.value.isIndependent() }.keys.forEach { k1 ->
                if(k != k1){
                    samplingComResourceCounter.getOrPut(k+separatorResources+k1){0}
                }
            }
        }
    }

    override fun hasSpecialInit(): Boolean {
        return !adHocInitialIndividuals.isEmpty() && config.probOfSmartSampling > 0
    }

    override fun resetSpecialInit() {
        initAdHocInitialIndividuals()
    }


    override fun feedback(betterResult: Boolean) {
        if(betterResult) ssc.reportImprovement()
    }


    private fun updateSamplingResourceCounter(actions: List<RestAction>) {
        actions
                .filter {
                    a -> (a is RestCallAction) && actions.find { ia -> (ia is RestCallAction) && a.path.toString() != ia.path.toString() &&a.path.isAncestorOf(ia.path) } == null
                }!!
                .map {
                    a -> (a as RestCallAction).path.toString()
                }.toHashSet()
                .forEach {
                    samplingResourceCounter.replace(it, samplingResourceCounter.getValue(it) + 1)
                }
    }

    private fun sampleRandomComResource(resourceCalls: MutableList<RestResourceCalls>){
        val keys = selectAComResource(randomness)
        //samplingComResourceCounter.getValue(keys).plus(1)
        var size = config.maxTestSize
        var num = 0
        for (key in keys.split(separatorResources)){
            //val temp = rm.getResourceCluster().getValue(key)
            //resourceCalls.add(temp.sampleRestResourceCalls(randomness, size))
            rm.sampleCall(key, true, resourceCalls, config.maxTestSize)
            //samplingResourceCounter.getValue(key).plus(1)
            size -= resourceCalls.last().actions.size
            num++
        }
    }

    private fun sampleManyResources(resourceCalls: MutableList<RestResourceCalls>){
        val executed = mutableListOf<String>()
        var size = randomness.nextInt(3, config.maxTestSize)
        val candR = rm.getResourceCluster().filter { r -> r.value.isAnyAction() }
        var num = 0
        while(size > 1 && executed.size < candR.size){
            val key = randomness.choose(candR.keys.filter { !executed.contains(it) })
            rm.sampleCall(key, true, resourceCalls, config.maxTestSize)
            //resourceCalls.add(candR.getValue(key).sampleRestResourceCalls(randomness, size))
            size -= resourceCalls.last().actions.size
            executed.add(key)
            num++
        }
    }


//    fun handleAddedRestIndividualII(ind : RestIndividualII) : MutableList<RestIndividualII>{
//        //precondition calls.actions.size > 1 && calls.actions.size < resource.actions.size
//        val dind = mutableListOf<RestIndividualII>()
//        if(ind.getResourceCalls().size == 1 && ind.getResourceCalls()[0].actions.size > 1){
//            val call = ind.getResourceCalls()[0]
//            if(call.resource.ar.actions.size > 1 && call.actions.size in 1..(call.resource.ar.actions.size-1)){
//                call.resource.ar.handleAdded(call)?.let {
//                    dind.add(RestIndividualII( mutableListOf(it), SampleType.SMART_RESOURCE))
//                }
//            }
//        }else if(ind.getResourceCalls().size == 2){
//            //two resource
//            ind.getResourceCalls().forEach {call->
//                if(call.resource.ar.actions.size > 1 && call.actions.size in 1..(call.resource.ar.actions.size-1)){
//                    call.resource.ar.handleAdded(call)?.let {
//                        dind.add(RestIndividualII(mutableListOf(it), SampleType.SMART_RESOURCE))
//                    }
//                }
//            }
//
//            val firstCall = ind.getResourceCalls()[0]
//            val multiCalls = mutableListOf<RestResourceCalls>()
//            var mfirstCall: RestResourceCalls? = null
//            if((firstCall.actions.last() as RestCallAction).verb == HttpVerb.DELETE){
//                mfirstCall = firstCall.resource.ar.createCallByVerb(HttpVerb.POST, firstCall.resource)
//            }else if((firstCall.actions.last() as RestCallAction).verb == HttpVerb.POST){
//                mfirstCall = firstCall.resource.ar.createCallByVerb(HttpVerb.DELETE, firstCall.resource)
//            }
//            if(mfirstCall != null){
//                multiCalls.add(mfirstCall)
//                multiCalls.add(ind.getResourceCalls()[1].copy())
//                dind.add(RestIndividualII( multiCalls, SampleType.SMART_RESOURCE))
//            }
//
//        }
//        return dind
//    }
}