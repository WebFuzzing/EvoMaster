package org.evomaster.core.problem.util

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.Endpoint
import org.evomaster.core.remote.SutProblemException


/**
 * Set of common utilities used when creating actions from schemas (e.g., for REST, GraphQL and RPC).
 */
object ActionBuilderUtil {

    fun verifySkipped(
        skipped: List<Endpoint>,
        endpointsToSkip: List<Endpoint>,
    ) {
        if (endpointsToSkip.toSet().size != endpointsToSkip.size) {

            val repeated = endpointsToSkip.filter { e ->
                endpointsToSkip.filter { it == e }.size > 1
            }

            throw SutProblemException("There are repeated, non-unique endpoint-to-skip declarations: " +
                repeated.joinToString(" , "))
        }

        if (skipped.size != endpointsToSkip.size) {
            val msg = "${endpointsToSkip.size} were set to be skipped, but only ${skipped.size}" +
                    " were found in the schema"
            LoggingUtil.getInfoLogger().error(msg)
            endpointsToSkip.filter { !skipped.contains(it) }
                .forEach { LoggingUtil.getInfoLogger().warn("Missing endpoint: $it") }
            throw SutProblemException(msg)
        }
    }

    fun printActionNumberInfo(type: String, n: Int, skipped: Int, errors: Int){

        LoggingUtil.getInfoLogger().apply {
            if (skipped > 0 ) {
                info("Skipped $skipped endpoints from the schema configuration")
            }

            when (n) {
                0 -> warn("There is _NO_ usable $type endpoint defined in the schema configuration")
                1 -> info("There is only 1 usable $type endpoint defined in the schema configuration")
                else -> info("There are $n usable $type endpoints defined in the schema configuration")
            }

            if (errors > 0){
                val k = if(errors == 1) "is 1 endpoint" else "are $errors endpoints"
                warn("There $k which have errors and will not be handled in the test generation")
            }
        }
    }
}