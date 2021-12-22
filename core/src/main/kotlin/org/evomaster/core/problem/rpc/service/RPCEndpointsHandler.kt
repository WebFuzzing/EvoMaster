package org.evomaster.core.problem.rpc.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.problem.RPCProblemDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.api.service.param.Param
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.param.RPCParam
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * this class is used to manage formulated individual with schemas of SUT
 */
class RPCEndpointsHandler {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RPCEndpointsHandler::class.java)
    }

    @Inject
    protected lateinit var configuration: EMConfig

    /**
     * key is an id of the endpoint, ie, interface name: action name
     * value is corresponding endpoint schema
     */
    private val actionSchemaCluster = mutableMapOf<String, RPCActionDto>()


    /**
     * key is type in the schema
     * value is object gene for it
     */
    private val typeCache = mutableMapOf<String, Gene>()

    private val objectMapper = ObjectMapper()

    /**
     * @param actionId is an id of the endpoint
     * @return action dto which contains info for its execution, eg, client, method name in the interface
     */
    fun getActionDto(actionId : String) : RPCActionDto{
        return actionSchemaCluster[actionId]?: throw IllegalStateException("could not find the $actionId")
    }

    /**
     * reset [actionCluster] based on interface schemas specified in [problem]
     */
    fun initActionCluster(problem: RPCProblemDto, actionCluster: MutableMap<String, Action>){

        problem.schemas.forEach { i->
            i.types.sortedBy { it.type.depth }
                .filter { it.type.type == RPCSupportedDataType.CUSTOM_OBJECT }.forEach { t ->
                typeCache[t.type.fullTypeName] = handleObjectType(t)
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
            }
        }

        // report statistic of endpoints
        reportEndpointsStatistics(problem.schemas.size)
    }

    private fun reportEndpointsStatistics(numSchema: Int){
        LoggingUtil.getInfoLogger().apply {
            info("There are $numSchema defined RPC interfaces with ${actionSchemaCluster.size} accessible endpoints.")
        }
    }

    /**
     * get rpc action dto based on specified [action]
     */
    fun transformActionDto(action: RPCCallAction) : RPCActionDto {
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

        return rpcAction
    }

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

    fun getParamDtoJson(dto: ParamDto) : String {
        return objectMapper.writeValueAsString(dto)
    }

    private fun transformGeneToParamDto(gene: Gene, dto: ParamDto){

        if (gene is OptionalGene && !gene.isActive){
            // set null value
            if (gene.gene is ObjectGene || gene.gene is DateTimeGene){
                dto.innerContent = null
            }
            return
        }

        when(val valueGene = ParamUtil.getValueGene(gene)){
            is IntegerGene -> dto.stringValue = valueGene.value.toString()
            is DoubleGene -> dto.stringValue = valueGene.value.toString()
            is FloatGene -> dto.stringValue = valueGene.value.toString()
            is BooleanGene -> dto.stringValue = valueGene.value.toString()
            is StringGene -> dto.stringValue = valueGene.getValueAsRawString()
            is EnumGene<*> -> dto.stringValue = valueGene.index.toString()
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
                val template = dto.type.example?.copy()?:throw IllegalStateException("a template for a pair is null")
                Lazy.assert { template.innerContent.size == 2 }
                val first = template.innerContent[0]
                transformGeneToParamDto(valueGene.first, first)
                val second = template.innerContent[1]
                transformGeneToParamDto(valueGene.first, second)
                dto.innerContent = listOf(first, second)
            }
            is MapGene<*, *> ->{
                val template = dto.type.example?.copy()?:throw IllegalStateException("a template for a map dto is null")
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
        if (!isValidToSetValue(gene, dto))
            throw IllegalStateException("the types of gene and its dto are mismatched, i.e., gene (${gene::class.java.simpleName}) vs. dto (${dto.type.type})")
        val valueGene = ParamUtil.getValueGene(gene)

        if (!isNullDto(dto)){
            when(valueGene){
                is IntegerGene -> valueGene.value = dto.stringValue.toInt()
                is DoubleGene -> valueGene.value = dto.stringValue.toDouble()
                is FloatGene -> valueGene.value = dto.stringValue.toFloat()
                is BooleanGene -> valueGene.value = dto.stringValue.toBoolean()
                is StringGene -> valueGene.value = dto.stringValue
                is LongGene -> valueGene.value = dto.stringValue.toLong()
                is EnumGene<*> -> valueGene.index = dto.stringValue.toInt()
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
                    dto.innerContent.forEach { p->
                        val copy = template.copyContent()
                        setGeneBasedOnParamDto(copy, p)
                        valueGene.addElement(copy)
                    }
                }
                is MapGene<*, *> ->{
                    val template = valueGene.template
                    dto.innerContent.forEach { p->
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
        return when(dto.type.type){
            RPCSupportedDataType.P_INT, RPCSupportedDataType.INT,
            RPCSupportedDataType.P_SHORT, RPCSupportedDataType.SHORT,
            RPCSupportedDataType.P_BYTE, RPCSupportedDataType.BYTE -> valueGene is IntegerGene
            RPCSupportedDataType.P_BOOLEAN, RPCSupportedDataType.BOOLEAN -> valueGene is BooleanGene
            RPCSupportedDataType.P_CHAR, RPCSupportedDataType.CHAR, RPCSupportedDataType.STRING, RPCSupportedDataType.BYTEBUFFER -> valueGene is StringGene
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
        }
    }

    private fun processEndpoint(name: String, endpointSchema: RPCActionDto) : RPCCallAction{
        val params = mutableListOf<Param>()

        endpointSchema.requestParams.forEach { p->
            val gene = handleDtoParam(p)
            params.add(RPCParam(p.name, gene))
        }

        var response: RPCParam? = null
        // response would be used for assertion generation
        if (endpointSchema.responseParam != null){
            val gene = handleDtoParam(endpointSchema.responseParam)
            response = RPCParam(endpointSchema.responseParam.name, gene)
        }
        /*
            TODO Man exception and auth
         */
        return RPCCallAction(name, params, responseTemplate = response, response = null )
    }

    private fun actionName(interfaceName: String, endpointName: String) = "$interfaceName:$endpointName"

    private fun handleDtoParam(param: ParamDto): Gene{
        val gene = when(param.type.type){
            RPCSupportedDataType.P_INT, RPCSupportedDataType.INT -> IntegerGene(param.name, min = param.minValue?.toInt()?: Int.MIN_VALUE, max = param.maxValue?.toInt()?:Int.MAX_VALUE)
            RPCSupportedDataType.P_BOOLEAN, RPCSupportedDataType.BOOLEAN -> BooleanGene(param.name)
            RPCSupportedDataType.P_CHAR, RPCSupportedDataType.CHAR -> StringGene(param.name, value="", maxLength = 1, minLength = param.minSize?.toInt()?:0)
            RPCSupportedDataType.P_DOUBLE, RPCSupportedDataType.DOUBLE -> DoubleGene(param.name, min = param.minValue?.toDouble(), max = param.maxValue?.toDouble())
            RPCSupportedDataType.P_FLOAT, RPCSupportedDataType.FLOAT -> FloatGene(param.name, min = param.minValue?.toFloat(), max = param.maxValue?.toFloat())
            RPCSupportedDataType.P_LONG, RPCSupportedDataType.LONG -> LongGene(param.name, min = param.minValue, max = param.maxValue)
            RPCSupportedDataType.P_SHORT, RPCSupportedDataType.SHORT -> IntegerGene(param.name, min = param.minValue?.toInt()?:Short.MIN_VALUE.toInt(), max = param.maxValue?.toInt()?:Short.MAX_VALUE.toInt())
            RPCSupportedDataType.P_BYTE, RPCSupportedDataType.BYTE -> IntegerGene(param.name, min = param.minValue?.toInt()?:Byte.MIN_VALUE.toInt(), max = param.maxValue?.toInt()?:Byte.MAX_VALUE.toInt())
            RPCSupportedDataType.STRING, RPCSupportedDataType.BYTEBUFFER -> StringGene(param.name).apply {
                if (param.minValue != null || param.maxValue != null){
                    // add specification based on constraint info
                    specializationGenes.add(LongGene(param.name, min=param.minValue, max = param.maxValue))
                }
            }
            RPCSupportedDataType.ENUM -> handleEnumParam(param)
            RPCSupportedDataType.ARRAY, RPCSupportedDataType.SET, RPCSupportedDataType.LIST-> handleCollectionParam(param)
            RPCSupportedDataType.MAP -> handleMapParam(param)
            RPCSupportedDataType.UTIL_DATE -> handleUtilDate(param)
            RPCSupportedDataType.CUSTOM_OBJECT -> handleObjectParam(param)
            RPCSupportedDataType.CUSTOM_CYCLE_OBJECT -> CycleObjectGene(param.name)
            RPCSupportedDataType.PAIR -> throw IllegalStateException("ERROR: pair should be handled inside Map")
        }

        return wrapWithOptionalGene(gene, param.isNullable)
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
            return MapGene(param.type.fullTypeName, PairGene.createStringPairGene(StringGene( "NO_ITEM")), maxSize = 0)
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
        val typeName = type.type.fullTypeName
        if (type.innerContent.isEmpty()){
            LoggingUtil.uniqueWarn(log, "Object with name (${type.type.fullTypeName}) has empty fields")
            // shall we set maxSize is 0 here?
            return MapGene(typeName, PairGene.createStringPairGene(StringGene( "field"), isFixedFirst = true))
        }

        val fields = type.innerContent.map { f-> handleDtoParam(f) }

        return ObjectGene(typeName, fields, refType = typeName)
    }

    private fun handleObjectParam(param: ParamDto): Gene{
        val objType = typeCache[param.type.fullTypeName]
            ?:throw IllegalStateException("missing ${param.type.fullTypeName} in typeCache")
        return objType.copy().apply { this.name = param.name }
    }

}