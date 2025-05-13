package org.evomaster.core.problem.util.inference

import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.resource.ParamInfo
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.resource.RestResourceNode
import org.evomaster.core.problem.util.inference.model.ParamGeneBindMap

/**
 * process inference regarding resource, which can be extended for the different inference technique
 */
interface DeriveResourceBinding {

    fun deriveResourceToTable(resourceCluster: MutableList<RestResourceNode>, allTables : Map<String, Table>)
    /**
     * derive relationship between a resource and a table
     */
    fun deriveResourceToTable(resourceNode : RestResourceNode, allTables : Map<String, Table>)

    fun generateRelatedTables(paramsInfo: List<ParamInfo>, calls: RestResourceCalls, sqlActions : List<SqlAction>) :  MutableMap<RestCallAction, MutableList<ParamGeneBindMap>>? = null

    fun generateRelatedTables(ar: RestResourceNode) :  MutableMap<String, MutableSet<String>>? = null
}