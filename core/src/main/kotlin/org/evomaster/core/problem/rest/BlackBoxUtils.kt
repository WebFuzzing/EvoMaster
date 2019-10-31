package org.evomaster.core.problem.rest

import org.evomaster.core.EMConfig
import java.net.URL


object BlackBoxUtils {

    fun restUrl(config: EMConfig) : String {

        return if(!config.bbTargetUrl.isNullOrBlank()){
            config.bbTargetUrl
        } else {
            //try to infer it from Swagger URL
            val url = URL(config.bbSwaggerUrl)
            val port = if(url.port > 0) ":${url.port}" else ""
            url.protocol + "://" + url.host + port
        }
    }

}