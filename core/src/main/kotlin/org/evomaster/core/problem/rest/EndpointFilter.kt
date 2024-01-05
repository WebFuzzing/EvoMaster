package org.evomaster.core.problem.rest

import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig

object EndpointFilter {

     fun getEndPointsToSkip(config: EMConfig, swagger: OpenAPI):List<Endpoint> {
        if(config.endpointFocus.isNullOrBlank()
            && config.endpointPrefix.isNullOrBlank()){
            return listOf()
        }

        val all = Endpoint.fromOpenApi(swagger)

        val selection =  if(config.endpointFocus != null) {
            all.filter { it.path.toString() != config.endpointFocus }
        } else if(config.endpointPrefix != null){
            all.filter { ! it.path.toString().startsWith(config.endpointPrefix!!) }
        } else {
            //should never happen
            throw IllegalStateException("Invalid endpoint to skip configuration")
        }

        return selection
    }



     fun getEndpointsToSkip(config: EMConfig, swagger: OpenAPI, infoDto: SutInfoDto)
            : List<Endpoint>{

        /*
            Check if we are manually configuring some endpoints to skip.
            Otherwise, if none, we look at what configured in the SUT EM Driver.
         */

        val endpointsToSkip = getEndPointsToSkip(config, swagger)
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