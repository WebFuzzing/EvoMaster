package org.evomaster.core.problem.rest

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType



object IdHeuristics {

    private val mapper = ObjectMapper()

    /**
     * Heuristically try to tell if the given string name is representing an id.
     * Note: we can never be 100% sure, so this is just a heuristic.
     */
    fun heuristicIsId(s: String) = s.endsWith("id", true)


    fun getId(body: String): IdLocationValue? {

        val node = try {
            mapper.readTree(body)
        } catch (e: JsonProcessingException) {
            return null
        } catch (e: JsonMappingException) {
            return null
        }
        if (node == null) {
            return null
        }

        return findIdInNode(node, "/")
    }

    private fun findIdInNode(node: JsonNode, path: String): IdLocationValue? {
        when (node.nodeType) {
            JsonNodeType.OBJECT, JsonNodeType.POJO -> {
                val id = node.fields().asSequence().firstOrNull { it.key.equals("id", true) }
                    ?: run {
                        val candidates = node.fields().asSequence().filter {
                            heuristicIsId(it.key) && it.value.isValueNode
                        }.toList()
                        if (candidates.isEmpty()) {
                            null
                        } else if (candidates.size == 1) {
                            candidates.first()
                        } else {
                            val underscore = candidates.firstOrNull { it.key.endsWith("_id", true) }
                            val capital = candidates.firstOrNull { it.key.endsWith("Id", false) }
                            if (underscore != null) {
                                underscore
                            } else if (capital != null) {
                                capital
                            } else {
                                null
                            }
                        }
                    }

                if (id == null) {
                    /*
                        special case of wrapped responses
                     */
                    val d = node.fields().asSequence().firstOrNull { it.key.equals("data") }
                    if (d != null) {
                        return findIdInNode(d.value, path + "data/")
                    }
                    return null
                }

                return IdLocationValue(path + id.key, id.value.asText())
            }

            JsonNodeType.ARRAY -> {
                //unsure we really need to handle arrays in this case
                return null
            }

            JsonNodeType.NUMBER, JsonNodeType.STRING -> {
                /*
                    Assume response is the id itself, but just if it is a single word
                 */
                return IdLocationValue(path, node.asText())
            }

            else -> {
                return null
            }
        }
    }
}