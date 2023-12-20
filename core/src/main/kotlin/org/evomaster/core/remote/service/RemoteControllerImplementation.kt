package org.evomaster.core.remote.service

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.google.inject.Inject
import org.evomaster.client.java.controller.api.ControllerConstants
import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.client.java.controller.api.dto.database.operations.*
import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.remote.NoRemoteConnectionException
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.TcpUtils
import org.evomaster.core.search.service.SearchTimeController
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.ws.rs.ProcessingException
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * We split it from its interface, to be able to stub it away in the integration tests
 */
class RemoteControllerImplementation() : RemoteController{

    companion object {
        val log: Logger = LoggerFactory.getLogger(RemoteController::class.java)
    }

    lateinit var host: String
    var port: Int = 0

    private var computeSqlHeuristics = true

    private var extractSqlExecutionInfo = true


    @Inject
    private lateinit var config: EMConfig

    //TODO: should clean up. in few places we use RemoteController without injection
    @Inject
    private var stc: SearchTimeController? = null

    private var client: Client = ClientBuilder.newClient()

    constructor(config: EMConfig) : this(){
        this.config = config
        initialize()
    }

    constructor(host: String, port: Int, computeSqlHeuristics: Boolean, extractSqlExecutionInfo: Boolean, config: EMConfig = EMConfig()) : this() {
        if (computeSqlHeuristics && !extractSqlExecutionInfo)
            throw IllegalArgumentException("'extractSqlExecutionInfo' should be enabled when 'computeSqlHeuristics' is enabled")
        this.host = host
        this.port = port
        this.computeSqlHeuristics = computeSqlHeuristics
        this.extractSqlExecutionInfo = computeSqlHeuristics || extractSqlExecutionInfo
        this.config = config
    }


    @PostConstruct
    private fun initialize() {
        host = config.sutControllerHost
        port = config.sutControllerPort
        computeSqlHeuristics = config.heuristicsForSQL && config.isMIO()
        extractSqlExecutionInfo = config.extractSqlExecutionInfo
    }

    @PreDestroy
    private fun preDestroy() {
        close()
    }

    override fun address() = "$host:$port"

    private fun getWebTarget(): WebTarget {
        return client.target("http://$host:$port" + ControllerConstants.BASE_PATH)
    }

    private fun makeHttpCall(lambda:  () -> Response) : Response {

        return  try{
            lambda.invoke()
        } catch (e: ProcessingException){
            when {
                TcpUtils.isOutOfEphemeralPorts(e) -> {
                    /*
                    This could happen if for any reason we run out of ephemeral ports.
                    In such a case, we wait X seconds, and try again, as OS might have released ports
                    meanwhile.
                    And while we are at it, let's release any hanging network resource
                    */
                    client.close() //make sure to release any resource
                    client = ClientBuilder.newClient()

                    TcpUtils.handleEphemeralPortIssue()

                    /*
                        [non-determinism-source] Man: this might lead to non-determinism
                     */
                    lambda.invoke()
                }
                TcpUtils.isRefusedConnection(e) -> {
                    //this is BAD!!! There isn't really much we can do here... :(
                    log.error("EvoMaster Driver process is no longer responding, refusing TCP connections." +
                            " Check if its process might have crashed. Also look at its logs")
                    throw e
                }
                TcpUtils.isStreamClosed(e) || TcpUtils.isEndOfFile(e) -> {
                    /*
                        TODO: there might be a potential issue here.
                        The GET on targets is not idempotent, as we collect the "first-seen" targets
                        only once. This should be handled here, somehow...
                     */
                    log.warn("EvoMaster Driver TCP connection is having issues: '${e.cause!!.message}'." +
                            " Let's wait a bit and try again.")
                    Thread.sleep(5_000)

                    /*
                        [non-determinism-source] Man: this might lead to non-determinism
                     */
                    lambda.invoke()
                }
                else -> throw e
            }
        }
    }

    override fun close() {
        client.close()
    }

    private fun readAndCheckResponse(response: Response, msg: String) : Boolean{

        val dto = try {
            response.readEntity(object : GenericType<WrappedResponseDto<*>>() {})
        } catch (e: Exception) {
            handleFailedDtoParsing(e)
            null
        }

        return checkResponse(response, dto, msg)
    }

    private fun checkResponse(response: Response, dto: WrappedResponseDto<*>?, msg: String) : Boolean{
        if (response.statusInfo.family != Response.Status.Family.SUCCESSFUL  || dto?.error != null) {
            LoggingUtil.uniqueWarn(log, "$msg. HTTP status ${response.status}. Error: '${dto?.error}")
//            log.warn("{}. HTTP status {}. Error: '{}'", msg, response.status, dto?.error)
            return false
        }

        return true
    }

    private  fun <T> getData(dto: WrappedResponseDto<T>?) : T?{
        if (dto?.data == null) {
            log.warn("Missing DTO")
            return null
        }
        return dto.data
    }

    private fun <T> getDtoFromResponse(response: Response, type: GenericType<WrappedResponseDto<T>>) : WrappedResponseDto<T>?{

        if(response.mediaType == MediaType.TEXT_PLAIN_TYPE){
            //something weird is going on... possibly a bug in the Driver?

            val res = response.readEntity(String::class.java)
            log.error("Driver error. HTTP status ${response.status}. Error: $res")
            return null
        }


        val dto = try {
            response.readEntity(type)
        } catch (e: Exception) {
            handleFailedDtoParsing(e)
            null
        }

        return dto
    }


    override fun getSutInfo(): SutInfoDto? {

        val response = makeHttpCall {
            getWebTarget()
                    .path(ControllerConstants.INFO_SUT_PATH)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get()
        }

        val dto = getDtoFromResponse(response, object : GenericType<WrappedResponseDto<SutInfoDto>>() {})

        if(!checkResponse(response, dto, "Failed to retrieve SUT info")){
            return null
        }

        return getData(dto)
    }


    override fun getControllerInfo(): ControllerInfoDto? {

        val response = makeHttpCall {
            getWebTarget()
                    .path(ControllerConstants.CONTROLLER_INFO)
                    .queryParam(ControllerConstants.METHOD_REPLACEMENT_CATEGORIES, config.methodReplacementCategories())
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get()
        }

        val dto = getDtoFromResponse(response, object : GenericType<WrappedResponseDto<ControllerInfoDto>>() {})

        if(!checkResponse(response, dto, "Failed to retrieve EM controller info")){
            return null
        }

        return getData(dto)
    }

    private fun changeState(run: Boolean, reset: Boolean): Boolean {

        val requestDto =  SutRunDto(
            run,
            reset,
            config.enableCustomizedMethodForMockObjectHandling,
            computeSqlHeuristics,
            extractSqlExecutionInfo,
            config.methodReplacementCategories()
        )
        requestDto.advancedHeuristics = config.heuristicsForSQLAdvanced

        val response = try {
            makeHttpCall {
                getWebTarget()
                        .path(ControllerConstants.RUN_SUT_PATH)
                        .request()
                        .put(Entity.json(requestDto))
            }
        } catch (e: Exception) {
            log.warn("Failed to connect to SUT: ${e.message}")
            return false
        }

        val dto = getDtoFromResponse(response, object : GenericType<WrappedResponseDto<Any>>() {})

        return checkResponse(response, dto, "Failed to change running state of the SUT")
    }

    /*
        Starting implies a clean reset state.
        Reset needs SUT to be up and running.
        If SUT already running, no need to restart it, we can reset its state.
        So, start and reset have same functionality here.
    */

    override fun startSUT() = changeState(true, true)

    override fun stopSUT() = changeState(false, false)

    override fun resetSUT() : Boolean{
        stc?.averageResetSUTTimeMs?.doStartTimer()
        val res = startSUT()
        stc?.averageResetSUTTimeMs?.addElapsedTime()
        return res
    }

    override fun checkConnection() {

        val response = try {
            getWebTarget()
                    .path(ControllerConstants.CONTROLLER_INFO)
                    .queryParam(ControllerConstants.METHOD_REPLACEMENT_CATEGORIES, config.methodReplacementCategories())
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get()
        } catch (e: Exception) {
            //FIXME proper exception checking
            throw NoRemoteConnectionException(port, host)
        }

        response.close()
    }

    override fun startANewSearch(): Boolean {

        val response = makeHttpCall {
            getWebTarget()
                    .path(ControllerConstants.NEW_SEARCH)
                    .request()
                    .post(Entity.entity("{\"newSearch\":true}", MediaType.APPLICATION_JSON_TYPE))
        }

        return readAndCheckResponse(response, "Failed to inform SUT of new search")
    }

    override fun getTestResults(ids: Set<Int>, ignoreKillSwitch: Boolean, allCovered: Boolean): TestResultsDto? {

        if(allCovered && ids.isNotEmpty()){
            throw IllegalArgumentException("Cannot specify allCovered and specific ids at same time")
        }

        val queryParam = ids.joinToString(",")

        if(!allCovered) stc?.averageOverheadMsTestResultsSubset?.doStartTimer()
        val response = makeHttpCall {
            getWebTarget()
                    .path(ControllerConstants.TEST_RESULTS)
                    .queryParam("ids", queryParam)
                    .queryParam("killSwitch", !ignoreKillSwitch && config.killSwitch)
                    .queryParam("allCovered", allCovered)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get()
        }
        if(!allCovered) stc?.averageOverheadMsTestResultsSubset?.addElapsedTime()

        stc?.apply {
            val len = try{ Integer.parseInt(response.getHeaderString("content-length"))}
                        catch (e: Exception) {-1}
            if(len >= 0) {
                if(allCovered) {
                    averageByteOverheadTestResultsAll.addValue(len)
                } else {
                    averageByteOverheadTestResultsSubset.addValue(len)
                }
            }
        }


        val dto = getDtoFromResponse(response, object : GenericType<WrappedResponseDto<TestResultsDto>>() {})

        if(!checkResponse(response, dto, "Failed to retrieve target coverage for $queryParam")){
            return null
        }

        return getData(dto)
    }


    /**
     * execute [actionDto] through [ControllerConstants.NEW_ACTION] endpoints of EMController,
     * @return execution response
     */
    override fun executeNewRPCActionAndGetResponse(actionDto: ActionDto) : ActionResponseDto?{

        val response = makeHttpCall {
            getWebTarget()
                    .path(ControllerConstants.NEW_ACTION)
                    .request()
                    .put(Entity.entity(actionDto, MediaType.APPLICATION_JSON_TYPE))
        }

        val dto = getDtoFromResponse(response,  object : GenericType<WrappedResponseDto<ActionResponseDto>>() {})

        if (!checkResponse(response, dto, "Failed to execute RPC call")) {
            return null
        }

        return dto?.data
    }

    /**
     * process post actions after search based on [postSearchActionDto]
     */
    override fun postSearchAction(postSearchActionDto: PostSearchActionDto): Boolean {
        val response = makeHttpCall {
            getWebTarget()
                .path(ControllerConstants.POST_SEARCH_ACTION)
                .request()
                .post(Entity.entity(postSearchActionDto, MediaType.APPLICATION_JSON_TYPE))
        }
        if (!wasSuccess(response)){
            //TODO
            return handleFailedResponse(response,"process post actions after search","postSearchAction failure")
        }
        return true
    }

    override fun registerNewAction(actionDto: ActionDto) : Boolean{

        val response = makeHttpCall {
            getWebTarget()
                    .path(ControllerConstants.NEW_ACTION)
                    .request()
                    .put(Entity.entity(actionDto, MediaType.APPLICATION_JSON_TYPE))
        }

        return readAndCheckResponse(response, "Failed to register new action")
    }

    override fun executeDatabaseCommand(dto: DatabaseCommandDto): Boolean {

        log.trace("Going to execute database command. Command:{} , Insertion.size={}",dto.command,dto.insertions?.size ?: 0)

        val response = makeHttpCall {
            getWebTarget()
                    .path(ControllerConstants.DATABASE_COMMAND)
                    .request()
                    .post(Entity.entity(dto, MediaType.APPLICATION_JSON_TYPE))
        }

        /*
           [non-determinism-source] Man: this might lead to non-determinism
        */
        if (!wasSuccess(response)) {

            return handleFailedResponse(response, "execute database command","Database command failure")
        }

        return true
    }

    private fun handleFailedResponse(response: Response, endpoint: String, textWarning: String) : Boolean{
        LoggingUtil.uniqueWarn(log, "Failed to $endpoint. HTTP status: {}.", response.status)

        if(response.mediaType == MediaType.TEXT_PLAIN_TYPE){
            //something weird is going on... possibly a bug in the Driver?

            val res = response.readEntity(String::class.java)
            log.error("$textWarning, HTTP status ${response.status}: $res")
            return false
        }

        val responseDto = try {
            response.readEntity(object : GenericType<WrappedResponseDto<*>>() {})
        } catch (e: Exception) {
            handleFailedDtoParsing(e)
            return false
        }

        if(responseDto.error != null) {
            //this can happen if we do not handle all constraints
            LoggingUtil.uniqueWarn(log, "Error message: " + responseDto.error)
        }

        /*
            TODO refactor this method once we support most of SQL handling, and do not need
            to have uniqueWarn any longer
         */

        return false
    }

    override fun executeDatabaseCommandAndGetQueryResults(dto: DatabaseCommandDto): QueryResultDto? {
        return executeDatabaseCommandAndGetResults(dto, object : GenericType<WrappedResponseDto<QueryResultDto>>() {})
    }

    override fun executeDatabaseInsertionsAndGetIdMapping(dto: DatabaseCommandDto): InsertionResultsDto? {
        return executeDatabaseCommandAndGetResults(dto, object : GenericType<WrappedResponseDto<InsertionResultsDto>>() {})
    }

    override fun executeMongoDatabaseInsertions(dto: MongoDatabaseCommandDto): MongoInsertionResultsDto? {
        return executeMongoDatabaseCommandAndGetResults(dto, object : GenericType<WrappedResponseDto<MongoInsertionResultsDto>>() {})
    }

    private fun <T> executeDatabaseCommandAndGetResults(dto: DatabaseCommandDto, type: GenericType<WrappedResponseDto<T>>): T?{

        val response = makeHttpCall {
            getWebTarget()
                    .path(ControllerConstants.DATABASE_COMMAND)
                    .request()
                    .post(Entity.entity(dto, MediaType.APPLICATION_JSON_TYPE))
        }

        val dto = getDtoFromResponse(response, type)

        if (!checkResponse(response, dto, "Failed to execute database command")) {
            return null
        }

        return dto?.data
    }

    private fun <T> executeMongoDatabaseCommandAndGetResults(dto: MongoDatabaseCommandDto, type: GenericType<WrappedResponseDto<T>>): T? {

        val response = makeHttpCall {
            getWebTarget()
                .path(ControllerConstants.MONGO_INSERTION)
                .request()
                .post(Entity.entity(dto, MediaType.APPLICATION_JSON_TYPE))
        }

        val dto = getDtoFromResponse(response, type)

        return dto?.data
    }


    private fun wasSuccess(response: Response?): Boolean {
        return response?.statusInfo?.family?.equals(Response.Status.Family.SUCCESSFUL) ?: false
    }

    private fun handleFailedDtoParsing(exception: Exception){

        if(exception is ProcessingException && exception.cause is UnrecognizedPropertyException){

            val version = this.javaClass.`package`?.implementationVersion
                    ?: "(cannot determine, likely due to EvoMaster being run directly from IDE and not as a packaged uber jar)"

            throw SutProblemException("There is a mismatch between the DTO that EvoMaster Driver is " +
                    "sending and what the EvoMaster Core process (this process) is expecting to receive. " +
                    "Are you sure you are using the same matching versions? This EvoMaster Core " +
                    "process version is: $version")
        } else {
            log.warn("Failed to parse dto", exception)
        }
    }
}
