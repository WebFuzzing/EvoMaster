package org.evomaster.core.seeding.service.rest

import com.google.inject.Inject
import org.evomaster.core.problem.enterprise.auth.AuthSettings
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.seeding.service.PirToIndividual
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct

class PirToRest: PirToIndividual(){

    companion object{
        private val log: Logger = LoggerFactory.getLogger(PirToRest::class.java)
    }

    @Inject
    private lateinit var sampler: AbstractRestSampler


    //TODO move up, once doing refactoring
    private lateinit var authSettings: AuthSettings


    /**
     * All actions that can be defined from the OpenAPI schema
     */
    private lateinit var actionDefinitions : List<RestCallAction>

    @PostConstruct
    fun initialize() {
        actionDefinitions = sampler.getActionDefinitions() as List<RestCallAction>
        authSettings = sampler.authentications
    }


    /**
     *  From components of a string representation, create an action, based on existing template
     */
    fun fromVerbPath(verb: String, path: String, queryParams : Map<String,String> = mapOf()) : RestCallAction?{

        val v = try{
            HttpVerb.valueOf(verb.uppercase())}
        catch (e: IllegalArgumentException){
            log.warn("Unrecognized http verb: $verb")
            return null
        }

        val candidates = actionDefinitions.filter { it.verb == v }
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

        /*
         * looking at all existing parameters in the candidate template.
         * note that input info might be missing data, if such is optional.
         * for example, if a query parameter is optional, that might be missing in the input to this function,
         * which would mean it must be off here
         */
        x.parameters.forEach { p ->
            when(p){
                is PathParam -> {
                    val toSeed = x.path.getKeyValues(path)?.get(p.name)!!
                    val isSet = p.gene.setFromStringValue(toSeed)
                    if(!isSet){
                        log.warn("Failed to update path parameter ${p.name} with value: $toSeed")
                        return null
                    }
                }
                is QueryParam ->{
                    /*
                     * when dealing with queries, several different scenarios could happen:
                     * 1: it is there as input (-> change value)
                     * 2: it is not there, possibly because optional (-> deactivate)
                     * 3: it is not there in the template, because the param does not exist any more,
                     *    eg seed is not in sync (-> ignore)
                     * 4: existing required param is not in the input (-> ignore)
                     */
                    val name = p.name
                    val gene = p.getGeneForQuery()
                    if(queryParams.containsKey(name)){
                        gene.setFromStringValue(queryParams[name]!!)
                    } else {
                        //TODO also check nullable genes
                        val optional = gene.getWrappedGene(OptionalGene::class.java)
                        if(optional != null){
                            //simply deactivate it
                            optional.isActive = false
                        } else {
                            // this means it is required, but not present in the seed
                            // as such, just leave it as randomized
                            log.warn("Required query parameter $name is not in the input seed." +
                                    " This could happen if schema has changed since the seed was created.")
                            //nothing to do further
                        }
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