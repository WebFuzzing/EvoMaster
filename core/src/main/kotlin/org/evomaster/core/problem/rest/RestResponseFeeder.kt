package org.evomaster.core.problem.rest

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType
import opennlp.tools.stemmer.PorterStemmer
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.service.DataPool
import javax.ws.rs.core.MediaType

object RestResponseFeeder {

    private const val data = "data"

    private val mapper = ObjectMapper()

    private val stemmer = PorterStemmer()

    /**
     * Heuristically try to tell if the given string name is representing an id.
     * Note: we can never be 100% sure, so this is just a heuristic.
     */
    fun heuristicIsId(s: String) = s.endsWith("id", true)


    /**
     * Based on response of a REST action, feed the data pool.
     * This means extracting all key-value pairs, and add them to the pool.
     * However, this is done only for successful 2xx GET requests.
     * Special case is for POST, in which only ids are handled
     */
    fun handleResponse(source: RestCallAction, res: RestCallResult, pool: DataPool){

        val status = res.getStatusCode()
        if(status !in 200..299){
            //collect data only from successful requests
            return
        }

        if(source.verb != HttpVerb.GET && source.verb != HttpVerb.POST){
            /*
                Only interested in GET... and possibly POST if it returns id of
                generated resource... although this should be handled as well
                by smart sampling
             */
            return
        }

        val body = res.getBody()
            ?: return
        if(body.isBlank()){
            return
        }

        if(res.getBodyType()?.isCompatible(MediaType.APPLICATION_JSON_TYPE) == true){
            val qualifier = source.path.nameQualifier
            val node = try{
                mapper.readTree(body)
            } catch (e: JsonProcessingException){
                return
            } catch (e: JsonMappingException){
                return
            }
            if(node == null){
                return
            }

            if(source.verb == HttpVerb.GET) {
                analyzeJsonNode(node, null, qualifier, pool)
            } else {
                assert(source.verb == HttpVerb.POST)
                /*
                    special case POST, just want id, as might have created new resource.
                    But likely this is not so critical.
                    This scenario is already (or should be) handled by smart sampling.
                    Also, newly created resources could be still fetched with a GET.
                    But not too complicated to handle, so we do it.
                 */
                handleOnlyId(node, qualifier, pool)
            }
        }
    }


    private fun handleOnlyId(node: JsonNode, qualifier: String, pool: DataPool) {
        when(node.nodeType){
            JsonNodeType.OBJECT, JsonNodeType.POJO -> {
                val id = node.fields().asSequence().firstOrNull { it.key.equals("id", true) }
                    ?: node.fields().asSequence().firstOrNull{
                        it.key.endsWith("id", true) && it.value.isValueNode
                    }

                if(id == null) {
                    /*
                        special case of wrapped responses
                     */
                    val d = node.fields().asSequence().firstOrNull{it.key.equals("data")}
                    if(d != null){
                        handleOnlyId(d.value, qualifier, pool)
                    }
                    return
                }

                val name = if(id.key.equals("id",true)){
                    stemmer.reset()
                    stemmer.stem(qualifier) + "id"
                } else {
                    id.key
                }
                pool.addValue(name, id.value.asText())
            }
            JsonNodeType.ARRAY ->  {
                //unsure we really need to handle arrays in this case
            }
            JsonNodeType.NUMBER, JsonNodeType.STRING -> {
                /*
                    Assume response is the id itself, but just if it is a single word
                 */
                val id = node.asText()
                if(id.contains(' ')){
                    return
                }
                stemmer.reset()
                val key = stemmer.stem(qualifier) + "id"
                pool.addValue(key, id)
            }
            else -> {/* do nothing */}
        }
    }

    private fun isCompositeType(node: JsonNode) =
        node.nodeType == JsonNodeType.OBJECT
                || node.nodeType == JsonNodeType.POJO
                || node.nodeType == JsonNodeType.ARRAY

    private fun analyzeJsonNode(node: JsonNode, field: String?, qualifier: String, pool: DataPool){
        when(node.nodeType){
            JsonNodeType.OBJECT, JsonNodeType.POJO -> node.fields().forEach {
                val q = if(isCompositeType(it.value)
                        /*
                            "data" is a generic term... which is also often used for
                            wrapped responses (eg, that's the case for GraphQL).
                            As such, we skip it as a qualifier
                         */
                        && it.key != data){
                    it.key
                } else {
                    qualifier
                }
                analyzeJsonNode(it.value, it.key, q, pool)
            }
            JsonNodeType.ARRAY -> node.elements().forEach {
                stemmer.reset()
                val q = stemmer.stem(qualifier)
                analyzeJsonNode(it, null, q, pool)
            }
            JsonNodeType.NUMBER, JsonNodeType.STRING -> {
                val key = if(field == null){
                    qualifier
                } else if(field.equals("id",true)){
                    stemmer.reset()
                    stemmer.stem(qualifier) + "id"
                } else {
                    field
                }
                pool.addValue(key, node.asText())
            }
            else -> {/* do nothing */}
        }
    }
}