package org.evomaster.core.problem.rest2.resources.token.parser


import io.swagger.models.Swagger
import io.swagger.parser.SwaggerParser

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import java.util.*
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.param.*
import org.evomaster.core.search.gene.DisruptiveGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.OptionalGene
import java.util.ArrayList

class ParserUtil {

    companion object {
        private const val REGEX_NOUN = "([{pos:/NN|NNS|NNP/}])"
        private const val REGEX_VERB = "([{pos:/VB|VBD|VBG|VBN|VBP|VBZ/}])"

        const val SimilarityThreshold = 0.6

//        private const val REGEX = "([{pos:/NN|NNS|NNP|VB|VBD|VBG|VBN|VBP|VBZ/}])"
//        private val TAGGER_EN : MaxentTagger = MaxentTagger(MaxentTagger.DEFAULT_JAR_PATH)

        private val PATTERN_NOUN = TokenSequencePattern.compile(REGEX_NOUN)
        private val PATTERN_VERB = TokenSequencePattern.compile(REGEX_VERB)

        private val PIPELINE = StanfordCoreNLP(object : Properties() {
            init {
                setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse")
            }
        })

//        private const val UNDEFILED_TYPE = "UndefinedType"
        private val GENERAL_TYPES = listOf("object", "body")

        private fun formatKey(source : String) : String = source.toLowerCase()

        fun parsePathTokens(path: String, map : MutableMap<String, PathRToken>){
            var level = 1
            val tokens = getNlpTokens(convertPathText(path))

            path.split("/").filter { s -> !s.isBlank() }
                    .forEach { s ->
                        /*
                            TODO: technically, leading spaces are valid in a URI.
                            Furthermore, need to check if things like /x-{id} are possible,
                            and how Swagger handle endpoint encoding
                         */
                        val trimmed = s.trim()
                        if (trimmed.startsWith("{")) {
                            if (!trimmed.endsWith("}")) {
                                throw IllegalArgumentException("Opening { was not matched by closing } in: $path")
                            }
                            val token = tokens.find { it.originalText() == trimmed.substring(1, trimmed.lastIndex) }!!
                            map.putIfAbsent(formatKey(token.originalText()), PathRToken(token.originalText(), token.lemma(), -1, true, isVerbByTag(token.tag()!!)))

                        } else {
                            val token = tokens.find { it.originalText() == trimmed }
                            if(token == null){
                                tokens
                                        .filter { trimmed.contains(it.originalText()) }
                                        .forEach { t->
                                            map.putIfAbsent(formatKey(t.originalText()), PathRToken(t.originalText(), t.lemma(), level, false, isVerbByTag(t.tag()!!)))
                                        }

                            }else{
                                map.putIfAbsent(formatKey(token.originalText()), PathRToken(token.originalText(), token.lemma(), level, false, isVerbByTag(token.tag()!!)))

                            }
                            level++
                        }
                    }
        }

        fun parseAction(action: RestCallAction, description: String, map : MutableMap<String, ActionRToken>){
            if(description != null)
                parseActionTokensByDes(description, map)
            parseActionTokensByParam(action.parameters, map)
        }
        private fun parseActionTokensByDes(description: String, map : MutableMap<String, ActionRToken>){
            val tokens = getNounTokens(description)
            tokens.forEach {
                map.putIfAbsent(formatKey(it.originalText()), ActionRToken(it.originalText(), it.lemma(), false, false, false, isVerbByTag(it.tag())))
            }

        }

        private fun findRToken(key : String, map: MutableMap<String, out RToken>) : RToken?{
            map[key.toLowerCase()]?.let { return it }
            return map.values.find { it.isKey(key) }

        }


        private fun parseActionTokensByParam(params : MutableList<Param>, map : MutableMap<String, ActionRToken>){
            /*
             swagger specification: The location of the parameter. Possible values are "query", "header", "path", "formData" or "body".
              There are five possible parameter types.
              Path - Used together with Path Templating, where the parameter value is actually part of the operation's URL.
                    This does not include the host or base path of the API. For example, in /items/{itemId}, the path parameter is itemId.
              Query - Parameters that are appended to the URL. For example, in /items?id=###, the query parameter is id.
              Header - Custom headers that are expected as part of the request.
              Body - The payload that's appended to the HTTP request.
                   Since there can only be one payload, there can only be one body parameter.
                   The name of the body parameter has no effect on the parameter itself and is used for documentation purposes only.
                   Since Form parameters are also in the payload, body and form parameters cannot exist together for the same operation.
              Form - Used to describe the payload of an HTTP request when either application/x-www-form-urlencoded,
                   multipart/form-data or both are used as the content type of the request (in Swagger's definition, the consumes property of an operation).
                   This is the only parameter type that can be used to send files, thus supporting the file type.
                   Since form parameters are sent in the payload, they cannot be declared together with a body parameter for the same operation.
                   Form parameters have a different format based on the content-type used (for further details, consult http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4):
                   application/x-www-form-urlencoded - Similar to the format of Query parameters but as a payload.
                       For example, foo=1&bar=swagger - both foo and bar are form parameters. This is normally used for simple parameters that are being transferred.
                       multipart/form-data - each parameter takes a section in the payload with an internal header.
                       For example, for the header Content-Disposition: form-data; name="submit-name" the name of the parameter is submit-name.
                       This type of form parameters is more commonly used for file transfers.
             */
            params.forEach {p->
                if(p is BodyParam || p is PathParam || p is QueryParam || p is FormParam){
                    handleParam(p, map)
                }
            }

        }

        private fun handleParam(param: Param, map: MutableMap<String, ActionRToken>){
            val typeGene = getFirstTypeGene(param.gene)
            if (typeGene is ObjectGene){
                //name is always body for BodyParam (based on RestActionBuilder), then we ignore to record this info.
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
                map.putIfAbsent(formatKey(paramName), ActionRToken(paramName, getNlpTokens(paramName)[0].lemma(), true, false, isDirect))
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
                    map.putIfAbsent(formatKey(refType), ActionRToken(refType, getNlpTokens(refType)[0].lemma(), true, true, isDirect).also {
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

        private fun convertPathText(path: String) : String{
            return path.replace("{","").replace("}","").replace("/", " ").plus(".")
        }

        private fun getNlpTokens(text : String) : List<CoreLabel>{
            val sentences = PIPELINE.process(text).get(CoreAnnotations.SentencesAnnotation::class.java)
            if(sentences.size > 0)
                return sentences.flatMap { it.get(CoreAnnotations.TokensAnnotation::class.java) }
            else
                throw java.lang.IllegalArgumentException("Input text is not single sentence. The text is $text")
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


        private fun load(resourcePath: String) : Swagger{
            return SwaggerParser().read(resourcePath)
        }

        fun process(path : String){
            val swagger = load(path)
            swagger.paths.apply {
                forEach { t, u ->
                    u.operations.forEach { op->
                        if(!op.description.isNullOrBlank()){
                            println("--path: $t")
                            parsePathTokens(t, mutableMapOf())
                            println("----description: ${op.description}")
//                            parseActionTokens(op.description, mutableMapOf())
                        }

                    }
                }
            }

        }

        fun stringSimilarityScore(str1 : String, str2 : String, algorithm : SimilarityAlgorithm =SimilarityAlgorithm.Trigrams): Double{
            return when(algorithm){
                SimilarityAlgorithm.Trigrams -> trigrams(bigram(str1.toLowerCase()), bigram(str2.toLowerCase()))
                else-> 0.0
            }
        }

        private fun trigrams(bigram1: MutableList<CharArray>, bigram2 : MutableList<CharArray>) : Double{
            val copy = ArrayList<CharArray>(bigram2)
            var matches = 0
            var i = bigram1.size
            while (--i >= 0) {
                val bigram = bigram1.get(i)
                var j = copy.size
                while (--j >= 0) {
                    val toMatch = copy.get(j)
                    if (bigram[0] == toMatch[0] && bigram[1] == toMatch[1]) {
                        copy.removeAt(j)
                        matches += 2
                        break
                    }
                }
            }
            return matches.toDouble() / (bigram1.size + bigram2.size)
        }

        private fun bigram(input: String): MutableList<CharArray> {
            val bigram = mutableListOf<CharArray>()
            for (i in 0 until input.length - 1) {
                val chars = CharArray(2)
                chars[0] = input[i]
                chars[1] = input[i + 1]
                bigram.add(chars)
            }
            return bigram
        }

    }
}

enum class SimilarityAlgorithm{
    Trigrams
}
