package org.evomaster.core.problem.rest

import org.evomaster.core.EMConfig
import java.net.URL


object BlackBoxUtils {

    fun restUrl(config: EMConfig) : String {

        /*
            Note: bbTargetUrl and bbSwaggerUrl are already validated
            in EMConfig (but they can be blank)
         */

        return if(!config.bbTargetUrl.isBlank()){
            config.bbTargetUrl
        } else {
            //try to infer it from Swagger URL
            val url = URL(config.bbSwaggerUrl)

            if(url.protocol != "http" && url.protocol != "https"){
                throw IllegalStateException("If the API is not running on same host:port from where the schema " +
                        "is fetched, then you MUST use --bbTargetUrl option to specify it")
            }

            //TODO checks if blank, give proper warning
            val port = if(url.port > 0) ":${url.port}" else ""
            url.protocol + "://" + url.host + port
        }
    }

}