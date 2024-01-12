package org.evomaster.core.problem.rest

import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig

object EndpointFilter {

     fun getEndpointsToSkip(config: EMConfig, swagger: OpenAPI):List<Endpoint> {
        if(config.endpointFocus.isNullOrBlank()
            && config.endpointPrefix.isNullOrBlank()
            && config.endpointTagFilter.isNullOrBlank()){
            return listOf()
        }

        if(! config.endpointTagFilter.isNullOrBlank()){
            //this validation needs to be done here and not in EMConfig, as there we have no info on schema
            Endpoint.validateTags(config.getTagFilters(), swagger)
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



     fun getEndpointsToSkip(config: EMConfig, swagger: OpenAPI, infoDto: SutInfoDto)
            : List<Endpoint>{

        /*
            Check if we are manually configuring some endpoints to skip.
            Otherwise, if none, we look at what configured in the SUT EM Driver.
         */

        val endpointsToSkip = getEndpointsToSkip(config, swagger)
        if(endpointsToSkip.isNotEmpty()){
            return endpointsToSkip
        }

        val all = Endpoint.fromOpenApi(swagger)

        //this has less priority
        return  infoDto.restProblem?.endpointsToSkip
            ?.flatMap {s ->  all.filter { e -> e.path.toString() == s } }
            ?: listOf()
    }
}