package org.evomaster.core.problem.rpc.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.client.java.controller.api.dto.MockDatabaseDto
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.problem.RPCProblemDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.*
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.externalservice.ApiExternalServiceAction
import org.evomaster.core.problem.externalservice.rpc.DbAsExternalServiceAction
import org.evomaster.core.problem.externalservice.rpc.DbAsExternalServiceAction.Companion.getDbAsExternalServiceAction
import org.evomaster.core.problem.externalservice.rpc.RPCExternalServiceAction
import org.evomaster.core.problem.externalservice.rpc.RPCExternalServiceAction.Companion.getRPCExternalServiceActionName
import org.evomaster.core.problem.externalservice.rpc.parm.ClassResponseParam
import org.evomaster.core.problem.externalservice.rpc.parm.UpdateForRPCResponseParam
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.RPCCallResult
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.problem.rpc.auth.RPCAuthenticationInfo
import org.evomaster.core.problem.rpc.auth.RPCNoAuth
import org.evomaster.core.problem.rpc.param.RPCParam
import org.evomaster.core.problem.util.ActionBuilderUtil
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.problem.util.ParserDtoUtil.parseJsonNodeAsGene
import org.evomaster.core.problem.util.ParserDtoUtil.setGeneBasedOnString
import org.evomaster.core.problem.util.ParserDtoUtil.wrapWithOptionalGene
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.FixedMapGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.numeric.*
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.gene.optional.NullableGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.string.NumericStringGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.min

/**
 * this class is used to manage formulated individual with schemas of SUT
 */
class RPCEndpointsHandler {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RPCEndpointsHandler::class.java)
    }

    @Inject
    protected lateinit var config: EMConfig

    @Inject
    private lateinit var randomness: Randomness

    @Inject(optional = true)
    private lateinit var remoteController: RemoteController


    lateinit var infoDto: SutInfoDto


    /**
     * a map of available auth info configured through driver by user, retrieving from em driver side
     * - Key is auth reference based on the index of auth info specified in the dirver
     * - value is the detailed auth info
     */
    protected val authentications: MutableMap<Int, RPCAuthenticationInfo> = mutableMapOf()

    /**
     * key is an id of the endpoint, ie, interface name: action name
     * value is corresponding endpoint schema
     */
    private val actionSchemaCluster = mutableMapOf<String, RPCActionDto>()

    /**
     * a map of authorizedAction with available auth info
     * - Key is the id of action (which is consistent with key of [actionSchemaCluster])
     * - Value is a list of auth (which is based on key of [authentications])
     */
    private val authorizedActionAuthMap = mutableMapOf<String, MutableList<Int>>()


    /**
     * a map of actions with available customized candidates
     * - Key is the id of action (which is consistent with key of [actionSchemaCluster])
     * - Value is a set of customized candidates (which is based on index of customization info)
     */
    private val actionWithCustomizedCandidatesMap = mutableMapOf<String, MutableSet<String>>()

    /**
     * a map of auth info which could be setup with RPC endpoints
     * - key is the reference of the auth see [authentications]
     * - value is the action for auth setup
     */
    private val authEndpointCluster = mutableMapOf<Int, RPCActionDto>()

    /**
     * a map of RPC action to external services for RPC-based SUT
     * - key is the id of action (which is consistent with key of [actionSchemaCluster])
     * - value is a list of seeded api external services for the RPC action which can be found with [seededExternalServiceCluster]
     */
    private val actionToExternalServiceMap = mutableMapOf<String, MutableSet<String>>()

    /**
     * a map of external service actions cluster based on seeded test cases
     * - key is action id
     * - value is the example of ApiExternalServiceAction that might be expanded
     */
    private val seededExternalServiceCluster = mutableMapOf<String, ApiExternalServiceAction>()

    /**
     * a map of mock objects for sql based on seeded test cases
     * - key is the id of sql action to mock
     * - value is the example of MockDatabaseDto
     */
    private val seededDbMockObjects = mutableMapOf<String, DbAsExternalServiceAction>()

    /**
     * key is type in the schema
     * value is object gene for it
     */
    private val typeCache = mutableMapOf<String, Gene>()


    // used to handle dto and its string json
    private val objectMapper = ObjectMapper()

    /**
     * @param actionId is an id of the endpoint
     * @return action dto which contains info for its execution, eg, client, method name in the interface
     */
    fun getActionDto(actionId : String) : RPCActionDto{
        return actionSchemaCluster[actionId]?: throw IllegalStateException("could not find the $actionId")
    }

    /**
     * handle customized test generation with post actions after search
     */
    fun handleCustomizedTests(individuals : List<EvaluatedIndividual<RPCIndividual>>){
        val postSearchActionDto = PostSearchActionDto()
        postSearchActionDto.rpcTests = individuals.map {eval->
            val test = RPCTestDto()
            test.actions = eval.evaluatedMainActions().map { eval->
                val call = eval.action as RPCCallAction
                val res = eval.result as RPCCallResult
                val evaluatedRPCActionDto = transformResponseDto(call)
                if (res.isExceptionThrown()){
                    evaluatedRPCActionDto.exceptionMessage = res.getErrorMessage()
                    evaluatedRPCActionDto.exceptionName = res.getExceptionTypeName()
                }
                evaluatedRPCActionDto
            }
            // TODO for sql insertion
            test
        }
        remoteController.postSearchAction(postSearchActionDto)
    }

    /**
     * create RPC individual based on seeded tests
     */
    fun handledSeededTests(tests: Map<String, List<RPCActionDto>>): List<RPCIndividual>{
        return tests.map {e->
            val rpcActionDtos = e.value
            val exActions = mutableListOf<List<ApiExternalServiceAction>>()
            val rpcActions = rpcActionDtos.map { rpcActionDto->
                val external = mutableListOf<ApiExternalServiceAction>()
                val name = actionName(rpcActionDto.interfaceId, rpcActionDto.actionName)
                if (rpcActionDto.mockRPCExternalServiceDtos != null && rpcActionDto.mockRPCExternalServiceDtos.isNotEmpty()){
                    val ex = rpcActionDto.mockRPCExternalServiceDtos.map { e->
                        e.responses.mapIndexed { index, r->
                            val exAction = seededExternalServiceCluster[
                                getRPCExternalServiceActionName(e, index)
                            ]!!.copy() as ApiExternalServiceAction
                            try {
                                setGeneBasedOnString(exAction.response.responseBody, r)
                                exAction
                            }catch (e: Exception){
                                log.warn("Fail to handle mocked responses:${e.message}")
                                //throw RuntimeException("Fail to handle mocked responses", e)
                                null
                            }
                        }.filterNotNull()
                    }.flatten()
                    external.addAll(ex)
                }


               if (rpcActionDto.mockDatabaseDtos != null && rpcActionDto.mockDatabaseDtos.isNotEmpty()){
                   val dbEx = rpcActionDto.mockDatabaseDtos.map { dbDto->
                        val dbExAction = seededDbMockObjects[
                            getDbAsExternalServiceAction(dbDto)
                        ]!!.copy() as DbAsExternalServiceAction
                        try {
                            if (dbDto.response != null)
                                setGeneBasedOnString(dbExAction.response.responseBody, dbDto.response)
                            else{
                                dbExAction.response.responseBody.isActive = false
                            }
                            dbExAction
                        }catch (e: Exception){
                            log.warn("Fail to handle mocked Database responses:${e.message}")
                            null
                        }
                    }.filterNotNull()
                   external.addAll(dbEx)
                }

                exActions.add(external)
                processEndpoint(name, rpcActionDto, true)
            }.toMutableList()

            if (rpcActions.any { it.seeTopGenes().any { g-> !g.isLocallyValid() } }){
                log.warn("The given test (${e.key}) is invalid (e.g., violate constraints) that will not be involved in the test generation")
                null
            }else
                RPCIndividual(sampleType = SampleType.SEEDED, actions = rpcActions, externalServicesActions = exActions)

        }.filterNotNull()
    }




    private fun readJson(response: String) : JsonNode?{
        return try {
            objectMapper.readTree(response)
        }catch (e: Exception){
            null
        }
    }

    private fun extractRPCExternalServiceAction(sutInfoDto: SutInfoDto, rpcActionDto: RPCActionDto){

        val interfaceDto = sutInfoDto.rpcProblem?.schemas?.find { it.interfaceId == rpcActionDto.interfaceId}
            ?:throw IllegalStateException("cannot find the interface with id (${rpcActionDto.interfaceId})")

        val actionKey = actionName(rpcActionDto.interfaceId, rpcActionDto.actionName)

        rpcActionDto.mockRPCExternalServiceDtos?.forEach { dto->

            if (dto.requestRules!=null && dto.requestRules.isNotEmpty() && dto.requestRules.size != dto.responses.size && dto.responses.size != dto.responseTypes.size)
                throw IllegalArgumentException("the size of request identifications and responses should same but ${dto.requestRules.size} vs. ${dto.responses.size} vs. ${dto.responseTypes.size}")


            (dto.responseFullTypesWithGeneric?:dto.responseTypes).forEachIndexed { index, s ->

                val exkey = RPCExternalServiceAction.getRPCExternalServiceActionName(
                    dto.interfaceFullName, dto.functionName, dto.requestRules?.get(index), s
                )
                if (!seededExternalServiceCluster.containsKey(exkey)){
                    val responseTypeClass = interfaceDto.identifiedResponseTypes?.find { it.type.fullTypeNameWithGenericType == s || it.type.fullTypeName == s }
                    var fromClass = false
                    val responseGene = (
                            if (responseTypeClass != null){
                                handleDtoParam(responseTypeClass).also { fromClass = (dto.responseFullTypesWithGeneric != null) }
                            }else if(sutInfoDto.unitsInfoDto.extractedSpecifiedDtos?.containsKey(s) == true){
                                val schema = sutInfoDto.unitsInfoDto.extractedSpecifiedDtos[s]!!
                                fromClass = true
                                RestActionBuilderV3.createGeneForDTO("return", schema, s,
                                    RestActionBuilderV3.Options(enableConstraintHandling=config.enableSchemaConstraintHandling))
                            }else{
                                val node = readJson(dto.responses[index])
                                if (node != null){
                                    parseJsonNodeAsGene("return", node)
                                }else{
                                    StringGene("return")
                                }
                            }.run { wrapWithOptionalGene(this, true) }) as OptionalGene

                    val response = ClassResponseParam(className = s, responseType = EnumGene("responseType", listOf("JSON")), response = responseGene)
                    if (fromClass) response.responseParsedWithClass()

                    val externalAction = RPCExternalServiceAction(
                        interfaceName = dto.interfaceFullName,
                        functionName = dto.functionName,
                        descriptiveInfo = dto.appKey,
                        inputParamTypes = dto.inputParameterTypes,
                        requestRuleIdentifier = dto.requestRules?.get(index),
                        responseParam = response)
                    Lazy.assert { exkey == externalAction.getName() }
                    seededExternalServiceCluster[exkey] = externalAction
                }

                actionToExternalServiceMap.getOrPut(actionKey){ mutableSetOf() }.add(exkey)
            }
        }

        rpcActionDto.mockDatabaseDtos?.forEach{ dbDto->
            if (dbDto.commandName != null && dbDto.appKey!=null && dbDto.responseFullType != null){
                val exKey = DbAsExternalServiceAction
                    .getDbAsExternalServiceAction(dbDto.commandName, dbDto.requests, dbDto.responseFullTypeWithGeneric?:dbDto.responseFullType)

                if (!seededDbMockObjects.containsKey(exKey)){
                    val responseTypeClass = interfaceDto.identifiedResponseTypes?.find {
                        it.type.fullTypeNameWithGenericType == dbDto.responseFullTypeWithGeneric || it.type.fullTypeName == dbDto.responseFullType
                    }
                    var fromClass = false

                    val responseGene = ((if (responseTypeClass != null){
                        handleDtoParam(responseTypeClass).also { fromClass = (dbDto.responseFullTypeWithGeneric != null) }
                    }else if (dbDto.response != null){
                        val node = readJson(dbDto.response)
                        if (node != null){
                            parseJsonNodeAsGene("return", node)
                        }else{
                            StringGene("return")
                        }
                    }else{
                        StringGene("return")
                    }).run {
                        wrapWithOptionalGene(this, true)
                    }) as OptionalGene

                    val response = ClassResponseParam(className = dbDto.responseFullTypeWithGeneric?:dbDto.responseFullType, responseType = EnumGene("responseType", listOf("JSON")), response = responseGene)
                    if (fromClass) response.responseParsedWithClass()
                    val dbExAction = DbAsExternalServiceAction(
                        dbDto.commandName,
                        dbDto.appKey,
                        dbDto.requests,
                        response
                    )
                    seededDbMockObjects[exKey] = dbExAction
                }
                actionToExternalServiceMap.getOrPut(actionKey){ mutableSetOf() }.add(exKey)

            }else{
                LoggingUtil.uniqueWarn(log, "incorrect mockDatabaseDto with ${dbDto.commandName?:"null"} commandName, ${dbDto.appKey?:"null"} appKey, and ${dbDto.responseFullType?:"null"} responseFullType")
            }

        }
    }

    /**
     * cover [action] to a dto for handling mock RPC external services at the driver side, eg, customized method
     */
    fun transformMockRPCExternalServiceDto(action: ApiExternalServiceAction) : MockRPCExternalServiceDto{
        if (action !is RPCExternalServiceAction)
            throw IllegalStateException("only support RPC external service action for the moment")

        val mode = if (action.response.isJson()) GeneUtils.EscapeMode.JSON else throw IllegalStateException("only support response with json type for the monument")

        return MockRPCExternalServiceDto().apply {
            interfaceFullName = action.interfaceName
            functionName = action.functionName
            appKey = action.descriptiveInfo
            inputParameterTypes = action.inputParamTypes
            requestRules = if (action.requestRuleIdentifier.isNullOrEmpty()) null else listOf(action.requestRuleIdentifier)
            responses = listOf(action.response.responseBody.getValueAsPrintableString(mode = mode))
            responseTypes = if ((action.response as? ClassResponseParam)?.className?.isNotBlank() == true) listOf((action.response as ClassResponseParam).className) else null
            responseFullTypesWithGeneric = if ((action.response as? ClassResponseParam)?.run { className.isNotBlank() && fromClass  } == true) listOf((action.response as ClassResponseParam).className) else null
        }
    }

    fun transformMockDatabaseDto(action: DbAsExternalServiceAction) : MockDatabaseDto {
        val mode = if (action.response.isJson()) GeneUtils.EscapeMode.JSON else throw IllegalStateException("only support response with json type for the monument")
        return MockDatabaseDto().apply {
            appKey = action.descriptiveInfo
            commandName = action.commandName
            requests = action.requestRuleIdentifier
            response = action.response.responseBody.getValueAsPrintableString(mode = mode)
            responseFullType  = if ((action.response as? ClassResponseParam)?.className?.isNotBlank() == true) (action.response as ClassResponseParam).className else null
            responseFullTypeWithGeneric = if ((action.response as? ClassResponseParam)?.run { className.isNotBlank() && fromClass  } == true) (action.response as ClassResponseParam).className else null
        }
    }

    private fun setAuthInfo(infoDto: SutInfoDto){
        infoDto.infoForAuthentication?:return

        infoDto.infoForAuthentication.forEachIndexed { index, dto ->
            if (!handleRPCAuthDto(index, dto))
                log.warn("auth info at $index is not handled by RPC auth")
        }
    }


    private fun handleRPCAuthDto(index: Int, auth: AuthenticationDto) : Boolean{
        if (auth.jsonAuthEndpoint == null && auth.localAuthSetup == null)
            return false
        if (auth.jsonAuthEndpoint != null){
            authentications[index] = RPCAuthenticationInfo(auth.name?:"untitled",
                auth.jsonAuthEndpoint.annotationOnEndpoint == null,
                index//authEndpointCluster[index]?:throw IllegalStateException("could not find the auth endpoint with index $index"),
            )
        }
        if (auth.localAuthSetup != null){
            authentications[index] = RPCAuthenticationInfo(auth.name?:"untitled",
                auth.localAuthSetup.annotationOnEndpoint == null, index)
        }
        return true
    }

    /**
     * setup auth for [action] with an auth info at random
     */
    fun actionWithRandomAuth(action: RPCCallAction){

        val gs = getRelatedAuthInfo(action)
        if (gs.isNotEmpty())
            action.auth = randomness.choose(gs)
        else
            action.auth = RPCNoAuth()
    }

    private fun getRelatedAuthInfo(action: RPCCallAction) : List<RPCAuthenticationInfo> = authentications.values.filter { it.isGlobal }.plus(authorizedActionAuthMap[action.id]?.map { authentications[it]!! }?: listOf())

    /**
     * setup auth info for [action] with all auth candidates, ie, [authorizedActionAuthMap] and [authentications]
     */
    fun actionWithAllAuth(action: RPCCallAction): List<RPCCallAction>{
        val results = mutableListOf<RPCCallAction>()
        getRelatedAuthInfo(action).plus(listOf(RPCNoAuth())).forEach { u ->
            val actionWithAuth = action.copy()
            (actionWithAuth as RPCCallAction).auth = u
            results.add(actionWithAuth)

        }

        return results
    }

    /**
     * @param action to be generated with its all seeds
     * @return a list of adhoc actions with its specified candidate input request
     */
    fun actionWithAllCandidates(action: RPCCallAction): List<RPCCallAction>{
        val results = mutableListOf<RPCCallAction>()
        actionWithCustomizedCandidatesMap[action.id]?.forEach {
            results.add((action.copy() as RPCCallAction).apply { handleActionWithSeededCandidates(this, it) })
        }
        val noSeed = action.copy() as RPCCallAction
        handleActionNoSeededCandidates(noSeed)
        results.add(noSeed)
        return results
    }

    /**
     * @param action to be set with one seed at random
     * @param noSeedProbability  is a probability to not apply any seed
     * @return an action with/without seed
     */
    fun actionWithRandomSeeded(action: RPCCallAction, noSeedProbability: Double): RPCCallAction{
        val candidates = actionWithCustomizedCandidatesMap[action.id]
        if (candidates== null || candidates.isEmpty()) return action
        if (randomness.nextBoolean(noSeedProbability))
            handleActionNoSeededCandidates(action)
        else{
            val selected = randomness.choose(candidates)
            handleActionWithSeededCandidates(action, selected)
        }
        return action
    }


    private fun handleActionWithSeededCandidates(action: RPCCallAction, candidateKey: String){
        action.seeTopGenes().flatMap { it.flatView() }.filter { it is CustomMutationRateGene<*> && it.gene is SeededGene<*> }.forEach { g->
            val index = ((g as CustomMutationRateGene<*>).gene as SeededGene<*>).seeded.values.indexOfFirst { it is Gene && it.name == candidateKey }
            if (index != -1){
                (g.gene as SeededGene<*>).employSeeded = true
                g.gene.seeded.index = index
            }
        }
    }

    private fun handleActionNoSeededCandidates(action: RPCCallAction){
        action.seeTopGenes().filter { it is CustomMutationRateGene<*> && it.gene is SeededGene<*> }.forEach { g->
            ((g as CustomMutationRateGene<*>).gene as SeededGene<*>).employSeeded = false
            ((g.gene as SeededGene<*>).gene as Gene).randomize(randomness, false)
        }
    }

    /**
     * reset [actionCluster] based on interface schemas specified in [problem]
     */
    fun initActionCluster(problem: RPCProblemDto, actionCluster: MutableMap<String, Action>, infoDto: SutInfoDto){
        this.infoDto = infoDto

        val clientVariableMap = problem.schemas.mapIndexed {i, e->
            e.interfaceId!! to nameClientVariable(i, e.interfaceId.split(".").last())
        }.toMap()

        problem.schemas.forEach { i->
            i.types.plus(i.identifiedResponseTypes?: listOf()).sortedBy { it.type.depth }
                .filter { it.type.type == RPCSupportedDataType.CUSTOM_OBJECT }.forEach { t ->
                buildTypeCache(t)
            }
        }

        actionCluster.clear()
        problem.schemas.forEach{ i->
            i.endpoints.forEach{e->
                e.clientVariable = clientVariableMap[e.interfaceId]
                actionSchemaCluster.putIfAbsent(actionName(i.interfaceId, e.actionName), e)
                val name = actionName(i.interfaceId, e.actionName)
                if (actionCluster.containsKey(name))
                    throw IllegalStateException("$name exists in the actionCluster")
                actionCluster[name] = processEndpoint(name, e)
                if (e.isAuthorized && e.requiredAuthCandidates != null){
                    authorizedActionAuthMap[name] = e.requiredAuthCandidates
                }
                if (e.relatedCustomization != null){
                    actionWithCustomizedCandidatesMap[name] = e.relatedCustomization
                }
            }
            if (i.authEndpoints != null && i.authEndpointReferences != null){
                Lazy.assert { i.authEndpoints.size == i.authEndpointReferences.size }
                i.authEndpoints.forEachIndexed { index, e ->
                    //val name = actionName(i.interfaceId, e.actionName)
                    e.clientVariable = clientVariableMap[e.interfaceId]
                    if (authEndpointCluster.containsKey(index))
                        throw IllegalStateException("auth info at $index exists in the authEndpointCluster")
                    val key = i.authEndpointReferences[index]
                    authEndpointCluster[key] = e //processEndpoint(name, e, true)
                }
            }
        }

        if (problem.localAuthEndpoints!= null && problem.localAuthEndpointReferences != null){
            Lazy.assert {
                problem.localAuthEndpoints.size == problem.localAuthEndpointReferences.size
            }
            problem.localAuthEndpoints.forEachIndexed { index, rpcActionDto ->
                rpcActionDto.clientVariable = clientVariableMap[rpcActionDto.interfaceId]
                authEndpointCluster[problem.localAuthEndpointReferences[index]] = rpcActionDto
            }
        }

        setAuthInfo(infoDto)

        if (config.seedTestCases){
            // handle seeded test dto
            infoDto.rpcProblem.seededTestDtos?.values?.forEach { t->
                t.forEach { a->
                    extractRPCExternalServiceAction(infoDto, a)
                }
            }
        }


        // report statistic of endpoints
        reportEndpointsStatistics(
                problem.schemas.size,
                problem.schemas.sumOf { it.skippedEndpoints?.size ?: 0 },
                infoDto.rpcProblem?.seededTestDtos?.size?:0
        )

        reportMsgLog(infoDto.errorMsg)
    }


    /**
     * expand RPC schema during service with [latestSchemaDto]
     */
    fun expandSchema(action: RPCCallAction, expandInfo: ExpandRPCInfoDto){
        (expandInfo.schemaDto?.types?: listOf())
            .plus(expandInfo.schemaDto?.identifiedResponseTypes?: listOf())
            .filterNot { typeCache.containsKey(it.type.fullTypeNameWithGenericType) }
            .sortedBy { it.type.depth }
            .filter { it.type.type == RPCSupportedDataType.CUSTOM_OBJECT }.forEach { t ->
                buildTypeCache(t)
            }

        // update actionToExternalServiceMap and seededDbMockObjects
        expandInfo.expandActionDto?.mockDatabaseDtos?.filter { it.responseFullTypeWithGeneric != null }?.forEach{db->
            val exkey = DbAsExternalServiceAction
                .getDbAsExternalServiceAction(db.commandName, db.requests, db.responseFullTypeWithGeneric)

            val responseGene = (if (typeCache.containsKey(db.responseFullTypeWithGeneric)){
                typeCache[db.responseFullTypeWithGeneric]!!.copy()
            }else{
                val responseTypeClass = expandInfo.schemaDto?.identifiedResponseTypes?.find {
                    it.type.fullTypeNameWithGenericType == db.responseFullTypeWithGeneric
                }
                if (responseTypeClass != null)
                    handleDtoParam(responseTypeClass)
                else null
            }?.run { wrapWithOptionalGene(this, true) } as? OptionalGene)

            if (responseGene != null){
                val expandResponse = ClassResponseParam(className = db.responseFullTypeWithGeneric, responseType = EnumGene("responseType", listOf("JSON")), response = responseGene)
                expandResponse.responseParsedWithClass()

                val dbExAction = DbAsExternalServiceAction(
                    db.commandName,
                    db.appKey,
                    db.requests,
                    expandResponse
                )
                seededDbMockObjects[exkey] = dbExAction
                actionToExternalServiceMap.getOrPut(action.id){ mutableSetOf() }.add(exkey)

                if (db.responseFullTypeWithGeneric != db.responseFullType){
                    val oldkey = DbAsExternalServiceAction
                        .getDbAsExternalServiceAction(db.commandName, db.requests, db.responseFullType)
                    seededDbMockObjects[oldkey] = dbExAction.copy() as DbAsExternalServiceAction
                    actionToExternalServiceMap[action.id]?.remove(oldkey)
                }
            }

        }

        // update actionToExternalServiceMap and seededExternalServiceCluster
        expandInfo.expandActionDto?.mockRPCExternalServiceDtos?.filter { it.responseFullTypesWithGeneric != null }?.forEach{ex->
            ex.responseFullTypesWithGeneric.forEachIndexed { index, s ->
                val exkey = RPCExternalServiceAction.getRPCExternalServiceActionName(
                    ex.interfaceFullName, ex.functionName, ex.requestRules?.get(index), s
                )
                val responseGene = (if (typeCache.containsKey(s)){
                    typeCache[s]!!.copy()
                }else{
                    val responseTypeClass = expandInfo.schemaDto?.identifiedResponseTypes?.find {
                        it.type.fullTypeNameWithGenericType == s
                    }
                    if (responseTypeClass != null)
                        handleDtoParam(responseTypeClass)
                    else null
                }?.run { wrapWithOptionalGene(this, true) } as? OptionalGene)

                if (responseGene != null){
                    val response = ClassResponseParam(className = s, responseType = EnumGene("responseType", listOf("JSON")), response = responseGene)
                    response.responseParsedWithClass()
                    val externalAction = RPCExternalServiceAction(
                        interfaceName = ex.interfaceFullName,
                        functionName = ex.functionName,
                        descriptiveInfo = ex.appKey,
                        inputParamTypes = ex.inputParameterTypes,
                        requestRuleIdentifier = ex.requestRules?.get(index),
                        responseParam = response)
                    Lazy.assert { exkey == externalAction.getName() }
                    seededExternalServiceCluster[exkey] = externalAction
                    actionToExternalServiceMap.getOrPut(action.id){ mutableSetOf() }.add(exkey)

                    if (ex.responseTypes[index] != s){
                        val oldkey = RPCExternalServiceAction.getRPCExternalServiceActionName(
                            ex.interfaceFullName, ex.functionName, ex.requestRules?.get(index), ex.responseTypes[index]
                        )
                        seededExternalServiceCluster[oldkey] = externalAction.copy() as ApiExternalServiceAction
                        actionToExternalServiceMap[action.id]?.remove(oldkey)
                    }
                }
            }
        }
    }


    /**
     * expand RPC action during service with [latestSchemaDto]
     */
    fun expandRPCAction(externalActions: List<StructuralElement>){
        externalActions
            .flatMap { (it as ActionComponent).flatten() }
            .forEach { a->
                var oldResponse : ClassResponseParam? = null
                var newResponse : ClassResponseParam? = null
                if (a is RPCExternalServiceAction){
                    oldResponse = a.response as? ClassResponseParam
                    if (oldResponse?.fromClass == false){
                        newResponse = seededExternalServiceCluster[a.getName()]?.response?.copy() as? ClassResponseParam
                    }
                } else if (a is DbAsExternalServiceAction){
                    oldResponse = a.response as? ClassResponseParam
                    if (oldResponse?.fromClass == false){
                        newResponse = seededDbMockObjects[a.getName()]?.response?.copy() as? ClassResponseParam
                    }
                }
                if (oldResponse != null && newResponse != null  && newResponse.fromClass){
                    a.killChild(oldResponse)
                    val update = UpdateForRPCResponseParam(oldResponse)
                    if (a is RPCExternalServiceAction)
                        a.addUpdateForParam(update)
                    else if (a is DbAsExternalServiceAction)
                        a.addUpdateForParam(update)
                }
            }
    }
    private fun buildTypeCache(type: ParamDto){
        if (type.type.type == RPCSupportedDataType.CUSTOM_OBJECT && !typeCache.containsKey(type.type.fullTypeNameWithGenericType)){
            typeCache[type.type.fullTypeNameWithGenericType] = handleObjectType(type, true)
        }
    }

    private fun nameClientVariable(index: Int, interfaceSimpleName: String) : String = "var_client${index}_${interfaceSimpleName.replace("\$","_").replace("\\.","_")}"

    private fun reportEndpointsStatistics(numSchema: Int, skipped: Int, numSeededTest: Int){
        ActionBuilderUtil.printActionNumberInfo("RPC", actionSchemaCluster.size, skipped, 0)
        LoggingUtil.getInfoLogger().apply {
            if(numSchema > 1) {
                info("There are $numSchema defined RPC interfaces (used as schema declarations).")
            }
            if (numSeededTest > 0)
                info("$numSeededTest test${if (numSeededTest > 1) "s are" else " is"} seeded.")
        }
    }

    private fun reportMsgLog(msg : List<String>?){
        msg?:return
        LoggingUtil.getInfoLogger().apply {
            if (msg.isNotEmpty())
                info("Errors/Warnings in extraction of RPC schema and seeded tests:")
            msg.forEach {
                info(it)
            }
        }
    }

    /**
     * get rpc action dto based on specified [action]
     */
    fun transformActionDto(action: RPCCallAction, index : Int = -1, externalActions: List<StructuralElement>? = null) : RPCActionDto {
        // generate RPCActionDto
        val rpcAction = actionSchemaCluster[action.id]?.copy()?: throw IllegalStateException("cannot find the ${action.id} in actionSchemaCluster")

        action.parameters.forEach { p->
            if (p is RPCParam){
                p.seeGenes().forEach { g->
                    val paramDto = rpcAction.requestParams.find{ r-> r.name == g.name}?:throw IllegalStateException("cannot find param with a name, ${g.name}")
                    transformGeneToParamDto(g, paramDto)
                }
            }
        }

        if (action.auth !is RPCNoAuth){
            rpcAction.authSetup = authEndpointCluster[action.auth.authIndex]?.copy()
                ?: throw IllegalStateException("cannot specified auth index ${action.auth.authIndex} from the authEndpointCluster")
        }

        setGenerationConfiguration(rpcAction, index, generateResponseVariable(index))

        val missingDto = mutableSetOf<String>()

        var eactions : List<StructuralElement>? = externalActions
        // get external action if not specified
        if (externalActions.isNullOrEmpty()){
            eactions = (action.parent as EnterpriseActionGroup<*>)
                .groupsView()!!.getAllInGroup(GroupsOfChildren.EXTERNAL_SERVICES)
        }

        if (!eactions.isNullOrEmpty()){
            val exActions = eactions
                .flatMap { (it as ActionComponent).flatten() }
                .filterIsInstance<RPCExternalServiceAction>()
                .map { e->
                    (e.response as? ClassResponseParam)?.run {
                        if (!this.fromClass && this.className.isNotBlank())
                            missingDto.add(this.className)
                    }
                    transformMockRPCExternalServiceDto(e)
                }
            val mockDbActions = eactions
                .flatMap { (it as ActionComponent).flatten() }
                .filterIsInstance<DbAsExternalServiceAction>()
                .map { e->
                    (e.response as? ClassResponseParam)?.run {
                        if (!this.fromClass && this.className.isNotBlank())
                            missingDto.add(this.className)
                    }
                    transformMockDatabaseDto(e)
                }
            if (exActions.isNotEmpty())
                rpcAction.mockRPCExternalServiceDtos = exActions
            if (mockDbActions.isNotEmpty())
                rpcAction.mockDatabaseDtos = mockDbActions
        }

        if (missingDto.isNotEmpty())
            rpcAction.missingDto = missingDto.toList()
        return rpcAction
    }

    // Man: comment it out for the moment
//    fun getJVMSchemaForDto(names: Set<String>): Map<String, Gene> {
//
//        if (names.any { infoDto.unitsInfoDto?.extractedSpecifiedDtos?.containsKey(it) == false} ) {
//            infoDto = remoteController.getSutInfo()!!
//
//        names.filter { infoDto.unitsInfoDto?.extractedSpecifiedDtos?.containsKey(it)  == false}.run {
//            if (isNotEmpty())
//                LoggingUtil.uniqueWarn(log, "cannot extract schema for dtos (ie, ${this.joinToString(",")}) in the SUT driver and instrumentation agent")
//            }
//        }
//
//        val allDtoNames = infoDto.unitsInfoDto.parsedDtos.keys.toList()
//        val allDtoSchemas = allDtoNames.map { infoDto.unitsInfoDto.parsedDtos[it]!! }
//        RestActionBuilderV3.createObjectGeneForDTOs(allDtoNames, allDtoSchemas, allDtoNames, enableConstraintHandling = config.enableSchemaConstraintHandling)
//
//        return names.filter { infoDto.unitsInfoDto?.extractedSpecifiedDtos?.containsKey(it)  == true}.associateWith { name ->
//            val schema = infoDto.unitsInfoDto.extractedSpecifiedDtos[name]!!
//            RestActionBuilderV3.createObjectGeneForDTO(name, schema, name, config.enableSchemaConstraintHandling)
//        }
//    }

    private fun transformResponseDto(action: RPCCallAction) : EvaluatedRPCActionDto{
        // generate RPCActionDto
        val rpcAction = actionSchemaCluster[action.id]?.copy()?: throw IllegalStateException("cannot find the ${action.id} in actionSchemaCluster")
        val rpcResponseDto = rpcAction.responseParam
        if (action.response != null) transformGeneToParamDto(action.response!!.gene, rpcResponseDto)

        val evaluatedDto = EvaluatedRPCActionDto()
        evaluatedDto.rpcAction = transformActionDto(action)
        evaluatedDto.response = rpcResponseDto
        return evaluatedDto
    }

    private fun setGenerationConfiguration(action: RPCActionDto, index: Int, responseVarName: String){
        // check generation configuration, might be removed later
        action.doGenerateTestScript = config.enablePureRPCTestGeneration && (index != -1)
        action.outputFormat = SutInfoDto.OutputFormat.valueOf(config.outputFormat.toString())

        action.doGenerateAssertions = config.enableRPCAssertionWithInstance

        if (action.doGenerateTestScript){
            action.controllerVariable = TestSuiteWriter.controller
        }
        if (action.doGenerateTestScript || action.doGenerateAssertions){
            action.responseVariable = responseVarName
            action.maxAssertionForDataInCollection = config.maxAssertionForDataInCollection
        }

        if (action.authSetup != null){
            setGenerationConfiguration(action.authSetup, index, responseVarName+"_auth")
        }
    }



    /**
     * generate response variable name for RPC action based on its [index] in a test
     */
    private fun generateResponseVariable(index: Int) = "res_$index"

    /**
     * get rpc action dto with string json based on specified [action]
     * this is only used in test generation
     */
    fun getRPCActionJson(action: RPCCallAction) : String {
        val dto = transformActionDto(action)
        // ignore response param
        dto.responseParam = null
        return objectMapper.writeValueAsString(dto)
    }

    /**
     * @return a string json of a [dto] object
     */
    fun getJsonStringFromDto(dto: Any) : String {
        return objectMapper.writeValueAsString(dto)
    }

    /**
     * @return a string json of a RPCAction [dto]
     */
    fun getRPCActionDtoJson(dto: RPCActionDto) : String {
        return objectMapper.writeValueAsString(dto)
    }

    /**
     * @return an endpoint auth setup (ie, RPCActionDto) for the [action]
     */
    fun getRPCAuthActionDto(action: RPCCallAction) : RPCActionDto?{
        if (action.auth is RPCNoAuth)
            return null
        return authEndpointCluster[action.auth.authIndex]
    }

    private fun transformGeneToParamDto(gene: Gene, dto: ParamDto){

        if (gene is OptionalGene && !gene.isActive){
            // set null value
            if (gene.gene is ObjectGene || gene.gene is DateTimeGene || gene.gene is DateGene){
//                dto.innerContent = null
//                dto.stringValue = null
                dto.setNullValue()
            }
            return
        }

        dto.setNotNullValue()

        when(val valueGene = ParamUtil.getValueGene(gene)){
            is IntegerGene -> dto.stringValue = valueGene.value.toString()
            is DoubleGene -> dto.stringValue = valueGene.value.toString()
            is FloatGene -> dto.stringValue = valueGene.value.toString()
            is BooleanGene -> dto.stringValue = valueGene.value.toString()
            is StringGene -> dto.stringValue = valueGene.getValueAsRawString()
            is RegexGene -> dto.stringValue = valueGene.getValueAsRawString()
            is EnumGene<*> -> dto.stringValue = valueGene.index.toString()
            is SeededGene<*> -> dto.stringValue = getValueForSeededGene(valueGene)
            is LongGene -> dto.stringValue = valueGene.value.toString()
            is NumericStringGene -> dto.stringValue = valueGene.number.getValueAsRawString()
            is BigDecimalGene -> dto.stringValue = valueGene.getValueAsRawString()
            is BigIntegerGene -> dto.stringValue = valueGene.getValueAsRawString()
            is ArrayGene<*> -> {
                val template = dto.type.example?.copy()?:throw IllegalStateException("a template for a collection is null")
                val innerContent = valueGene.getViewOfElements().map {
                    val copy = template.copy()
                    transformGeneToParamDto(it, copy)
                    copy
                }
                dto.innerContent = innerContent
            }
            is DateTimeGene -> {
                transformGeneToParamDto(valueGene.date.year, dto.innerContent[0])
                transformGeneToParamDto(valueGene.date.month, dto.innerContent[1])
                transformGeneToParamDto(valueGene.date.day, dto.innerContent[2])
                transformGeneToParamDto(valueGene.time.hour, dto.innerContent[3])
                transformGeneToParamDto(valueGene.time.minute, dto.innerContent[4])
                transformGeneToParamDto(valueGene.time.second, dto.innerContent[5])
            }

            is DateGene -> {
                transformGeneToParamDto(valueGene.year, dto.innerContent[0])
                transformGeneToParamDto(valueGene.month, dto.innerContent[1])
                transformGeneToParamDto(valueGene.day, dto.innerContent[2])
            }
            is PairGene<*, *> ->{
                val template = dto.type.example?.copy()
                    ?:throw IllegalStateException("a template for a pair (with dto name: ${dto.name} and gene name: ${gene.name}) is null")
                Lazy.assert { template.innerContent.size == 2 }
                val first = template.innerContent[0]
                transformGeneToParamDto(valueGene.first, first)
                val second = template.innerContent[1]
                transformGeneToParamDto(valueGene.first, second)
                dto.innerContent = listOf(first, second)
            }
            is FixedMapGene<*, *> ->{
                val template = dto.type.example?.copy()
                    ?:throw IllegalStateException("a template for a map dto (with dto name: ${dto.name} and gene name: ${gene.name}) is null")
                val innerContent = valueGene.getAllElements().map {
                    val copy = template.copy()
                    transformGeneToParamDto(it, copy)
                    copy
                }
                dto.innerContent = innerContent
            }
            is ObjectGene -> {
                valueGene.fields.forEach { f->
                    val pdto = dto.innerContent.find { it.name == f.name }
                        ?:throw IllegalStateException("could not find the field (${f.name}) in ParamDto")
                    transformGeneToParamDto(f, pdto)
                }
            }
            is CycleObjectGene ->{
                dto.setNullValue()
            }
            else -> throw IllegalStateException("Not support transformGeneToParamDto with gene ${gene::class.java.simpleName} and dto (${dto.type.type})")
        }
    }

    /**
     * set values of [gene] based on dto i.e., [ParamDto]
     * note that it is typically used for handling responses of RPC endpoints
     */
    fun setGeneBasedOnParamDto(gene: Gene, dto: ParamDto){

        val valueGene = ParamUtil.getValueGene(gene)

        if (!isValidToSetValue(valueGene, dto))
            throw IllegalStateException("the types of gene and its dto are mismatched, i.e., gene (${valueGene::class.java.simpleName}) vs. dto (${dto.type.type})")

        if (!isNullDto(dto)){
            when(valueGene){
                is IntegerGene -> valueGene.setValueWithRawString(dto.stringValue)
                is DoubleGene -> valueGene.setValueWithRawString(dto.stringValue)
                is FloatGene -> valueGene.setValueWithRawString(dto.stringValue)
                is BooleanGene -> valueGene.setValueWithRawString(dto.stringValue)
                is StringGene -> valueGene.value = dto.stringValue
                is BigDecimalGene -> valueGene.setValueWithRawString(dto.stringValue)
                is BigIntegerGene -> valueGene.setValueWithRawString(dto.stringValue)
                is NumericStringGene -> valueGene.number.setValueWithRawString(dto.stringValue)
                is RegexGene -> {
                    // TODO set value based on RegexGene
                }
                is LongGene -> valueGene.setValueWithRawString(dto.stringValue)
                is EnumGene<*> -> valueGene.setValueWithRawString(dto.stringValue)
                is SeededGene<*> -> {
                    /*
                        response might refer to input dto, then it might exist seeded gene
                     */
                    valueGene.employSeeded = false
                    setGeneBasedOnParamDto(valueGene.gene as Gene, dto)
                }
                is PairGene<*, *> -> {
                    Lazy.assert { dto.innerContent.size == 2 }
                    setGeneBasedOnParamDto(valueGene.first, dto.innerContent[0])
                    setGeneBasedOnParamDto(valueGene.second, dto.innerContent[1])
                }
                is DateTimeGene ->{
                    Lazy.assert { dto.innerContent.size == 6 }
                    setGeneBasedOnParamDto(valueGene.date.year, dto.innerContent[0])
                    setGeneBasedOnParamDto(valueGene.date.month, dto.innerContent[1])
                    setGeneBasedOnParamDto(valueGene.date.day, dto.innerContent[2])
                    setGeneBasedOnParamDto(valueGene.time.hour, dto.innerContent[3])
                    setGeneBasedOnParamDto(valueGene.time.minute, dto.innerContent[4])
                    setGeneBasedOnParamDto(valueGene.time.second, dto.innerContent[5])
                }
                is DateGene -> {
                    Lazy.assert { dto.innerContent.size == 3 }
                    setGeneBasedOnParamDto(valueGene.year, dto.innerContent[0])
                    setGeneBasedOnParamDto(valueGene.month, dto.innerContent[1])
                    setGeneBasedOnParamDto(valueGene.day, dto.innerContent[2])
                }
                is ArrayGene<*> -> {
                    val template = valueGene.template
                    dto.innerContent.run {
                        if (valueGene.maxSize!=null && valueGene.maxSize!! < size){
                            log.warn("ArrayGene: responses have more elements than it allows, i.e., max is ${valueGene.maxSize} but the actual is ${size}")
                            subList(0, valueGene.maxSize!!)
                        }else
                            this
                    }.forEach { p->
                        val copy = template.copy()
                        // TODO need to handle cycle object gene in responses
                        if (copy !is CycleObjectGene){
                            setGeneBasedOnParamDto(copy, p)
                            valueGene.addElement(copy)
                        }
                    }
                }
                is FixedMapGene<*, *> ->{
                    val template = valueGene.template
                    dto.innerContent.run {
                        if (valueGene.maxSize!=null && valueGene.maxSize!! < size){
                            log.warn("MapGene: responses have more elements than it allows, i.e., max is ${valueGene.maxSize} but the actual is ${size}")
                            subList(0, valueGene.maxSize!!)
                        }else
                            this
                    }.forEach { p->
                        val copy = template.copy()
                        setGeneBasedOnParamDto(copy, p)
                        valueGene.addElement(copy)
                    }
                }
                is ObjectGene -> {
                    valueGene.fields.forEach { f->
                        val pdto = dto.innerContent.find { it.name == f.name }
                            ?:throw IllegalStateException("could not find the field (${f.name}) in ParamDto")
                        setGeneBasedOnParamDto(f, pdto)
                    }
                }
                is CycleObjectGene ->{
                    if (dto.innerContent != null){
                        LoggingUtil.uniqueWarn(log, "NOT support to handle cycle object with more than 2 depth")
                    }
                }
                else -> throw IllegalStateException("Not support setGeneBasedOnParamDto with gene ${gene::class.java.simpleName} and dto (${dto.type.type})")
            }
        }else{
            if (gene is OptionalGene && dto.isNullable)
                gene.isActive = false
            else if (gene is NullableGene && dto.isNullable)
                gene.isActive = false
            else{
                /*
                    such case might exist in seeded tests
                    later might support robustness testing for RPC to handle, eg, set null for non-nullable parameter
                 */
                LoggingUtil.uniqueWarn(log, "could not retrieve value of ${dto.name?:"untitled"} with ${gene::class.java.simpleName}")
            }

        }
    }

    private fun isNullDto(dto: ParamDto) : Boolean{
        return when(dto.type.type){
            RPCSupportedDataType.P_INT, RPCSupportedDataType.INT,
            RPCSupportedDataType.P_SHORT, RPCSupportedDataType.SHORT,
            RPCSupportedDataType.P_BYTE, RPCSupportedDataType.BYTE,
            RPCSupportedDataType.P_BOOLEAN, RPCSupportedDataType.BOOLEAN,
            RPCSupportedDataType.P_CHAR, RPCSupportedDataType.CHAR, RPCSupportedDataType.STRING, RPCSupportedDataType.BYTEBUFFER,
            RPCSupportedDataType.P_DOUBLE, RPCSupportedDataType.DOUBLE,
            RPCSupportedDataType.P_FLOAT, RPCSupportedDataType.FLOAT,
            RPCSupportedDataType.P_LONG, RPCSupportedDataType.LONG,
            RPCSupportedDataType.ENUM,
            RPCSupportedDataType.BIGDECIMAL, RPCSupportedDataType.BIGINTEGER,
            RPCSupportedDataType.UTIL_DATE, RPCSupportedDataType.LOCAL_DATE, RPCSupportedDataType.CUSTOM_OBJECT -> dto.stringValue == null
            RPCSupportedDataType.ARRAY, RPCSupportedDataType.SET, RPCSupportedDataType.LIST,
            RPCSupportedDataType.MAP,
            RPCSupportedDataType.CUSTOM_CYCLE_OBJECT,
            RPCSupportedDataType.PAIR -> dto.innerContent == null
        }
    }

    /**
     * @return if types of [gene] and its [dto] ie, ParamDto are matched.
     */
    private fun isValidToSetValue(gene: Gene, dto: ParamDto) : Boolean{
        val valueGene = ParamUtil.getValueGene(gene)
        if (valueGene is SeededGene<*>)
            return isValidToSetValue(valueGene.gene  as Gene, dto)

        return when(dto.type.type){
            RPCSupportedDataType.P_INT, RPCSupportedDataType.INT,
            RPCSupportedDataType.P_SHORT, RPCSupportedDataType.SHORT,
            RPCSupportedDataType.P_BYTE, RPCSupportedDataType.BYTE -> valueGene is IntegerGene
            RPCSupportedDataType.P_BOOLEAN, RPCSupportedDataType.BOOLEAN -> valueGene is BooleanGene
            RPCSupportedDataType.P_CHAR,
            RPCSupportedDataType.CHAR,
            RPCSupportedDataType.STRING,
            RPCSupportedDataType.BYTEBUFFER -> valueGene is StringGene || (valueGene is RegexGene && dto.pattern != null) || valueGene is NumericStringGene
            RPCSupportedDataType.P_DOUBLE, RPCSupportedDataType.DOUBLE -> valueGene is DoubleGene
            RPCSupportedDataType.P_FLOAT, RPCSupportedDataType.FLOAT -> valueGene is FloatGene
            RPCSupportedDataType.P_LONG, RPCSupportedDataType.LONG -> valueGene is LongGene
            RPCSupportedDataType.ENUM -> valueGene is FixedMapGene<*, *> || valueGene is EnumGene<*>
            RPCSupportedDataType.ARRAY, RPCSupportedDataType.SET, RPCSupportedDataType.LIST-> valueGene is ArrayGene<*>
            RPCSupportedDataType.MAP -> valueGene is FixedMapGene<*, *>
            RPCSupportedDataType.CUSTOM_OBJECT -> valueGene is ObjectGene || valueGene is FixedMapGene<*, *>
            RPCSupportedDataType.CUSTOM_CYCLE_OBJECT -> valueGene is CycleObjectGene
            RPCSupportedDataType.UTIL_DATE -> valueGene is DateTimeGene
            RPCSupportedDataType.LOCAL_DATE -> valueGene is DateGene
            RPCSupportedDataType.PAIR -> valueGene is PairGene<*, *>
            RPCSupportedDataType.BIGDECIMAL -> valueGene is BigDecimalGene
            RPCSupportedDataType.BIGINTEGER -> valueGene is BigIntegerGene
        }
    }

    private fun processEndpoint(name: String, endpointSchema: RPCActionDto, doAssignGeneValue: Boolean = false) : RPCCallAction{
        val params = mutableListOf<Param>()

        endpointSchema.requestParams.forEach { p->
            val gene = handleDtoParam(p)
            params.add(RPCParam(p.name, gene))
            if (doAssignGeneValue){
                setGeneBasedOnParamDto(gene, p)
            }
        }

        var response: RPCParam? = null
        // response would be used for assertion generation
        if (endpointSchema.responseParam != null){
            val gene = handleDtoParam(endpointSchema.responseParam)
            response = RPCParam(endpointSchema.responseParam.name, gene)
        }
        /*
            TODO Man exception
         */

        return RPCCallAction(endpointSchema.interfaceId, name, params, responseTemplate = response, response = null )
    }

    private fun actionName(interfaceName: String, endpointName: String) = "$interfaceName:$endpointName"

    private fun handleDtoParam(param: ParamDto, building: Boolean = false): Gene{
        val gene = when(param.type.type){
            RPCSupportedDataType.P_INT, RPCSupportedDataType.INT ->
                IntegerGene(param.name, min = param.minValue?.toInt()?: Int.MIN_VALUE, max = param.maxValue?.toInt()?:Int.MAX_VALUE,
                    minInclusive = param.minValue == null || param.minInclusive, maxInclusive = param.maxValue == null || param.maxInclusive, precision = param.precision)
            RPCSupportedDataType.P_BOOLEAN, RPCSupportedDataType.BOOLEAN -> BooleanGene(param.name)
            RPCSupportedDataType.P_CHAR, RPCSupportedDataType.CHAR -> StringGene(param.name, value="", maxLength = 1, minLength = param.minSize?.toInt()?:0)
            RPCSupportedDataType.P_DOUBLE, RPCSupportedDataType.DOUBLE ->
                DoubleGene(param.name, min = param.minValue?.toDouble(), max = param.maxValue?.toDouble(),
                    minInclusive = param.minValue == null || param.minInclusive, maxInclusive = param.maxValue == null || param.maxInclusive,
                    precision = param.precision, scale = param.scale)
            RPCSupportedDataType.P_FLOAT, RPCSupportedDataType.FLOAT ->
                FloatGene(param.name, min = param.minValue?.toFloat(), max = param.maxValue?.toFloat(),
                    minInclusive = param.minValue == null || param.minInclusive, maxInclusive = param.maxValue == null || param.maxInclusive,
                    precision = param.precision, scale = param.scale)
            RPCSupportedDataType.P_LONG, RPCSupportedDataType.LONG ->
                LongGene(param.name, min = param.minValue?.toLongOrNull(), max = param.maxValue?.toLongOrNull(),
                    minInclusive = param.minValue == null || param.minInclusive, maxInclusive = param.maxValue == null || param.maxInclusive, precision = param.precision)
            RPCSupportedDataType.P_SHORT, RPCSupportedDataType.SHORT ->
                IntegerGene(param.name, min = param.minValue?.toInt()?:Short.MIN_VALUE.toInt(), max = param.maxValue?.toInt()?:Short.MAX_VALUE.toInt(),
                    minInclusive = param.minValue == null || param.minInclusive, maxInclusive = param.maxValue == null || param.maxInclusive, precision = param.precision)
            RPCSupportedDataType.P_BYTE, RPCSupportedDataType.BYTE ->
                IntegerGene(param.name, min = param.minValue?.toInt()?:Byte.MIN_VALUE.toInt(), max = param.maxValue?.toInt()?:Byte.MAX_VALUE.toInt(),
                    minInclusive = param.minValue == null || param.minInclusive, maxInclusive = param.maxValue == null || param.maxInclusive, precision = param.precision)
            RPCSupportedDataType.STRING, RPCSupportedDataType.BYTEBUFFER -> {
                if (param.hasNumberConstraints() && param.pattern == null){
                    val p : Int? = if (param.precision!= null && param.maxSize != null){
                        min(param.precision!!, (if (param.scale == null || param.scale == 0) param.maxSize else (param.maxSize-1)).toInt())
                    }else param.precision

                    NumericStringGene(name = param.name, minLength = param.minSize?.toInt()?:0, min = param.minValue?.toBigDecimalOrNull(), max = param.maxValue?.toBigDecimalOrNull(),
                        minInclusive = param.minValue == null || param.minInclusive, maxInclusive = param.maxValue == null || param.maxInclusive,
                        precision = p, scale = param.scale?:0)
                }else {
                    if (param.hasNumberConstraints() && param.pattern != null)
                        log.warn("Not support numeric constraints and pattern together yet, and check the param ${param.name}")

                    var strGene : Gene = StringGene(param.name, minLength = param.minSize?.toInt()?:0, maxLength = param.maxSize?.toInt()?:EMConfig.stringLengthHardLimit).apply {

                        // String could have bigDecimal or bigInteger as part of specification if any number related constraint property is specified
                        if (param.precision != null || param.scale != null){
                            addChild(
                                BigDecimalGene(param.name, min = param.minValue?.toBigDecimalOrNull(), max = param.maxValue?.toBigDecimalOrNull(),
                                precision = param.precision, scale = param.scale, minInclusive = param.minValue == null || param.minInclusive, maxInclusive = param.maxValue == null || param.maxInclusive)
                            )
                        } else if (param.minValue != null || param.maxValue != null){
                            // only max or min, we recognize it as biginteger
                            addChild(
                                BigIntegerGene(param.name, min=param.minValue?.toBigIntegerOrNull(), max = param.maxValue?.toBigIntegerOrNull(),
                                minInclusive = param.minValue == null || param.minInclusive, maxInclusive = param.maxValue == null || param.maxInclusive)
                            )
                        }
                    }

                    if (param.pattern != null){
                        try {
                            val regex = RegexHandler.createGeneForEcma262(param.pattern).apply {this.name = param.name}
                            /*
                                if there only exists pattern, we recognize it as regexgene
                                otherwise put the regex as part of specialization
                             */
                            if ((strGene as? StringGene)?.specializationGenes?.isNotEmpty() == true){
                                strGene.addChild(regex)
                            }else
                                strGene = regex
                        } catch (e: Exception) {
                            LoggingUtil.uniqueWarn(log, "Cannot handle regex: ${param.pattern}")
                        }
                    }

                    strGene
                }
            }
            RPCSupportedDataType.ENUM -> handleEnumParam(param)
            RPCSupportedDataType.ARRAY, RPCSupportedDataType.SET, RPCSupportedDataType.LIST-> handleCollectionParam(param, building)
            RPCSupportedDataType.MAP -> handleMapParam(param, building)
            RPCSupportedDataType.UTIL_DATE -> handleUtilDate(param)
            RPCSupportedDataType.LOCAL_DATE -> handleLocalDate(param)
            RPCSupportedDataType.CUSTOM_OBJECT -> handleObjectParam(param)
            RPCSupportedDataType.CUSTOM_CYCLE_OBJECT -> CycleObjectGene(param.name)
            RPCSupportedDataType.PAIR -> throw IllegalStateException("ERROR: pair should be handled inside Map")
            RPCSupportedDataType.BIGINTEGER ->
                BigIntegerGene(param.name, min = param.minValue?.toBigIntegerOrNull(), max = param.maxValue?.toBigIntegerOrNull(),
                    minInclusive = param.minValue == null || param.minInclusive, maxInclusive = param.maxValue == null || param.maxInclusive, precision = param.precision)
            RPCSupportedDataType.BIGDECIMAL -> BigDecimalGene(param.name, min = param.minValue?.toBigDecimalOrNull(), max = param.maxValue?.toBigDecimalOrNull(),
                minInclusive = param.minValue == null || param.minInclusive, maxInclusive = param.maxValue == null || param.maxInclusive,
                precision = param.precision, scale = param.scale)
        }

        if (param.candidates != null){
            val candidates = param.candidates.map {p-> gene.copy().apply { setGeneBasedOnParamDto(this, p) } }.toList()
            if (candidates.isNotEmpty()){
                if (param.candidateReferences != null){
                    Lazy.assert { param.candidates.size == param.candidateReferences.size }
                    candidates.forEachIndexed { index, g ->  g.name = param.candidateReferences[index] }
                }
                val seededGene = handleGeneWithCandidateAsEnumGene(gene, candidates)

                if (param.candidateReferences == null)
                    return wrapWithOptionalGene(seededGene, param.isNullable)

                return CustomMutationRateGene(param.name, seededGene, 0.0)
            }
        }

        val wg = wrapWithOptionalGene(gene, param.isNullable)
        return handleIsMutableAndDefault(wg, param.isMutable, param.defaultValue)
    }

    /**
     * handle isMutable property of Parameter and default value
     *
     * note that
     * if the gene is not mutable, then employ DisruptiveGene to handle it with 0.0 probability
     */
    private fun handleIsMutableAndDefault(gene: Gene, isMutable : Boolean, defaultValue: ParamDto?) : Gene{
        if (isMutable) {
            if (defaultValue != null)
                setGeneBasedOnParamDto(gene, defaultValue)
            return gene
        }

        if (defaultValue == null){
            if (gene !is OptionalGene)
                throw IllegalStateException("Fail to set default value for an immutable gene")
            gene.isActive = false
            return CustomMutationRateGene(gene.name, gene, 0.0)
        }

        setGeneBasedOnParamDto(gene, defaultValue)
        return CustomMutationRateGene(gene.name, gene, 0.0)
    }

    private fun handleGeneWithCandidateAsEnumGene(gene: Gene, candidates: List<Gene>) : SeededGene<*>{
        return  when (gene) {
            is StringGene -> SeededGene(gene.name, gene, EnumGene(gene.name, candidates.map { it as StringGene }))
            is IntegerGene -> SeededGene(gene.name, gene, EnumGene(gene.name, candidates.map { it as IntegerGene }))
            is FloatGene ->  SeededGene(gene.name, gene, EnumGene(gene.name, candidates.map { it as FloatGene }))
            is LongGene ->  SeededGene(gene.name, gene, EnumGene(gene.name, candidates.map { it as LongGene }))
            is DoubleGene -> SeededGene(gene.name, gene, EnumGene(gene.name, candidates.map { it as DoubleGene }))
            is BigDecimalGene -> SeededGene(gene.name, gene, EnumGene(gene.name, candidates.map { it as BigDecimalGene }))
            is BigIntegerGene -> SeededGene(gene.name, gene, EnumGene(gene.name, candidates.map { it as BigIntegerGene }))
            is NumericStringGene -> SeededGene(gene.name, gene, EnumGene(gene.name, candidates.map { it as NumericStringGene }))
            // might be DateGene
            else -> {
                throw IllegalStateException("Do not support configuring candidates for ${gene::class.java.simpleName} gene type")
            }
        }
    }

    private fun getValueForSeededGene(gene: SeededGene<*>) : String{
        return when (val pGene = gene.getPhenotype() as Gene) {
            is StringGene -> pGene.getValueAsRawString()
            is IntegerGene -> pGene.value.toString()
            is FloatGene -> pGene.value.toString()
            is LongGene -> pGene.value.toString()
            is DoubleGene -> pGene.value.toString()
            is BigDecimalGene,is BigIntegerGene, is NumericStringGene -> pGene.getValueAsRawString()
            else -> {
                throw IllegalStateException("Do not support configuring candidates for ${gene::class.java.simpleName} gene type")
            }
        }
    }

    private fun handleUtilDate(param: ParamDto) : DateTimeGene{
        /*
            only support simple format (more details see [org.evomaster.client.java.controller.problem.rpc.schema.types.DateType]) for the moment
         */
        Lazy.assert { param.innerContent.size == 6 }
        return DateTimeGene(param.name)
    }

    private fun handleLocalDate(param: ParamDto) : DateGene {
        /*
            only support simple format (more details see [org.evomaster.client.java.controller.problem.rpc.schema.types.DateType]) for the moment
         */
        Lazy.assert { param.innerContent.size == 3 }
        return DateGene(param.name)
    }


    private fun handleEnumParam(param: ParamDto): Gene{
        if (param.type.fixedItems.isNullOrEmpty()){
            LoggingUtil.uniqueWarn(log, "Enum with name (${param.type.fullTypeName}) has empty items")
            // TODO check not sure
            //return MapGene(param.type.fullTypeName, PairGene.createStringPairGene(StringGene( "NO_ITEM")), maxSize = 0)
            return EnumGene(param.name, listOf<String>())
        }
        return EnumGene(param.name, param.type.fixedItems.toList())

    }

    private fun handleMapParam(param: ParamDto, building: Boolean) : Gene{
        val pair = param.type.example
        Lazy.assert { pair.innerContent.size == 2 }
        val keyTemplate = handleDtoParam(pair.innerContent[0], building)
        val valueTemplate = handleDtoParam(pair.innerContent[1], building)

        return FixedMapGene(param.name, keyTemplate, valueTemplate, maxSize = param.maxSize?.toInt(), minSize = param.minSize?.toInt())
    }

    private fun handleCollectionParam(param: ParamDto, building: Boolean) : Gene{
        val templateParam = when(param.type.type){
            RPCSupportedDataType.ARRAY, RPCSupportedDataType.SET, RPCSupportedDataType.LIST -> param.type.example
            else -> throw IllegalStateException("do not support the collection type: "+ param.type.type)
        }
        if (building)
            buildTypeCache(templateParam)
        val template = handleDtoParam(templateParam)
        return ArrayGene(param.name, template, maxSize = param.maxSize?.toInt(), minSize = param.minSize?.toInt(), uniqueElements = param.type.type == RPCSupportedDataType.SET)
    }

    private fun handleObjectType(type: ParamDto, building: Boolean): Gene{
        val typeName = type.type.fullTypeNameWithGenericType
        if (type.innerContent.isEmpty()){
            LoggingUtil.uniqueWarn(log, "Object with name (${type.type.fullTypeNameWithGenericType}) has empty fields")
            //return MapGene(typeName, PairGene.createStringPairGene(StringGene( "field"), isFixedFirst = true))
            return ObjectGene(typeName, listOf(), refType = typeName)
        }

        val fields = type.innerContent.map { f->
            if (building)
                buildTypeCache(f)
            handleDtoParam(f, building)
        }

        return ObjectGene(typeName, fields, refType = typeName)
    }

    private fun handleObjectParam(param: ParamDto): Gene{
        val objType = typeCache[param.type.fullTypeNameWithGenericType]
            ?:throw IllegalStateException("missing ${param.type.fullTypeNameWithGenericType} in typeCache")
        return objType.copy().apply { this.name = param.name }
    }

    fun getGeneIfExist(typeName: String, paramName : String): Gene?{
        val objType = typeCache[typeName]?: return null
        return objType.copy().apply { this.name = paramName }
    }

    /**
     * @return a list of map of
     *      key is client variable
     *      value is client info, client class name to interface class name
     */
    fun getClientAndItsVariable() : Map<String, Pair<String, String>>{
        val map = mutableMapOf<String, Pair<String, String>>()
        actionSchemaCluster.forEach { (t, u) ->
            Lazy.assert { u.clientVariable != null && u.clientInfo != null }
            map.putIfAbsent(u.clientVariable, u.clientInfo to u.interfaceId)
        }
        return map
    }
}
