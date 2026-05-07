package org.evomaster.core.problem.rest.oracle

import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.problem.rest.schema.SchemaUtils

/**
 * Series of oracles that can be evaluated directly on the HTTP responses based on the actual retrieved status code.
 * Some checks might depend on the API schema and/or the input request.
 * These checks are based on work done in:
 *
 * https://github.com/alixdecr/scoas
 * https://github.com/alixdecr/scoas/blob/main/evaluation/status-code-rules.md
 *
 *
 * Not all oracles can be translated to a dynamic version. Here, we just support the following:
 * no-non-standard-codes
 * no-201-if-delete
 * no-201-if-get
 * no-201-if-patch
 * no-204-if-content
 * no-413-if-no-payload
 * no-415-if-no-payload
 * no-401-if-no-auth (schema)
 * no-403-if-no-401 (schema)
 * has-406-if-accept (schema)
 *
 *
 * IMPORTANT: in contrast to what done in [HttpSemanticsOracle], here there is no need to construct any test case
 * on purpose with specific properties.
 * These checks are supposed to be lightweight, and so could be checked on each fitness evaluation.
 */
object HttpStatusOracle {


    fun checkOracles(call: RestCallAction, result: RestCallResult, schema: RestSchema) : List<FaultCategory>{

        val faults = mutableListOf<FaultCategory>()

        val status = result.getStatusCode()
            ?: return faults // all oracles depend on checking the status code

        if(status !in 100..599){
            faults.add(ExperimentalFaultCategory.HTTP_STATUS_NO_NON_STANDARD_CODES)
        }

        val verb = call.verb

        if(status == 201){
            when(verb){
                HttpVerb.GET -> faults.add(ExperimentalFaultCategory.HTTP_STATUS_NO_201_IF_GET)
                HttpVerb.DELETE -> faults.add(ExperimentalFaultCategory.HTTP_STATUS_NO_201_IF_DELETE)
                HttpVerb.PATCH -> faults.add(ExperimentalFaultCategory.HTTP_STATUS_NO_201_IF_PATCH)
                else -> {}
            }
        }

        if(status == 204 && verb == HttpVerb.GET){
            faults.add(ExperimentalFaultCategory.HTTP_STATUS_NO_204_IF_CONTENT)
        }

        val bodyParam = call.parameters.filterIsInstance<BodyParam>()
            .firstOrNull()

        val hasBody = bodyParam != null && bodyParam.primaryGene().getValueAsRawString().isNotEmpty()

        if(status == 413 && !hasBody){
            faults.add(ExperimentalFaultCategory.HTTP_STATUS_NO_413_IF_NO_PAYLOAD)
        }

        if(status == 415 && !hasBody){
            faults.add(ExperimentalFaultCategory.HTTP_STATUS_NO_415_IF_NO_PAYLOAD)
        }

        if(status == 406 && !call.isForRobustnessTesting()){
            faults.add(ExperimentalFaultCategory.HTTP_STATUS_HAS_406_IF_ACCEPT)
        }

        if(status == 401 && !SchemaUtils.hasAuthDefinition(schema)){
            faults.add(ExperimentalFaultCategory.HTTP_STATUS_NO_401_IF_NO_AUTH)
        }

        if(status == 403 && !SchemaUtils.getDeclaredStatusInResponse(call.endpoint, schema).contains(401)){
            faults.add(ExperimentalFaultCategory.HTTP_STATUS_NO_403_IF_NO_401)
        }

        return faults
    }
}