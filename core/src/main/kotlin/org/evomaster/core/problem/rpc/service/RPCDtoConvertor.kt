package org.evomaster.core.problem.rpc.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.client.java.controller.api.dto.problem.RPCProblemDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCActionDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCSupportedDataType
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.httpws.service.param.Param
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.param.PRCInputParam
import org.evomaster.core.problem.rpc.param.PRCReturnParam
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.regex.RegexGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * this class is used to manage formulated individual with schemas of SUT
 */
class RPCDtoConvertor {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RPCDtoConvertor::class.java)
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

    fun transformActionDto(action: RPCCallAction, index: Int) : RPCActionDto {
        // generate RPCActionDto
        val rpc = RPCActionDto()
        TODO("")

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
            params.add(PRCInputParam(p.name, gene))
        }

        // response would be used for assertion generation
        if (endpointSchema.responseParam != null){
            // return should be nullable
            Lazy.assert {
                endpointSchema.responseParam.isNullable
            }
            val gene = OptionalGene(endpointSchema.responseParam.name, handleDtoParam(endpointSchema.responseParam))
            params.add(PRCReturnParam(endpointSchema.responseParam.name, gene))
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
            RPCSupportedDataType.P_BOOLEAN, RPCSupportedDataType.BOOLEAN, RPCSupportedDataType.P_BYTE, RPCSupportedDataType.BYTE -> BooleanGene(param.name)
            RPCSupportedDataType.P_CHAR, RPCSupportedDataType.CHAR -> StringGene(param.name, value="", maxLength = 1, minLength = 0)
            RPCSupportedDataType.P_DOUBLE, RPCSupportedDataType.DOUBLE -> DoubleGene(param.name)
            RPCSupportedDataType.P_FLOAT, RPCSupportedDataType.FLOAT -> FloatGene(param.name)
            RPCSupportedDataType.P_LONG, RPCSupportedDataType.LONG -> LongGene(param.name)
            RPCSupportedDataType.P_SHORT, RPCSupportedDataType.SHORT -> IntegerGene(param.name, min = Short.MIN_VALUE.toInt(), max = Short.MAX_VALUE.toInt())
            RPCSupportedDataType.STRING -> StringGene(param.name)
            RPCSupportedDataType.ENUM -> handleEnumParam(param)
            RPCSupportedDataType.ARRAY, RPCSupportedDataType.SET, RPCSupportedDataType.LIST, RPCSupportedDataType.BYTEBUFFER -> handleCollectionParam(param)
            RPCSupportedDataType.MAP -> handleMapParam(param)
            RPCSupportedDataType.CUSTOM_OBJECT -> handleObjectParam(param)
            RPCSupportedDataType.PAIR -> throw IllegalStateException("ERROR: pair should be handled inside Map")
        }
    }

    private fun handleEnumParam(param: ParamDto): Gene{
        if (param.type.fixedItems.isNullOrEmpty()){
            LoggingUtil.uniqueWarn(log, "Enum with name (${param.type.fullTypeName}) has empty items")
            // TODO check not sure
            return MapGene(param.type.fullTypeName, StringGene("item"))
        }
        return EnumGene(param.name, param.type.fixedItems.toList())

    }

    private fun handleMapParam(param: ParamDto) : Gene{
        val pair = param.type.example
        Lazy.assert { pair.innerContent.size == 2 }
        val keyTemplate = handleCollectionParam(pair.innerContent[0])
        val valueTemplate = handleCollectionParam(pair.innerContent[1])
//        return MapGene(param.name, key, value)
        TODO("wait until map-extend branch is merged")
    }



    private fun handleCollectionParam(param: ParamDto) : Gene{
        val templateParam = when(param.type.type){
            RPCSupportedDataType.ARRAY, RPCSupportedDataType.SET, RPCSupportedDataType.LIST, RPCSupportedDataType.BYTEBUFFER -> param.type.example
            else -> throw IllegalStateException("")
        }
        val template = handleDtoParam(templateParam)
        return ArrayGene(param.name, template)
    }

    private fun handleObjectType(type: ParamDto): Gene{
        val typeName = type.type.fullTypeName
        if (type.innerContent.isEmpty()){
            LoggingUtil.uniqueWarn(log, "Object with name (${type.type.fullTypeName}) has empty fields")
            return MapGene(typeName, StringGene("field"))
        }

        val fields = type.innerContent.map { f-> handleDtoParam(f) }

        return ObjectGene(typeName, fields, refType = typeName)
    }

    private fun handleObjectParam(param: ParamDto): Gene{
        val objType = typeCache[param.type.fullTypeName] ?:throw IllegalStateException("missing ${param.type.fullTypeName} in typeCache")
        return objType.copy().apply { this.name = param.name }
    }


}