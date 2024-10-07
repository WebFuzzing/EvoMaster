package org.evomaster.core.problem.rest

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.Request
import com.atlassian.oai.validator.model.SimpleResponse
import com.atlassian.oai.validator.report.ValidationReport


class RestSchemaOracle(
    schema: String
) {

    private val validator = OpenApiInteractionValidator.createFor(schema).build()

    fun handleSchemaOracles(path: String, verb: HttpVerb, rcr: RestCallResult) : ValidationReport {

        val res = SimpleResponse.Builder(rcr.getStatusCode()!!)
            .withBody(rcr.getBody())
            .withContentType(rcr.getBodyType()?.toString())
            //TODO we should collect headers and handle them here
            .build()

        val report = validator.validateResponse(path, Request.Method.valueOf(verb.name), res)

        /*
            FIXME
            library is buggy:
            https://bitbucket.org/atlassian/swagger-request-validator/issues/369/make-the
            additionalProperties are allowed by default, unless explicitly disabled.
            this would create false positive.
            We have 3 options:
            1) wait for fix in library
            2) make a fork of the library and fix by ourselves
            3) filter out wrong error messages here (quite complicated, as some would be valid...)
            For now, we go for (1)
         */

        return report
    }

}