package org.evomaster.core.problem.rest.service.resource.model

import io.swagger.parser.SwaggerParser
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.problem.rest.RestActionBuilder
import org.evomaster.core.problem.rest.service.ResourceSampler
import org.evomaster.core.search.Action

class SimpleResourceSampler : ResourceSampler() {

    fun initialize(swaggerPath : String, skipAction: List<String> = listOf(), sqlInsertBuilder: SqlInsertBuilder?) {

        val swagger = SwaggerParser().read(swaggerPath)
        val actionCluster = mutableMapOf<String, Action>()
        actionCluster.clear()

        RestActionBuilder.addActionsFromSwagger(swagger, actionCluster, skipAction, config.doesApplyNameMatching)
        super.initialize(mutableListOf(), actionCluster, sqlInsertBuilder)
    }
}