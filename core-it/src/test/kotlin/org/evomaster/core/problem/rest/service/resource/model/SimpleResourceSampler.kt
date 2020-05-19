package org.evomaster.core.problem.rest.service.resource.model

import io.swagger.parser.OpenAPIParser
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.problem.rest.service.ResourceSampler
import org.evomaster.core.search.Action

class SimpleResourceSampler : ResourceSampler() {

    fun initialize(swaggerPath : String, skipAction: List<String> = listOf(), sqlInsertBuilder: SqlInsertBuilder?) {

        val swagger = OpenAPIParser().readLocation(swaggerPath, null, null).openAPI
        val actionCluster = mutableMapOf<String, Action>()
        actionCluster.clear()

        RestActionBuilderV3.addActionsFromSwagger(swagger, actionCluster, skipAction, config.doesApplyNameMatching)
        super.initialize(mutableListOf(), actionCluster, sqlInsertBuilder)
    }
}