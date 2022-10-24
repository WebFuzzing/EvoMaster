package org.evomaster.core.problem.external.service.httpws

import org.evomaster.core.problem.external.service.param.ResponseParam

class ActualResponseInfo(val response: String) {
    private var schema : String? = null
    private var param : ResponseParam? = null
}