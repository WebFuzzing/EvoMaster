package org.evomaster.core.problem.rest

import com.fasterxml.jackson.databind.ObjectMapper
import javax.ws.rs.core.MediaType

object RestLinkValueUpdater {

    /**
     * Update the genes of the [target] action based on the [link] pointing to the given [source].
     */
    fun update(target: RestCallAction, link: RestLink, source: RestCallAction, sourceResults: RestCallResult){

        //TODO validation of inputs


        for (p in link.parameters) {
            val value = extractValue(p, sourceResults)
                ?: continue

            val candidates = target.parameters.filter {
                it.name == p.name
                        &&
                        (p.scope == null || p.scope.matchType(it))
            }
            if(candidates.size != 1){
                //TODO bug is schema?
                continue
            }
            val chosen = candidates[0]
            val gene = chosen.primaryGene()
            val ok = gene.setFromStringValue(value)
            if(!ok){
                //TODO possibly a bug?
            }
        }
    }


    private fun extractValue(p: RestLinkParameter, sourceResults: RestCallResult) : String?{
        return when {
            p.isConstant() -> {
                p.value
            }
            p.isBodyField() ->{
                val body = sourceResults.getBody()
                    ?: return null
                val bodyType = sourceResults.getBodyType()
                    ?: return null
                if(!bodyType.isCompatible(MediaType.APPLICATION_JSON_TYPE)){
                    return null
                }
                val pointer = p.bodyPointer()
                val jackson = ObjectMapper()
                val tree = jackson.readTree(body)
                val token = tree.at(pointer).asText()
                if(token.isNullOrEmpty()){
                    /*
                        TODO this could be considered a bug?
                     */
                    return null
                }
                token
            }
            //TODO other cases when implemented
            else -> return null
        }
    }
}