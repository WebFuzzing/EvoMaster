package org.evomaster.core.problem.rpc.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.problem.RPCProblemDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.httpws.service.param.Param
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.param.RPCInputParam
import org.evomaster.core.problem.rpc.param.RPCReturnParam
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
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
     * key is a name of the endpoint, ie, interface name: action name
     * value is corresponding endpoint schema
     */
    private val actionSchemaCluster = mutableMapOf<String, RPCActionDto>()


    /**
     * key is type in the schema
     * value is object gene for it
     */
    private val typeCache = mutableMapOf<String, Gene>()

    /**
     * reset [actionCluster] based on interface schemas specified in [problem]
     */
    fun initActionCluster(problem: RPCProblemDto, actionCluster: MutableMap<String, Action>){

        problem.schemas.forEach { i->
            i.types.filter { it.type.type == RPCSupportedDataType.CUSTOM_OBJECT }.forEach { t ->
                typeCache[t.type.fullTypeName] = handleObjectType(t)
            }
        }

        actionCluster.clear()
        problem.schemas.forEach { i->
            i.endpoints.forEach{e->
                actionSchemaCluster.putIfAbsent(actionName(i.interfaceId, e.actionId), e)
                val name = actionName(i.interfaceId, e.actionId)
                if (actionCluster.containsKey(name))
                    throw IllegalStateException("$name exists in the actionCluster")
                actionCluster[name] = processEndpoint(name, e)
            }
        }
    }

    fun transformActionDto(action: RPCCallAction) : RPCActionDto {
        // generate RPCActionDto
        val rpcAction = actionSchemaCluster[action.id]?.copy()?: throw IllegalStateException("cannot find the ${action.id} in actionSchemaCluster")

        action.parameters.forEach { p->
            if (p is RPCInputParam){
                p.seeGenes().forEach { g->
                    val paramDto = rpcAction.requestParams.find{ r-> r.name == g.name}?:throw IllegalStateException("cannot find param with a name, ${g.name}")
                    transformGeneToParamDto(g, paramDto)
                }
            }
        }

        return rpcAction
    }

    private fun transformGeneToParamDto(gene: Gene, dto: ParamDto){
        when(gene){
            is IntegerGene -> dto.jsonValue = gene.value.toString()
            is DoubleGene -> dto.jsonValue = gene.value.toString()
            is FloatGene -> dto.jsonValue = gene.value.toString()
            is BooleanGene -> dto.jsonValue = gene.value.toString()
            is StringGene -> dto.jsonValue = gene.value
            is EnumGene<*> -> dto.jsonValue = gene.index.toString()
            is ArrayGene<*> -> {
                val template = dto.type.example?.copy()?:throw IllegalStateException("a template for an array is null")
                val innerContent = gene.getAllElements().map {
                    val copy = template.copy()
                    transformGeneToParamDto(it, copy)
                    copy
                }
                dto.innerContent = innerContent
            }
            else -> TODO("")

        }
    }

    private fun processEndpoint(name: String, endpointSchema: RPCActionDto) : RPCCallAction{
        val params = mutableListOf<Param>()

        endpointSchema.requestParams.forEach { p->
            val gene = handleDtoParam(p).run {
                if (p.isNullable)
                    OptionalGene(this.name, this)
                else
                    this
            }
            params.add(RPCInputParam(p.name, gene))
        }

        // response would be used for assertion generation
        if (endpointSchema.responseParam != null){
            // return should be nullable
            Lazy.assert {
                endpointSchema.responseParam.isNullable
            }
            val gene = OptionalGene(endpointSchema.responseParam.name, handleDtoParam(endpointSchema.responseParam))
            params.add(RPCReturnParam(endpointSchema.responseParam.name, gene))
        }
        /*
            TODO Man exception and auth
         */
        return RPCCallAction(name, params)
    }

    private fun actionName(interfaceName: String, endpointName: String) = "$interfaceName:$endpointName"

    private fun handleDtoParam(param: ParamDto): Gene{
        return when(param.type.type){
            RPCSupportedDataType.P_INT, RPCSupportedDataType.INT -> IntegerGene(param.name)
            RPCSupportedDataType.P_BOOLEAN, RPCSupportedDataType.BOOLEAN -> BooleanGene(param.name)
            RPCSupportedDataType.P_CHAR, RPCSupportedDataType.CHAR -> StringGene(param.name, value="", maxLength = 1, minLength = 0)
            RPCSupportedDataType.P_DOUBLE, RPCSupportedDataType.DOUBLE -> DoubleGene(param.name)
            RPCSupportedDataType.P_FLOAT, RPCSupportedDataType.FLOAT -> FloatGene(param.name)
            RPCSupportedDataType.P_LONG, RPCSupportedDataType.LONG -> LongGene(param.name)
            RPCSupportedDataType.P_SHORT, RPCSupportedDataType.SHORT -> IntegerGene(param.name, min = Short.MIN_VALUE.toInt(), max = Short.MAX_VALUE.toInt())
            RPCSupportedDataType.P_BYTE, RPCSupportedDataType.BYTE -> IntegerGene(param.name, min = Byte.MIN_VALUE.toInt(), max = Byte.MAX_VALUE.toInt())
            RPCSupportedDataType.STRING, RPCSupportedDataType.BYTEBUFFER -> StringGene(param.name)
            RPCSupportedDataType.ENUM -> handleEnumParam(param)
            RPCSupportedDataType.ARRAY, RPCSupportedDataType.SET, RPCSupportedDataType.LIST-> handleCollectionParam(param)
            RPCSupportedDataType.MAP -> handleMapParam(param)
            RPCSupportedDataType.CUSTOM_OBJECT -> handleObjectParam(param)
            RPCSupportedDataType.PAIR -> throw IllegalStateException("ERROR: pair should be handled inside Map")
        }
    }

    private fun handleEnumParam(param: ParamDto): Gene{
        if (param.type.fixedItems.isNullOrEmpty()){
            LoggingUtil.uniqueWarn(log, "Enum with name (${param.type.fullTypeName}) has empty items")
            // TODO check not sure
            return MapGene(param.type.fullTypeName, PairGene.createStringPairGene(StringGene( "NO_ITEM")))
        }
        return EnumGene(param.name, param.type.fixedItems.toList())

    }

    private fun handleMapParam(param: ParamDto) : Gene{
        val pair = param.type.example
        Lazy.assert { pair.innerContent.size == 2 }
        val keyTemplate = handleCollectionParam(pair.innerContent[0])
        val valueTemplate = handleCollectionParam(pair.innerContent[1])
        return MapGene(param.name, keyTemplate, valueTemplate)
    }



    private fun handleCollectionParam(param: ParamDto) : Gene{
        val templateParam = when(param.type.type){
            RPCSupportedDataType.ARRAY, RPCSupportedDataType.SET, RPCSupportedDataType.LIST -> param.type.example
            else -> throw IllegalStateException("")
        }
        val template = handleDtoParam(templateParam)
        return ArrayGene(param.name, template)
    }

    private fun handleObjectType(type: ParamDto): Gene{
        val typeName = type.type.fullTypeName
        if (type.innerContent.isEmpty()){
            LoggingUtil.uniqueWarn(log, "Object with name (${type.type.fullTypeName}) has empty fields")
            return MapGene(typeName, PairGene.createStringPairGene(StringGene( "field"), isFixedFirst = true))
        }

        val fields = type.innerContent.map { f-> handleDtoParam(f) }

        return ObjectGene(typeName, fields, refType = typeName)
    }

    private fun handleObjectParam(param: ParamDto): Gene{
        val objType = typeCache[param.type.fullTypeName] ?:throw IllegalStateException("missing ${param.type.fullTypeName} in typeCache")
        return objType.copy().apply { this.name = param.name }
    }


}