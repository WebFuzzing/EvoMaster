package org.evomaster.core.problem.rest


import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.service.Sampler
import java.net.URL


object BlackBoxUtils {

    /**
     * Return the URL of the SUT.
     * This might depend on the several factors, like REST vs GraphQL, and whether
     * the info is overridden compared to what provided in the schema (eg for REST).
     */
    fun targetUrl(config: EMConfig, sampler: Sampler<*>? = null): String {

        /*
            Note: bbTargetUrl and bbSwaggerUrl are already validated
            in EMConfig (but they can be blank)
         */

        if (config.bbTargetUrl.isNotBlank()) {
            //this has the priority
            return config.bbTargetUrl
        } else {

            when (config.problemType) {
                //should had been already validated in EMConfig
                EMConfig.ProblemType.GRAPHQL -> throw IllegalStateException("BUG: no target for GQL is defined")
                EMConfig.ProblemType.REST -> {
                    //https://swagger.io/docs/specification/api-host-and-base-path/
                    val schema = (sampler as AbstractRestSampler).schemaHolder.main.schemaParsed
                    return if (schema.servers == null || schema.servers.isEmpty()) {
                        /*
                            Schema has no info on where the API is, eg 'host' in v2 and 'servers' in v3.
                            "/" is default if value missing in schema.
                            So, going to use same location as where the schema was downloaded from,
                            as specified in the specs to do in these cases
                         */
                        extractTarget(config.bbSwaggerUrl)
                    } else {
                        //OpenAPI specs call it a "url", but it is actually a URI
                        val uri = schema.servers[0].url
                        if(uri.startsWith("//")){
                            // this can happen if 'scheme' is missing in V2, resulting in an invalid URL in current parser.
                            // this is also a valid value in V3
                            extractTarget("http:$uri")
                        } else if(uri.startsWith("/")){
                            //this is a relative URI, so get info from schema location
                            extractTarget(config.bbSwaggerUrl)
                        } else {
                            extractTarget(uri)
                        }
                    }
                }
                else -> throw IllegalStateException("Black-box testing is currently not supported for ${config.problemType}")
            }
        }
    }

    private fun extractTarget(fullUrl: String): String {
        val url = try {
            URL(fullUrl)
        }catch (e: Exception){
            throw SutProblemException("Invalid URL: $fullUrl")
        }

        if (url.protocol == "file") {
            throw IllegalStateException("If the schema is read from local file system, and" +
                    " there is no info on host:port, then you MUST use --bbTargetUrl option to specify it")
        }

        val protocol = if (url.protocol == null || url.protocol.isEmpty()) {
            "http" //defaulting to it
        } else if (url.protocol == "http" || url.protocol == "https") {
            url.protocol
        } else {
            throw IllegalStateException("Not supported protocol: ${url.protocol}")
        }
        val port = if (url.port > 0) ":${url.port}" else ""
        return protocol + "://" + url.host + port
    }

}