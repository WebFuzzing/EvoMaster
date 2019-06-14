package org.evomaster.core.problem.rest.util.inference

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.schema.Table
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.resource.RestResourceNode
import org.evomaster.core.problem.rest.util.inference.model.ParamGeneBindMap

/**
 * process inference regarding resource, which can be extended for the different inference technique
 */
interface DeriveResourceBinding {

    fun deriveResourceToTable(resourceCluster: MutableList<RestResourceNode>, allTables : Map<String, Table>)
    /**
     * derive relationship between a resource and a table
     */
    fun deriveResourceToTable(resourceNode : RestResourceNode, allTables : Map<String, Table>)

    fun generateRelatedTables(calls: RestResourceCalls, dbActions : MutableList<DbAction>) :  MutableMap<RestAction, MutableList<ParamGeneBindMap>>? = null

    fun generateRelatedTables(ar: RestResourceNode) :  MutableMap<String, MutableSet<String>>? = null
}