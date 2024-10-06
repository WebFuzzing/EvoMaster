package org.evomaster.core.problem.rest

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.Request
import com.atlassian.oai.validator.model.SimpleResponse
import com.atlassian.oai.validator.report.ValidationReport
import org.evomaster.core.search.FitnessValue

class RestSchemaOracles(
    schema: String
) {

    private val validator = OpenApiInteractionValidator.createFor(schema).build()

    fun handleSchemaOracles(path: String, verb: HttpVerb, rcr: RestCallResult, fv: FitnessValue) : ValidationReport {

        val res = SimpleResponse.Builder(rcr.getStatusCode()!!)
            .withBody(rcr.getBody())
            .withContentType(rcr.getBodyType()?.toString())
            //TODO we should collect headers and handle them here
            .build()

        val report = validator.validateResponse(path, Request.Method.valueOf(verb.name), res)

        //TODO update rcr and fv

        return report
    }

}