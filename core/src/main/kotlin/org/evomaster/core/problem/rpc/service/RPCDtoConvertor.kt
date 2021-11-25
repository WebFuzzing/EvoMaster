package org.evomaster.core.problem.rpc.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.problem.RPCProblemDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.EndpointSchema
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params.*
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.ObjectType
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
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
    private val actionSchemaCluster = mutableMapOf<String, EndpointSchema>()


    /**
     * key is type in the schema
     * value is object gene for it
     */
    private val typeCache = mutableMapOf<String, Gene>()

    fun initActionCluster(problem: RPCProblemDto, actionCluster: MutableMap<String, Action>){

        problem.schemas.forEach { i->
            i.typeCollections.filterValues { it is ObjectType }.forEach { (t, u) ->
                typeCache[t] = handleObjectType(u as ObjectType)
            }
        }

        problem.schemas.forEach { i->
            i.endpoints.forEach{e->
                actionSchemaCluster.putIfAbsent(actionName(i.name, e.name), e)
                TODO("set ")
            }
        }
    }

    private fun actionName(interfaceName: String, endpointName: String) = "$interfaceName:$endpointName"


    private fun handleDtoParam(param: NamedTypedValue<*,*>): Gene{
        return when(param){
            is PrimitiveOrWrapperParam<*> -> handlePrimitiveTypes(param)
            is StringParam -> handleStringParam(param)
            is ByteBufferParam -> handleByteBufferParam(param)
            is ArrayParam, is SetParam, is ListParam -> handleCollectionParam(param)
            is EnumParam -> handleEnumParam(param)
            is MapParam -> handleMapParam(param)
            is ObjectParam -> handleObjectParam(param)
            else -> throw IllegalStateException("missing handling the parameter, ie, name: ${param.name}, type: ${param.type}")
        }
    }

    private fun handleEnumParam(param: EnumParam): Gene{
        return EnumGene(param.name, param.type.items.toList())
    }

    private fun handleMapParam(param: MapParam) : Gene{
        val key = handleDtoParam(param.type.keyTemplate)
        val value = handleDtoParam(param.type.valueTemplate)
//        return MapGene(param.name, key, value)
        TODO()
    }


    private fun handleStringParam(param: StringParam) : Gene{
        return StringGene(param.name)
    }

    private fun handlePrimitiveTypes(param: PrimitiveOrWrapperParam<*>) : Gene{
        return when(param){
            is BooleanParam, is ByteParam -> BooleanGene(param.name)
            is CharParam -> StringGene(param.name, value="", maxLength = 1, minLength = 0)
            is DoubleParam -> DoubleGene(param.name)
            is FloatParam -> FloatGene(param.name)
            is IntParam -> IntegerGene(param.name)
            is LongParam -> LongGene(param.name)
            else -> throw IllegalStateException("missing handling the PrimitiveOrWrapperParam, ie, name: ${param.name}, type: ${param.type}")
        }

    }

    private fun handleByteBufferParam(param: ByteBufferParam): Gene{
        val template = handleDtoParam(param.type.template)
        Lazy.assert { template is BooleanGene }
        return ArrayGene(param.name, template)
    }

    private fun handleCollectionParam(param: NamedTypedValue<*,*>) : Gene{
        val templateParam = when(param){
            is ArrayParam -> param.type.template
            is SetParam -> param.type.template
            is ListParam -> param.type.template
            else -> throw IllegalStateException("")
        }
        val template = handleDtoParam(templateParam)
        return ArrayGene(param.name, template)
    }

    private fun handleObjectType(type: ObjectType): Gene{
        val fields = type.fields.map { handleDtoParam(it) }
        return ObjectGene(type.type, fields, refType = type.fullTypeName)
    }

    private fun handleObjectParam(param: ObjectParam): Gene{
        val obj = typeCache[param.type.fullTypeName] ?:throw IllegalStateException("missing ${param.type.fullTypeName} in typeCache")
        return obj.copy()
    }


}