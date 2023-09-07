package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.problem.ExternalServiceDto
import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.EMConfig
import org.evomaster.core.output.service.PartialOracles
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.externalservice.ExternalService
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceInfo
import org.evomaster.core.problem.externalservice.httpws.service.HttpWsExternalServiceHandler
import org.evomaster.core.problem.httpws.service.HttpWsSampler
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.RestActionBuilderV3.buildActionBasedOnUrl
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.problem.rest.seeding.Parser
import org.evomaster.core.problem.rest.seeding.postman.PostmanParser
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.tracer.Traceable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct


abstract class AbstractRestSampler : HttpWsSampler<RestIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AbstractRestSampler::class.java)
    }

    @Inject
    protected lateinit var configuration: EMConfig

    @Inject
    protected lateinit var partialOracles: PartialOracles

    protected val adHocInitialIndividuals: MutableList<RestIndividual> = mutableListOf()

    lateinit var swagger: OpenAPI
        protected set

    private lateinit var infoDto: SutInfoDto

    // TODO: This will moved under ApiWsSampler once RPC and GraphQL support is completed
    @Inject
    protected lateinit var externalServiceHandler: HttpWsExternalServiceHandler

    @PostConstruct
    open fun initialize() {

        log.debug("Initializing {}", AbstractRestSampler::class.simpleName)

        if(configuration.blackBox && !configuration.bbExperiments){
            initForBlackBox()
            return
        }

        rc.checkConnection()

        val started = rc.startSUT()
        if (!started) {
            throw SutProblemException("Failed to start the system under test")
        }

        infoDto = rc.getSutInfo()
                ?: throw SutProblemException("Failed to retrieve the info about the system under test")

        val problem = infoDto.restProblem
                ?: throw java.lang.IllegalStateException("Missing problem definition object")

        val openApiURL = problem.openApiUrl
        val openApiSchema = problem.openApiSchema

        if(!openApiURL.isNullOrBlank()) {
            swagger = OpenApiAccess.getOpenAPIFromURL(openApiURL)
        } else if(! openApiSchema.isNullOrBlank()){
            swagger = OpenApiAccess.getOpenApi(openApiSchema)
        } else {
            throw SutProblemException("No info on the OpenAPI schema was provided")
        }

        if (swagger.paths == null) {
            throw SutProblemException("There is no endpoint definition in the retrieved Swagger file")
        }

        actionCluster.clear()
        val skip = EndpointFilter.getEndpointsToSkip(config, swagger, infoDto)
        RestActionBuilderV3.addActionsFromSwagger(swagger, actionCluster, skip, enableConstraintHandling = config.enableSchemaConstraintHandling)

        if(config.extraQueryParam){
            addExtraQueryParam(actionCluster)
        }
        if(config.extraHeader){
            addExtraHeader(actionCluster)
        }

        setupAuthentication(infoDto)
        initSqlInfo(infoDto)

        initExternalServiceInfo(infoDto)

        // TODO: temp
        if (problem.servicesToNotMock != null) {
            registerExternalServicesToSkip(problem.servicesToNotMock)
        }

        initAdHocInitialIndividuals()

        if (config.seedTestCases)
            initSeededTests()

        postInits()

        updateConfigBasedOnSutInfoDto(infoDto)

        /*
            TODO this would had been better handled with optional injection, but Guice seems pretty buggy :(
         */
        partialOracles.setupForRest(swagger, config)

        log.debug("Done initializing {}", AbstractRestSampler::class.simpleName)
    }

    private fun addExtraQueryParam(actionCluster: Map<String, Action>){

        val key = TaintInputName.EXTRA_PARAM_TAINT

        actionCluster.values.forEach {
            (it as RestCallAction).addParam(QueryParam(key,
                CustomMutationRateGene(key,
                    OptionalGene(
                        key,
                        CustomMutationRateGene(key, StringGene(key, "42"), 0.0),
                        searchPercentageActive = config.searchPercentageExtraHandling
                    ),
                    probability = 1.0,
                    searchPercentageActive = config.searchPercentageExtraHandling
                )
            ))
        }
    }

    private fun addExtraHeader(actionCluster: Map<String, Action>){

        val key = TaintInputName.EXTRA_HEADER_TAINT

        actionCluster.values.forEach {
            (it as RestCallAction).addParam(HeaderParam(key,
                CustomMutationRateGene(key,
                    OptionalGene(
                        key,
                        CustomMutationRateGene(key, StringGene(key, "42"), 0.0),
                        searchPercentageActive = config.searchPercentageExtraHandling
                    ),
                    probability = 1.0,
                    searchPercentageActive = config.searchPercentageExtraHandling
                )
            ))
        }
    }


    /**
     * create AdHocInitialIndividuals
     */
    fun initAdHocInitialIndividuals(){
        customizeAdHocInitialIndividuals()
    }

    override fun initSeededTests(infoDto: SutInfoDto?) {
        // if test case seeding is enabled, add those test cases too
        if (!config.seedTestCases) {
            throw IllegalStateException("'seedTestCases' should be true when initializing seeded tests")
        }
        val parser = getParser()
        val seededTestCases = parser.parseTestCases(config.seedTestCasesPath)
        seededIndividuals.addAll(seededTestCases.map {
            it.forEach { a -> a.doInitialize() }
            createIndividual(SampleType.SEEDED, it)
        })
    }


    override fun getPreDefinedIndividuals() : List<RestIndividual>{
        val addCallAction = addCallToSwagger() ?: return listOf()
        addCallAction.doInitialize()
        return listOf(createIndividual(SampleType.PREDEFINED,mutableListOf(addCallAction)))
    }

    open fun getExcludedActions() : List<RestCallAction>{
        val addCallAction = addCallToSwagger() ?: return listOf()
        return listOf(addCallAction)
    }

    /*
        This is mainly done for the coverage of the generated tests, in case there are targets
        on the endpoint from which the schema is fetched from.
        particularly important for NodeJS applications
     */
    private fun addCallToSwagger() : RestCallAction?{

        val id =  "Call to Swagger"

        if (configuration.blackBox && !configuration.bbExperiments) {
            return if (configuration.bbSwaggerUrl.startsWith("http", true)){
                buildActionBasedOnUrl(BlackBoxUtils.targetUrl(config,this), id, HttpVerb.GET, configuration.bbSwaggerUrl, true)
            } else
                null
        }

        val base = infoDto.baseUrlOfSUT
        val openapi = infoDto.restProblem.openApiUrl

        if(openapi == null || !openapi.startsWith(base)){
            /*
                We only make the call if schema is on same host:port of the API,
                ie coming from SUT. Otherwise would not be much of the point.
             */
            return null
        }

        return buildActionBasedOnUrl(base, id, HttpVerb.GET, openapi, true)
    }

    /**
     * customize AdHocInitialIndividuals
     */
    abstract fun customizeAdHocInitialIndividuals()

    /**
     * post action after InitialIndividuals are crated
     */
    open fun postInits(){
        //do nothing
    }

    override fun resetSpecialInit() {
        initAdHocInitialIndividuals()
    }



    private fun initForBlackBox() {

        swagger = OpenApiAccess.getOpenAPIFromURL(configuration.bbSwaggerUrl)
        if (swagger.paths == null) {
            throw SutProblemException("There is no endpoint definition in the retrieved Swagger file")
        }

        // ONUR: Add all paths to list of paths to ignore except endpointFocus
        val endpointsToSkip = EndpointFilter.getEndPointsToSkip(config,swagger);

        actionCluster.clear()

        // ONUR: Rather than an empty list, give the list of endpoints to skip.
        RestActionBuilderV3.addActionsFromSwagger(swagger, actionCluster, endpointsToSkip, enableConstraintHandling = config.enableSchemaConstraintHandling)

        initAdHocInitialIndividuals()
        if (config.seedTestCases)
            initSeededTests()

        addAuthFromConfig()

        /*
            TODO this would had been better handled with optional injection, but Guice seems pretty buggy :(
         */
        partialOracles.setupForRest(swagger, config)

        log.debug("Done initializing {}", AbstractRestSampler::class.simpleName)
    }

    fun getOpenAPI(): OpenAPI{
        return swagger
    }

    override fun hasSpecialInitForSmartSampler(): Boolean {
        return (adHocInitialIndividuals.isNotEmpty() && config.isEnabledSmartSampling())
    }

    /**
     * @return size of adHocInitialIndividuals
     */
    fun getSizeOfAdHocInitialIndividuals() = adHocInitialIndividuals.size

    /**
     * @return a list of adHocInitialIndividuals which have not been executed yet
     *
     * it is only used for debugging
     */
    fun getNotExecutedAdHocInitialIndividuals() = adHocInitialIndividuals.toList()

    /**
     * @return a created individual with specified actions, i.e., [restCalls].
     * All actions must have been already initialized
     */
    open fun createIndividual(sampleType: SampleType, restCalls: MutableList<RestCallAction>): RestIndividual {
        if(restCalls.any { !it.isInitialized() }){
            throw IllegalArgumentException("Action is not initialized")
        }
        val ind =  RestIndividual(restCalls, sampleType, mutableListOf()//, usedObjects.copy()
                ,trackOperator = if (config.trackingEnabled()) this else null, index = if (config.trackingEnabled()) time.evaluatedIndividuals else Traceable.DEFAULT_INDEX)
        ind.doInitializeLocalId()
//        ind.computeTransitiveBindingGenes()
        org.evomaster.core.Lazy.assert { ind.isInitialized() }
        return ind
    }

    private fun getParser(): Parser {
        return when(config.seedTestCasesFormat) {
            EMConfig.SeedTestCasesFormat.POSTMAN -> PostmanParser(seeAvailableActions().filterIsInstance<RestCallAction>(), swagger)
        }
    }

    /**
     * To collect external service info through SutInfoDto
     */
    private fun initExternalServiceInfo(info: SutInfoDto) {
        if (info.bootTimeInfoDto?.externalServicesDto != null) {
            info.bootTimeInfoDto.externalServicesDto.forEach {
                externalServiceHandler.addExternalService(
                    HttpExternalServiceInfo(
                        it.protocol,
                        it.remoteHostname,
                        it.remotePort
                )
                )
            }
        }
    }

    private fun registerExternalServicesToSkip(services: List<ExternalServiceDto>) {
        services.forEach {
            externalServiceHandler.registerExternalServiceToSkip(
                ExternalService(
                it.hostname,
                it.port
            )
            )
        }
    }

}
