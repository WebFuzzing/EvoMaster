package org.evomaster.core.problem.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.SeededGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.MapGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.numeric.*
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.string.NumericStringGene
import org.evomaster.core.search.gene.string.StringGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ParserDtoUtil {

    private final val objectMapper = ObjectMapper()
    private val log: Logger = LoggerFactory.getLogger(ParserDtoUtil::class.java)

    fun getOrParseDtoWithSutInfo(infoDto: SutInfoDto) : Map<String, Gene>{
        /*
            need to get all for handling `ref`
         */
        val names = infoDto.unitsInfoDto?.parsedDtos?.keys?.toList()?:return emptyMap()
        val schemas = names.map { infoDto.unitsInfoDto.parsedDtos[it]!! }
        //TODO need to check: referType is same with the name?
        val genes = RestActionBuilderV3.createObjectGeneForDTOs(names, schemas, names)
        Lazy.assert { names.size == genes.size }
        return names.mapIndexed { index, s -> s to genes[index] }.toMap()
    }

    fun jsonNodeAsObjectGene(name: String, jsonNode: JsonNode): Gene{
        return when{
            jsonNode.isBoolean -> BooleanGene(name, jsonNode.booleanValue())
            jsonNode.isBigDecimal -> BigDecimalGene(name, jsonNode.decimalValue())
            jsonNode.isDouble -> DoubleGene(name, jsonNode.doubleValue())
            jsonNode.isFloat -> FloatGene(name, jsonNode.floatValue())
            jsonNode.isInt -> IntegerGene(name, jsonNode.intValue())
            jsonNode.isLong -> LongGene(name, jsonNode.longValue())
            jsonNode.isShort -> IntegerGene(name, jsonNode.shortValue().toInt(), min = Short.MIN_VALUE.toInt(), max = Short.MAX_VALUE.toInt())
            jsonNode.isTextual -> {
                StringGene(name, jsonNode.textValue())
            }
            jsonNode.isObject ->{
                val fields = jsonNode.fields().asSequence().map { wrapWithOptionalGene(jsonNodeAsObjectGene(it.key, it.value), true) }.toMutableList()
                ObjectGene(name, fields)
            }
            jsonNode.isArray -> {
                val elements = jsonNode.map { jsonNodeAsObjectGene(name + "_item", it) }
                if (elements.isNotEmpty()){
                    val template = if (elements.any { ParamUtil.getValueGene(it) is ObjectGene })
                        elements.maxByOrNull { (ParamUtil.getValueGene(it) as? ObjectGene)?.fields?.size ?: -1 }!!
                    else elements.first()
                    ArrayGene(name, template = template.copy(), elements = elements.toMutableList())
                } else
                    ArrayGene(name, template = StringGene(name + "_item"))
            }
            else -> throw IllegalStateException("Not support to parse json object with the type ${jsonNode.nodeType.name}")
        }
    }

    fun wrapWithOptionalGene(gene: Gene, isOptional: Boolean): Gene{
        return if (isOptional && gene !is OptionalGene) OptionalGene(gene.name, gene) else gene
    }

    fun setGeneBasedOnString(gene: Gene, stringValue: String?, format : String = "json"){
        val valueGene = ParamUtil.getValueGene(gene)

        if (stringValue != null){
            when(valueGene){
                is IntegerGene -> valueGene.setValueWithRawString(stringValue)
                is DoubleGene -> valueGene.setValueWithRawString(stringValue)
                is FloatGene -> valueGene.setValueWithRawString(stringValue)
                is BooleanGene -> valueGene.setValueWithRawString(stringValue)
                is StringGene -> valueGene.value = stringValue
                is BigDecimalGene -> valueGene.setValueWithRawString(stringValue)
                is BigIntegerGene -> valueGene.setValueWithRawString(stringValue)
                is NumericStringGene -> valueGene.number.setValueWithRawString(stringValue)
                is RegexGene -> {
                    // TODO set value based on RegexGene
                    LoggingUtil.uniqueWarn(log, "do not handle setGeneBasedOnString with stringValue($stringValue) for Regex")
                }
                is LongGene -> valueGene.setValueWithRawString(stringValue)
                is EnumGene<*> -> valueGene.setValueWithRawString(stringValue)
                is SeededGene<*> -> {
                    valueGene.employSeeded = false
                    setGeneBasedOnString(valueGene.gene as Gene, stringValue)
                }
                is PairGene<*, *> -> {
                    throw IllegalStateException("should not really happen since this gene is only")
                }
                is DateTimeGene ->{
                    // TODO
                    LoggingUtil.uniqueWarn(log, "do not handle setGeneBasedOnString with stringValue($stringValue) for DateTimeGene")
                }
                is ArrayGene<*> -> {
                    val template = valueGene.template
                    val node = objectMapper.readTree(stringValue)
                    if (node.isArray){
                        node.run {
                            if (valueGene.maxSize!=null && valueGene.maxSize!! < size()){
                                log.warn("ArrayGene: responses have more elements than it allows, i.e., max is ${valueGene.maxSize} but the actual is ${size()}")
                                this.filterIndexed { index, _ ->  index < valueGene.maxSize!!}
                            }else
                                this
                        }.forEach { p->
                            val copy = template.copy()
                            // TODO need to handle cycle object gene in responses
                            if (copy !is CycleObjectGene){
                                setGeneBasedOnString(copy, p.toPrettyString())
                                valueGene.addElement(copy)
                            }
                        }
                    }else{
                        throw IllegalStateException("stringValue ($stringValue) is not Array")
                    }
                }
                is MapGene<*, *> ->{
                    val template = valueGene.template
                    val node = objectMapper.readTree(stringValue)
                    if (node.isObject){
                        node.fields().asSequence().forEachIndexed {index, p->

                            if (valueGene.maxSize!=null && index >= valueGene.maxSize ){
                                log.warn("MapGene: responses have more elements than it allows, i.e., max is ${valueGene.maxSize} but the actual is ${node.size()}")
                            }else{
                                val copy = template.copy() as PairGene<*, *>
//                                setGeneBasedOnString(copy, p.toPrettyString())
                                setGeneBasedOnString(copy.first, p.key)
                                setGeneBasedOnString(copy.second, p.value.toPrettyString())
                                valueGene.addElement(copy)
                            }
                        }
                    }else{
                        throw IllegalStateException("stringValue ($stringValue) is not Object or Map")
                    }

                }
                is ObjectGene -> {
                    val node = objectMapper.readTree(stringValue)
                    if (node.isObject){
                        valueGene.fields.forEach { f->
                            val pdto = node.fields().asSequence().find { it.key == f.name }
                            //?:throw IllegalStateException("could not find the field (${f.name}) in ParamDto")
                            if (pdto == null && f is OptionalGene)
                                f.isActive = false
                            else if (pdto != null)
                                setGeneBasedOnString(f, pdto.value.toPrettyString())
                            else
                                throw IllegalStateException("could not set value for the field (${f.name})")
                        }
                    }else{
                        throw IllegalStateException("stringValue ($stringValue) is not Object")
                    }
                }
                is CycleObjectGene ->{
                    LoggingUtil.uniqueWarn(log, "NOT support to handle cycle object with more than 2 depth")

                }
                else -> throw IllegalStateException("Not support setGeneBasedOnParamDto with gene ${gene::class.java.simpleName} and stringValue ($stringValue)")
            }
        }else{
            if (gene is OptionalGene)
                gene.isActive = false
            else
                log.warn("could not set null for ${gene.name} with type (${gene::class.java.simpleName})")
        }
    }

}