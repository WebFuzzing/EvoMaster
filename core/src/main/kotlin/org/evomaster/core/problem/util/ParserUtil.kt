package org.evomaster.core.problem.rest.util

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern
import edu.stanford.nlp.pipeline.StanfordCoreNLP
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
 */
object ParserUtil {

    private const val REGEX_NOUN = "([{pos:/NN|NNS|NNP/}])"
    private const val REGEX_VERB = "([{pos:/VB|VBD|VBG|VBN|VBP|VBZ/}])"

    private val PATTERN_NOUN = TokenSequencePattern.compile(REGEX_NOUN)
    private val PATTERN_VERB = TokenSequencePattern.compile(REGEX_VERB)

    /**
     * configure stanford parser
     */
    private var PIPELINE : StanfordCoreNLP? = null

    private fun getPipeline(): StanfordCoreNLP {
        if (PIPELINE == null) {
            PIPELINE = StanfordCoreNLP(object : Properties() {
                init {
                    setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse")
                }
            })
        }
        return PIPELINE!!
    }

    private fun formatKey(source : String) : String = source.toLowerCase()

    fun parsePathTokens(path: RestPath, tokenMap : MutableMap<String, PathRToken>, withParser : Boolean){
        if (withParser)
            parsePathTokensWithParser(path, tokenMap)
        else
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
     * parser path of resource, and generate a set of [PathRToken] on [tokenMap]
     */
    private fun parsePathTokensWithParser(path: RestPath, tokenMap : MutableMap<String, PathRToken>){
        val nlpPath = path.getElements().flatMap { it.keys }.joinToString(" ")
        val tokens = getNlpTokens(nlpPath)

        var segment = ""
        var nearestParam = -1
        path.getElements().forEachIndexed { index, map ->
            map.forEach { eToken, isParam ->
                if(isParam){
                    val ptoken = tokens.find { it.originalText() == eToken }
                            ?:throw IllegalArgumentException("cannot find $eToken")
                    tokenMap.putIfAbsent(
                            formatKey(ptoken.originalText()),
                            PathRToken(
                                    ptoken.originalText(),
                                    ptoken.lemma(), index,
                                    true,
                                    isVerbByTag(ptoken.tag()!!),
                                    segment,
                                    nearestParam))
                }else{
                    segment = ParamUtil.appendParam(segment, eToken)

                    val token = tokens.find { it.originalText() == eToken }
                    if(token == null){
                        val comrPathToken = PathRToken(eToken, eToken, index, false, false, segment)
                        var innerSegment = segment
                        tokens.filter { eToken.contains(it.originalText()) }
                                .forEach { t->
                                    innerSegment += t.originalText()
                                    comrPathToken.subTokens.add(PathRToken(t.originalText(), t.lemma(), index, false, isVerbByTag(t.tag()!!), innerSegment, nearestParam))
                                }

                        tokenMap.putIfAbsent(
                                formatKey(eToken),
                                comrPathToken)
                    }else{
                        tokenMap.putIfAbsent(
                                formatKey(token.originalText()),
                                PathRToken(token.originalText(), token.lemma(), index, false, isVerbByTag(token.tag()!!), segment, nearestParam))

                    }
                }
            }


            if(map.values.any { it }){
                nearestParam = index
                segment = ""
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
            val tokens = getNounTokens(description)
            tokens.forEach {
                map.putIfAbsent(formatKey(it.originalText()), ActionRToken(it.originalText(), it.lemma(), false, false, false, isVerbByTag(it.tag())))
            }
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
            map.putIfAbsent(formatKey(paramName), ActionRToken(paramName, getNlpTokens(paramName)[0].lemma(), fromDefinition = true, isType = false, isDirect = isDirect))
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
                map.putIfAbsent(formatKey(refType), ActionRToken(refType, getNlpTokens(refType)[0].lemma(), fromDefinition = true, isType = true, isDirect = isDirect).also {
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

    private fun isVerbByTag(wordTag: String):Boolean = wordTag.contains("VB")


    private fun getNlpTokens(text : String) : List<CoreLabel>{
        val sentences = getPipeline().process(text).get(CoreAnnotations.SentencesAnnotation::class.java)
        if(sentences.size > 0)
            return sentences.flatMap { it.get(CoreAnnotations.TokensAnnotation::class.java) }
        else
            throw IllegalArgumentException("Input text is not single sentence. The text is $text")
    }

    private fun getNounTokens(text : String) : List<CoreLabel>{
        if(text.isNotBlank()){
            val tokens = getNlpTokens(text)

            val resultNouns = getMatched(PATTERN_NOUN, tokens)
            val resultVerbs = getMatched(PATTERN_VERB, tokens)

            return tokens.filter { resultNouns.plus(resultVerbs).contains(it.originalText()) }
        }
        return mutableListOf()
    }

    private fun getMatched(pattern: TokenSequencePattern, tokens : List<CoreLabel>) : List<String>{
        val matcher = pattern.matcher(tokens)
        val result = mutableListOf<String>()
        while (matcher.find()){
            result.add(matcher.group())
        }
        return result.toList()
    }

}

