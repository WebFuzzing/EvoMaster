package org.evomaster.core.problem.rest

import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaOpenAPI

object EndpointFilter {

    @Deprecated("Rather use version with RestSchema instead")
    fun getEndpointsToSkip(config: EMConfig, swagger: OpenAPI):List<Endpoint>{
        return getEndpointsToSkip(config, RestSchema(SchemaOpenAPI("", swagger, SchemaLocation.MEMORY)))
    }

     fun getEndpointsToSkip(config: EMConfig, schema: RestSchema):List<Endpoint> {

         //FIXME/TODO: should handle $ref in this code, and not just rely on main
         val swagger = schema.main.schemaParsed

         if(config.endpointFocus.isNullOrBlank()
            && config.endpointPrefix.isNullOrBlank()
            && config.endpointTagFilter.isNullOrBlank()){
            return listOf()
        }

        if(! config.endpointTagFilter.isNullOrBlank()){
            //this validation needs to be done here and not in EMConfig, as there we have no info on schema
            Endpoint.validateTags(config.getTagFilters(), swagger)
        }

        if(! config.endpointFocus.isNullOrBlank()){
            Endpoint.validateFocus(config.endpointFocus!!, swagger)
        }

        if(! config.endpointPrefix.isNullOrBlank()){
            Endpoint.validatePrefix(config.endpointPrefix!!, swagger)
        }

        val all = Endpoint.fromOpenApi(swagger)

        val x =  if(config.endpointFocus != null) {
            all.filter { it.path.toString() != config.endpointFocus }
        } else if(config.endpointPrefix != null){
            all.filter { ! it.path.toString().startsWith(config.endpointPrefix!!) }
        } else {
           listOf()
        }

        val tags = config.getTagFilters()
        val y = if(tags.isNotEmpty()){
            all.filter { e -> e.getTags(swagger).none { t -> tags.contains(t) }}
        } else {
            listOf()
        }

        return mutableSetOf<Endpoint>().apply {
            addAll(x)
            addAll(y)
        }.toList()
    }



     fun getEndpointsToSkip(config: EMConfig, schema: RestSchema, infoDto: SutInfoDto)
            : List<Endpoint>{

        /*
            Check if we are manually configuring some as well as what configured in the SUT EM Driver.
         */

        val fromConfig = getEndpointsToSkip(config, schema)

        val all = Endpoint.fromOpenApi(schema.main.schemaParsed) //TODO use schema, ie, handle $ref

        val fromDriver =  infoDto.restProblem?.endpointsToSkip
            ?.flatMap {s ->  all.filter { e -> e.path.toString() == s } }
            ?: listOf()

         return mutableSetOf<Endpoint>().apply {
             addAll(fromConfig)
             addAll(fromDriver)
         }.toList()
    }
}