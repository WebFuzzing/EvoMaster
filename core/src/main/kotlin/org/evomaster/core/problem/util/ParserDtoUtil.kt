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
import org.evomaster.core.search.gene.collection.*
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.numeric.*
import org.evomaster.core.search.gene.optional.FlexibleGene
import org.evomaster.core.search.gene.optional.NullableGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.string.NumericStringGene
import org.evomaster.core.search.gene.string.StringGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ParserDtoUtil {

    private val objectMapper = ObjectMapper()
    private val log: Logger = LoggerFactory.getLogger(ParserDtoUtil::class.java)

    fun getJsonNodeFromText(text: String) : JsonNode?{
        return try {
            objectMapper.readTree(text)
        }catch (e: Exception){
            null
        }
    }

    /**
     * get or parse schema of dto classes from [infoDto] as gene
     * @return a map of dto class name to corresponding gene
     */
    fun getOrParseDtoWithSutInfo(infoDto: SutInfoDto,
                                 enableConstraintHandling: Boolean) : Map<String, Gene>{
        /*
            need to get all for handling `ref`
         */
        val names = infoDto.unitsInfoDto?.parsedDtos?.keys?.toList()?:return emptyMap()
        val schemas = names.map { infoDto.unitsInfoDto.parsedDtos[it]!! }
        //TODO need to check: referType is same with the name?
        val genes = RestActionBuilderV3.createGenesForDTOs(names, schemas, names, RestActionBuilderV3.Options(enableConstraintHandling=enableConstraintHandling))
        Lazy.assert { names.size == genes.size }
        return names.mapIndexed { index, s -> s to genes[index] }.toMap()
    }

    /**
     * parse gene based on json node
     */
    fun parseJsonNodeAsGene(name: String, jsonNode: JsonNode): Gene{
        return parseJsonNodeAsGene(name, jsonNode, null)
            ?:throw IllegalStateException("Fail to parse the given json node: ${jsonNode.toPrettyString()}")
    }


    /**
     * parse gene based on json node and optionally employ given [objectGeneCluster] to parse object gene
     */
    fun parseJsonNodeAsGene(name: String, jsonNode: JsonNode, objectGeneCluster: Map<String, Gene>?): Gene?{
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
            jsonNode.isArray -> {
                val elements = jsonNode.map { parseJsonNodeAsGene(name + "_item", it, objectGeneCluster) }
                if (elements.any { it == null })
                    return null

                if (elements.isNotEmpty()){
                    val template = if (elements.any { ParamUtil.getValueGene(it!!) is ObjectGene })
                        elements.maxByOrNull { (ParamUtil.getValueGene(it!!) as? ObjectGene)?.fields?.size ?: -1 }!!
                    else elements.first()
                    ArrayGene(name, template = template!!.copy())
                } else
                    ArrayGene(name, template = StringGene(name + "_item"))
            }
            jsonNode.isObject ->{
                if (objectGeneCluster == null){
                    inferGeneBasedOnJsonNode(name, jsonNode, objectGeneCluster)?: return null
                }else {
                    (findAndCopyExtractedObjectDto(jsonNode, objectGeneCluster)
                        ?: inferGeneBasedOnJsonNode(name, jsonNode, objectGeneCluster))?:return null
                }
            }
            jsonNode.isNull -> {
                Lazy.assert {  objectGeneCluster == null }
                // TODO change it to NullGene later
                return NullableGene(name, StringGene(name)).also { it.isActive = false }
            }
            else -> throw IllegalStateException("Not support to parse json object with the type ${jsonNode.nodeType.name}")
        }
    }

    private fun inferGeneBasedOnJsonNode(name: String, jsonNode : JsonNode, objectGeneCluster: Map<String, Gene>?) : Gene?{
        if (jsonNode.size() == 0)
            return FixedMapGene(name, StringGene("key"), StringGene("value"))

        val values = jsonNode.fields().asSequence().map { parseJsonNodeAsGene(it.key, it.value, objectGeneCluster) }.toMutableList()
        if (values.any { it == null })
            return null
//        val groupedValues  = values.filterNotNull().groupBy { g->
//            val v = ParamUtil.getValueGene(g)
//            if (v is ObjectGene) v.refType?:(v.fields.joinToString("-") { f->f.name }) else v::class.java.name
//        }
        return FlexibleMapGene(name, StringGene("key"), values.first()!!.copy(), null)
    }

    private fun findAndCopyExtractedObjectDto(node: JsonNode, objectGeneMap: Map<String, Gene>) : ObjectGene? {
        return objectGeneMap.values.filterIsInstance<ObjectGene>().firstOrNull { o->
            var all = true
            node.fields().forEach { f ->
                all = all && o.fields.any { of -> of.name.equals(f.key, ignoreCase = true) }
            }
            all
        }?.copy() as? ObjectGene
    }

    /**
     * wrap the given [gene] with OptionalGene if the gene is not
     */
    fun wrapWithOptionalGene(gene: Gene, isOptional: Boolean): Gene{
        return if (isOptional && gene !is OptionalGene) OptionalGene(gene.name, gene) else gene
    }

    /**
     * set value of gene based on [stringValue] with json format
     */
    fun setGeneBasedOnString(gene: Gene, stringValue: String?){
        val valueGene = ParamUtil.getValueGene(gene)

        if (stringValue != null && !stringValue.equals("null", ignoreCase = true)){
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
                                setGeneBasedOnString(copy, getTextForStringGene(copy, p))
                                valueGene.addElement(copy)
                            }
                        }
                    }else{
                        throw IllegalStateException("stringValue ($stringValue) is not Array")
                    }
                }
                is FixedMapGene<*, *> ->{
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
                                setGeneBasedOnString(copy.second, getTextForStringGene(copy.second, p.value))
                                valueGene.addElement(copy)
                            }
                        }
                    }else{
                        throw IllegalStateException("stringValue ($stringValue) is not Object or Map")
                    }

                }
                is FlexibleMapGene<*> -> {
                    val template = valueGene.template
                    val node = objectMapper.readTree(stringValue)
                    if (node.isObject){
                        node.fields().asSequence().forEachIndexed {index, p->

                            if (valueGene.maxSize!=null && index >= valueGene.maxSize ){
                                log.warn("MapGene: responses have more elements than it allows, i.e., max is ${valueGene.maxSize} but the actual is ${node.size()}")
                            }else{
                                val copy = template.copy() as PairGene<*, FlexibleGene>
                                setGeneBasedOnString(copy.first, p.key)

                                val fvalueGene = parseJsonNodeAsGene("flexibleValue", p.value)
                                copy.second.replaceGeneTo(fvalueGene)
                                setGeneBasedOnString(fvalueGene, getTextForStringGene(fvalueGene, p.value))
                                valueGene.addElement(copy)
                            }
                        }
                    }else{
                        throw IllegalStateException("stringValue ($stringValue) is not Object")
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
                                setGeneBasedOnString(f, getTextForStringGene(f, pdto.value))
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
            when (gene) {
                is OptionalGene -> gene.isActive = false
                is NullableGene -> gene.isActive = false
                else -> log.warn("could not set null for ${gene.name} with type (${gene::class.java.simpleName})")
            }
        }
    }

    private fun getTextForStringGene(gene: Gene, node: JsonNode) : String{
        if (ParamUtil.getValueGene(gene).run { this is StringGene || this is EnumGene<*> || this is NumberGene<*>} && node.isTextual)
            return node.asText()
        return node.toPrettyString()
    }

}