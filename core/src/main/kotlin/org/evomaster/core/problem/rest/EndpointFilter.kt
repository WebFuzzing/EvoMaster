package org.evomaster.core.problem.rest

import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig

object EndpointFilter {

     fun getEndPointsToSkip(config: EMConfig, swagger: OpenAPI):List<String> {
        if(config.endpointFocus.isNullOrBlank()
            && config.endpointPrefix.isNullOrBlank()){
            return listOf()
        }

        val all = swagger.paths.map{it.key}

        val selection =  if(config.endpointFocus != null) {
            all.filter { it != config.endpointFocus }
        } else if(config.endpointPrefix != null){
            all.filter { ! it.startsWith(config.endpointPrefix!!) }
        } else {
            //should never happens
            throw IllegalStateException("Invalid endpoint to skip configuration")
        }

        return selection
    }



     fun getEndpointsToSkip(config: EMConfig, swagger: OpenAPI, infoDto: SutInfoDto)
            : List<String>{

        /*
            If we are debugging, and focusing on a single endpoint, we skip
            everything but it.
            Otherwise, we just look at what configured in the SUT EM Driver.
         */

        val endpointsToSkip = getEndPointsToSkip(config, swagger)
        if(endpointsToSkip.isNotEmpty()){
            return endpointsToSkip
        }

        //this has less priority
        return  infoDto.restProblem?.endpointsToSkip ?: listOf()
    }
}