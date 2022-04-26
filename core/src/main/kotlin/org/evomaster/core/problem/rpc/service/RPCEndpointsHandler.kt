package org.evomaster.core.problem.rpc.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.problem.RPCProblemDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.problem.api.service.param.Param
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.problem.rpc.auth.RPCAuthenticationInfo
import org.evomaster.core.problem.rpc.auth.RPCNoAuth
import org.evomaster.core.problem.rpc.param.RPCParam
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.regex.RegexGene
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
     * create RPC individual based on seeded tests
     */
    fun handledSeededTests(tests: List<List<RPCActionDto>>): List<RPCIndividual>{
        return tests.map {td->
            RPCIndividual(actions = td.map { d->
                val name = actionName(d.interfaceId, d.actionName)
                processEndpoint(name, d, true)
            }.toMutableList())
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
        action.seeGenes().flatMap { it.flatView() }.filter { it is DisruptiveGene<*> && it.gene is SeededGene<*> }.forEach { g->
            val index = ((g as DisruptiveGene<*>).gene as SeededGene<*>).seeded.values.indexOfFirst { it.name == candidateKey }
            if (index != -1){
                (g.gene as SeededGene<*>).employSeeded = true
                g.gene.seeded.index = index
            }
        }
    }

    private fun handleActionNoSeededCandidates(action: RPCCallAction){
        action.seeGenes().filter { it is DisruptiveGene<*> && it.gene is SeededGene<*> }.forEach { g->
            ((g as DisruptiveGene<*>).gene as SeededGene<*>).employSeeded = false
            (g.gene as SeededGene<*>).gene.randomize(randomness, false)
        }
    }

    /**
     * reset [actionCluster] based on interface schemas specified in [problem]
     */
    fun initActionCluster(problem: RPCProblemDto, actionCluster: MutableMap<String, Action>, infoDto: SutInfoDto){

        problem.schemas.forEach { i->
            i.types.sortedBy { it.type.depth }
                .filter { it.type.type == RPCSupportedDataType.CUSTOM_OBJECT }.forEach { t ->
                typeCache[t.type.fullTypeNameWithGenericType] = handleObjectType(t)
            }

        }

        actionCluster.clear()
        problem.schemas.forEach { i->
            i.endpoints.forEach{e->
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
                authEndpointCluster[problem.localAuthEndpointReferences[index]] = rpcActionDto
            }
        }

        setAuthInfo(infoDto)

        // report statistic of endpoints
        reportEndpointsStatistics(problem.schemas.size, problem.schemas.sumOf { it.skippedEndpoints?.size ?: 0 })
    }

    private fun reportEndpointsStatistics(numSchema: Int, skipped: Int){
        LoggingUtil.getInfoLogger().apply {
            info("There are $numSchema defined RPC interfaces with ${actionSchemaCluster.size} accessible endpoints and $skipped skipped endpoints.")
        }
    }

    /**
     * get rpc action dto based on specified [action]
     */
    fun transformActionDto(action: RPCCallAction, index : Int = -1) : RPCActionDto {
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

        return rpcAction
    }

    private fun setGenerationConfiguration(action: RPCActionDto, index: Int, responseVarName: String){
        // check generation configuration, might be removed later
        action.doGenerateTestScript = config.enablePureRPCTestGeneration && (index != -1)
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
     * @return a string json of a RPC param [dto]
     */
    fun getParamDtoJson(dto: ParamDto) : String {
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
            if (gene.gene is ObjectGene || gene.gene is DateTimeGene){
                dto.innerContent = null
                dto.stringValue = null
            }
            return
        }

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
            is ArrayGene<*> -> {
                val template = dto.type.example?.copy()?:throw IllegalStateException("a template for a collection is null")
                val innerContent = valueGene.getAllElements().map {
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
            is MapGene<*, *> ->{
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
                is IntegerGene -> valueGene.value = dto.stringValue.toInt()
                is DoubleGene -> valueGene.value = dto.stringValue.toDouble()
                is FloatGene -> valueGene.value = dto.stringValue.toFloat()
                is BooleanGene -> valueGene.value = dto.stringValue.toBoolean()
                is StringGene -> valueGene.value = dto.stringValue
                is RegexGene -> {
                    // TODO set value based on RegexGene
                }
                is LongGene -> valueGene.value = dto.stringValue.toLong()
                is EnumGene<*> -> valueGene.index = dto.stringValue.toInt()
                is SeededGene<*> -> {
                    /*
                        response might refer to input dto, then it might exist seeded gene
                     */
                    valueGene.employSeeded = false
                    setGeneBasedOnParamDto(valueGene.gene, dto)
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
                is ArrayGene<*> -> {
                    val template = valueGene.template
                    dto.innerContent.run {
                        if (valueGene.maxSize!=null && valueGene.maxSize!! < size){
                            log.warn("ArrayGene: responses have more elements than it allows, i.e., max is ${valueGene.maxSize} but the actual is ${size}")
                            subList(0, valueGene.maxSize!!)
                        }else
                            this
                    }.forEach { p->
                        val copy = template.copyContent()
                        // TODO need to handle cycle object gene in responses
                        if (copy !is CycleObjectGene){
                            setGeneBasedOnParamDto(copy, p)
                            valueGene.addElement(copy)
                        }
                    }
                }
                is MapGene<*, *> ->{
                    val template = valueGene.template
                    dto.innerContent.run {
                        if (valueGene.maxSize!=null && valueGene.maxSize!! < size){
                            log.warn("MapGene: responses have more elements than it allows, i.e., max is ${valueGene.maxSize} but the actual is ${size}")
                            subList(0, valueGene.maxSize!!)
                        }else
                            this
                    }.forEach { p->
                        val copy = template.copyContent()
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
                else -> throw IllegalStateException("Not support setGeneBasedOnParamDto with gene ${gene::class.java.simpleName} and dto (${dto.type.type})")
            }
        }else{
            if (gene is OptionalGene && dto.isNullable)
                gene.isActive = false
            else
                log.warn("could not retrieve value of ${dto.name?:"untitled"}")
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
            RPCSupportedDataType.UTIL_DATE, RPCSupportedDataType.CUSTOM_OBJECT -> dto.stringValue == null
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
            return isValidToSetValue(valueGene.gene, dto)

        return when(dto.type.type){
            RPCSupportedDataType.P_INT, RPCSupportedDataType.INT,
            RPCSupportedDataType.P_SHORT, RPCSupportedDataType.SHORT,
            RPCSupportedDataType.P_BYTE, RPCSupportedDataType.BYTE -> valueGene is IntegerGene
            RPCSupportedDataType.P_BOOLEAN, RPCSupportedDataType.BOOLEAN -> valueGene is BooleanGene
            RPCSupportedDataType.P_CHAR,
            RPCSupportedDataType.CHAR,
            RPCSupportedDataType.STRING,
            RPCSupportedDataType.BYTEBUFFER -> valueGene is StringGene || (valueGene is RegexGene && dto.pattern != null)
            RPCSupportedDataType.P_DOUBLE, RPCSupportedDataType.DOUBLE -> valueGene is DoubleGene
            RPCSupportedDataType.P_FLOAT, RPCSupportedDataType.FLOAT -> valueGene is FloatGene
            RPCSupportedDataType.P_LONG, RPCSupportedDataType.LONG -> valueGene is LongGene
            RPCSupportedDataType.ENUM -> valueGene is MapGene<*,*> || valueGene is EnumGene<*>
            RPCSupportedDataType.ARRAY, RPCSupportedDataType.SET, RPCSupportedDataType.LIST-> valueGene is ArrayGene<*>
            RPCSupportedDataType.MAP -> valueGene is MapGene<*, *>
            RPCSupportedDataType.CUSTOM_OBJECT -> valueGene is ObjectGene || valueGene is MapGene<*,*>
            RPCSupportedDataType.CUSTOM_CYCLE_OBJECT -> valueGene is CycleObjectGene
            RPCSupportedDataType.UTIL_DATE -> valueGene is DateTimeGene
            RPCSupportedDataType.PAIR -> valueGene is PairGene<*,*>
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

        return RPCCallAction(name, params, responseTemplate = response, response = null )
    }

    private fun actionName(interfaceName: String, endpointName: String) = "$interfaceName:$endpointName"

    private fun handleDtoParam(param: ParamDto): Gene{
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
                        min(param.precision!!, (if (param.scale == 0) param.maxSize else (param.maxSize-1)).toInt())
                    }else null

                    NumericStringGene(name = param.name, minLength = param.minSize?.toInt()?:0, min = param.minValue?.toBigDecimalOrNull(), max = param.maxValue?.toBigDecimalOrNull(),
                        minInclusive = param.minValue == null || param.minInclusive, maxInclusive = param.maxValue == null || param.maxInclusive,
                        precision = p, scale = param.scale)
                }else {
                    if (param.hasNumberConstraints() && param.pattern != null)
                        log.warn("Not support numeric constraints and pattern together yet, and check the param ${param.name}")

                    var strGene : Gene = StringGene(param.name, minLength = param.minSize?.toInt()?:0, maxLength = param.maxSize?.toInt()?:16).apply {

                        // String could have bigDecimal or bigInteger as part of specification if any number related constraint property is specified
                        if (param.precision != null || param.scale != null){
                            specializationGenes.add(BigDecimalGene(param.name, min = param.minValue?.toBigDecimalOrNull(), max = param.maxValue?.toBigDecimalOrNull(),
                                precision = param.precision, scale = param.scale, minInclusive = param.minValue == null || param.minInclusive, maxInclusive = param.maxValue == null || param.maxInclusive))
                        } else if (param.minValue != null || param.maxValue != null){
                            // only max or min, we recognize it as biginteger
                            specializationGenes.add(BigIntegerGene(param.name, min=param.minValue?.toBigIntegerOrNull(), max = param.maxValue?.toBigIntegerOrNull(),
                                minInclusive = param.minValue == null || param.minInclusive, maxInclusive = param.maxValue == null || param.maxInclusive))
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
                                strGene.specializationGenes.add(regex)
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
            RPCSupportedDataType.ARRAY, RPCSupportedDataType.SET, RPCSupportedDataType.LIST-> handleCollectionParam(param)
            RPCSupportedDataType.MAP -> handleMapParam(param)
            RPCSupportedDataType.UTIL_DATE -> handleUtilDate(param)
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

                return DisruptiveGene(param.name, seededGene, 0.0)
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
            return DisruptiveGene(gene.name, gene, 0.0)
        }

        setGeneBasedOnParamDto(gene, defaultValue)
        return DisruptiveGene(gene.name, gene, 0.0)
    }

    private fun handleGeneWithCandidateAsEnumGene(gene: Gene, candidates: List<Gene>) : SeededGene<*>{
        return  when (gene) {
            is StringGene -> SeededGene(gene.name, gene, EnumGene(gene.name, candidates.map { it as StringGene }))
            is IntegerGene -> SeededGene(gene.name, gene, EnumGene(gene.name, candidates.map { it as IntegerGene }))
            is FloatGene ->  SeededGene(gene.name, gene, EnumGene(gene.name, candidates.map { it as FloatGene }))
            is LongGene ->  SeededGene(gene.name, gene, EnumGene(gene.name, candidates.map { it as LongGene }))
            is DoubleGene -> SeededGene(gene.name, gene, EnumGene(gene.name, candidates.map { it as DoubleGene }))
            is BigDecimalGene, is BigIntegerGene -> TODO()
            // might be DateGene
            else -> {
                throw IllegalStateException("Do not support configuring candidates for ${gene::class.java.simpleName} gene type")
            }
        }
    }

    private fun getValueForSeededGene(gene: SeededGene<*>) : String{
        return when (val pGene = gene.getPhenotype()) {
            is StringGene -> pGene.getValueAsRawString()
            is IntegerGene -> pGene.value.toString()
            is FloatGene -> pGene.value.toString()
            is LongGene -> pGene.value.toString()
            is DoubleGene -> pGene.value.toString()
            is BigDecimalGene, is BigIntegerGene -> TODO()
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

    private fun wrapWithOptionalGene(gene: Gene, isOptional: Boolean): Gene{
        return if (isOptional && gene !is OptionalGene) OptionalGene(gene.name, gene) else gene
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

    private fun handleMapParam(param: ParamDto) : Gene{
        val pair = param.type.example
        Lazy.assert { pair.innerContent.size == 2 }
        val keyTemplate = handleDtoParam(pair.innerContent[0])
        val valueTemplate = handleDtoParam(pair.innerContent[1])

        return MapGene(param.name, keyTemplate, valueTemplate, maxSize = param.maxSize?.toInt(), minSize = param.minSize?.toInt())
    }

    private fun handleCollectionParam(param: ParamDto) : Gene{
        val templateParam = when(param.type.type){
            RPCSupportedDataType.ARRAY, RPCSupportedDataType.SET, RPCSupportedDataType.LIST -> param.type.example
            else -> throw IllegalStateException("do not support the collection type: "+ param.type.type)
        }
        val template = handleDtoParam(templateParam)
        return ArrayGene(param.name, template, maxSize = param.maxSize?.toInt(), minSize = param.minSize?.toInt())
    }

    private fun handleObjectType(type: ParamDto): Gene{
        val typeName = type.type.fullTypeNameWithGenericType
        if (type.innerContent.isEmpty()){
            LoggingUtil.uniqueWarn(log, "Object with name (${type.type.fullTypeNameWithGenericType}) has empty fields")
            //return MapGene(typeName, PairGene.createStringPairGene(StringGene( "field"), isFixedFirst = true))
            return ObjectGene(typeName, listOf(), refType = typeName)
        }

        val fields = type.innerContent.map { f-> handleDtoParam(f) }

        return ObjectGene(typeName, fields, refType = typeName)
    }

    private fun handleObjectParam(param: ParamDto): Gene{
        val objType = typeCache[param.type.fullTypeNameWithGenericType]
            ?:throw IllegalStateException("missing ${param.type.fullTypeNameWithGenericType} in typeCache")
        return objType.copy().apply { this.name = param.name }
    }

}