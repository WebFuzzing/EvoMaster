package org.evomaster.core.problem.rest.util

import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestPath
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.problem.rest.resource.ActionRToken
import org.evomaster.core.problem.rest.resource.PathRToken
import org.evomaster.core.problem.rest.resource.RToken
import org.evomaster.core.search.gene.DisruptiveGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.OptionalGene
import java.util.*

/**
 * handling name (with string type) matching
 *
 * NOTE THAT nlp is disabled for the moment in the master branch
 */
object ParserUtil {


    private fun formatKey(source : String) : String = source.toLowerCase()

    fun parsePathTokens(path: RestPath, tokenMap : MutableMap<String, PathRToken>, withParser : Boolean){
//        if (withParser)
//            parsePathTokensWithParser(path, tokenMap)
//        else
            parsePathTokens(path, tokenMap)
    }

    private fun parsePathTokens(path: RestPath, tokenMap : MutableMap<String, PathRToken>){
        var segment = ""
        var nearestParam = -1
        path.getElements().forEachIndexed { index, map ->
            map.forEach { t, isParam ->
                tokenMap.putIfAbsent(
                        formatKey(t),
                        PathRToken(
                                t,
                                t, index,
                                isParam,
                                false,
                                segment,
                                nearestParam))
            }
            if (map.values.any { it }){
                nearestParam = index
                segment = ""
            }else{
                assert(map.keys.size == 1)
                segment = map.keys.first()
            }
        }
    }


    /**
     * parser description and summary of action, and generate a set of [ActionRToken] on [map]
     */
    fun parseAction(action: RestCallAction, description: String, map : MutableMap<String, ActionRToken>){
        if(description.isNotBlank())
            parseActionTokensByDes(description, map)
        parseActionTokensByParam(action.parameters, map)
    }
    private fun parseActionTokensByDes(description: String, map : MutableMap<String, ActionRToken>, simpleParser : Boolean = true){

        if(simpleParser) {
            val tokens = description.replace(".","").split(" ")
            tokens.forEach {
                map.putIfAbsent(formatKey(it), ActionRToken(formatKey(it), formatKey(it), false, false, false, false))
            }
        }else{
            throw IllegalStateException("parser is disabled for the moment")
//            val tokens = getNounTokens(description)
//            tokens.forEach {
//                map.putIfAbsent(formatKey(it.originalText()), ActionRToken(it.originalText(), it.lemma(), false, false, false, isVerbByTag(it.tag())))
//            }
        }
    }

    private fun findRToken(key : String, map: MutableMap<String, out RToken>) : RToken?{
        map[key.toLowerCase()]?.let { return it }
        return map.values.find { it.isKey(key) }

    }


    private fun parseActionTokensByParam(params : MutableList<Param>, map : MutableMap<String, ActionRToken>){
        params.filter {p-> p is BodyParam || p is PathParam || p is QueryParam}.forEach {p->
            handleParam(p, map)
        }
    }

    private fun handleParam(param: Param, map: MutableMap<String, ActionRToken>){
        val typeGene = getFirstTypeGene(param.gene)
        if (typeGene is ObjectGene){
            /*
            name is always 'body' for BodyParam (based on RestActionBuilder), then we ignore to record this info.
             */
            handleObjectGene(typeGene, map, true, null, true)
        }else{
            handleTypeGene(typeGene, map, true)
        }
    }


    private fun getFirstTypeGene(gene : Gene) : Gene{
        if(gene is ObjectGene) return gene
        else if(gene is DisruptiveGene<*>){
            return getFirstTypeGene(gene.gene)
        }else if(gene is OptionalGene){
            return getFirstTypeGene(gene.gene)
        }else return gene
    }

    /**
     * @param gene is not ObjectGene
     */
    private fun handleTypeGene(gene: Gene, map: MutableMap<String, ActionRToken>, isDirect : Boolean){
        assert(gene !is ObjectGene)
        handleParamName(gene.name, map, isDirect)
    }

    private fun handleParamName(paramName : String, map: MutableMap<String, ActionRToken>, isDirect: Boolean){
        val token = findRToken(paramName, map)
        if(token != null){
            (token as ActionRToken).fromDefinition = true
            token.isType = false
            if(isDirect) token.isDirect = isDirect
        }else{
            map.putIfAbsent(
                    formatKey(paramName),
                    ActionRToken(paramName,
                            paramName,//getNlpTokens(paramName)[0].lemma(),
                            fromDefinition = true, isType = false, isDirect = isDirect))
        }
    }

    private fun handleObjectGene(objectGene: ObjectGene, map: MutableMap<String, ActionRToken>, isDirect : Boolean, name : String?, deeperParam: Boolean){
        val refType = objectGene.refType
        if( refType != null){
            val token = findRToken(refType, map)
            if(token != null){
                (token as ActionRToken).fromDefinition = true
                token.isType = true
                if(isDirect) token.isDirect = isDirect //avoid to change isDirect from true -> false
                if(name != null) token.alternativeNames.add(name)
            }else{
                map.putIfAbsent(
                        formatKey(refType),
                        ActionRToken(refType,
                                refType, //getNlpTokens(refType)[0].lemma(),
                                fromDefinition = true, isType = true, isDirect = isDirect).also {
                    if(name != null) it.alternativeNames.add(name)
                })
            }
            map.getValue(formatKey(refType)).fields.apply {
                clear()
                addAll(objectGene.fields.map { it.name})
            }
        }

        if(deeperParam){
            objectGene.fields.forEach { fg->
                val obj = getFirstTypeGene(fg)
                if(obj is ObjectGene){
                    handleObjectGene(obj, map, false, fg.name, false)
                }
                handleParamName(fg.name, map, false)
            }
        }
    }

}

