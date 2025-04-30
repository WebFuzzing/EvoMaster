package org.evomaster.core.problem.rest.link

import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import javax.ws.rs.core.MediaType

object RestLinkValueUpdater {

    /**
     * Update the genes of the [target] action based on the [link] pointing to the given [source].
     */
    fun update(
        target: RestCallAction,
        link: RestLink,
        source: RestCallAction,
        sourceResults: RestCallResult
    ) : Boolean {

        val blr = target.backwardLinkReference
            ?: throw IllegalArgumentException("target action does not have a backward link reference")
        if(blr.sourceLinkId != link.id){
            throw IllegalArgumentException("Not matching ids for link: ${blr.sourceLinkId} != ${link.id}")
        }
        if(link.statusCode != sourceResults.getStatusCode()){
            throw IllegalArgumentException("Not matching status code: ${link.statusCode} != ${sourceResults.getStatusCode()}")
        }
        if(source.getLocalId() != sourceResults.sourceLocalId){
            throw IllegalArgumentException("Source action ${source.getLocalId()} not matching result for ${sourceResults.sourceLocalId}")
        }
        if(source.links.none { it.id == link.id }){
            throw IllegalArgumentException("Input link not part of the source action")
        }

        var anyModification = false

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
                //TODO possibly a bug? eg a string value does not match a pattern constraint
            }
            anyModification = anyModification || ok
        }

        return anyModification
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