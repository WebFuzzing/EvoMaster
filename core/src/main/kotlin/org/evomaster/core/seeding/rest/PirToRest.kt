package org.evomaster.core.seeding.rest

import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.seeding.PirToIndividual
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PirToRest(
    private val schema : List<RestCallAction>,
    private val randomness: Randomness
) : PirToIndividual(){

    companion object{
        private val log: Logger = LoggerFactory.getLogger(PirToRest::class.java)
    }

    fun fromVerbPath(verb: String, path: String) : RestCallAction?{

        val v = try{HttpVerb.valueOf(verb)}
        catch (e: IllegalArgumentException){
            log.warn("Unrecognized http verb: $verb")
            return null
        }

        val candidates = schema.filter { it.verb == v }
            .filter { it.path.matches(path) }

        if(candidates.isEmpty()){
            log.warn("No match for endpoint path: $path")
            return null
        }

        if(candidates.size > 1){
            log.warn("Ambiguity issue, as ${candidates.size} endpoints are a match for: $path")
        }

        val x = candidates[0].copy() as RestCallAction
        x.doInitialize(randomness)

        x.parameters.forEach { p ->
            when(p){
                is PathParam -> {
                    val toSeed = x.path.getKeyValues(path)?.get(p.name)!!
                    val isSet = p.gene.setFromStringValue(toSeed)
                    if(isSet){
                        log.warn("Failed to update path parameter ${p.name} with value: $toSeed")
                        return null
                    }
                }
                else -> {
                    //TODO other cases
                }
            }
        }

        return x
    }

}