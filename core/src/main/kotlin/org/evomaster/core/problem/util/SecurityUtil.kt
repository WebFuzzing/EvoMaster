package org.evomaster.core.problem.util

import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.problem.rest.SchemaDescription

/**
 * Utility class contains methods related to security
 */
object SecurityUtil {

    fun extractDescriptionFromSchema(swagger: OpenAPI) : SchemaDescription {
        val descriptions = SchemaDescription()

        // Header values and descriptions
        swagger.paths.forEach { (_, paths) ->
            if (paths.post != null && paths.post.parameters != null) {
                paths.post.parameters.forEach { x ->
                    if (!x.name.isNullOrEmpty() && !x.description.isNullOrEmpty()) {
                        descriptions.addHeader(x.name, x.description)
                    }
                }
            }
        }

        // Body parameters descriptions
        if (swagger.components != null && swagger.components.schemas != null) {
            swagger.components.schemas.forEach { (_, x) ->
                if (x.properties != null) {
                    x.properties.forEach { (n, d) ->
                        if (!n.isNullOrEmpty() && !d.description.isNullOrEmpty()) {
                            descriptions.addBody(n, d.description)
                        }
                    }
                }
            }
        }

        return descriptions
    }
}
