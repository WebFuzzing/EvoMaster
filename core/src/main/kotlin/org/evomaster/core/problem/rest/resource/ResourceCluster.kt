package org.evomaster.core.problem.rest.resource

import org.evomaster.client.java.controller.api.dto.database.operations.DataRowDto
import org.evomaster.core.EMConfig
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.database.schema.Table
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.Action

/**
 * this class is to record the identified resources in the sut, i.e., based on the 'paths' of 'OpenAPI'
 */
class ResourceCluster {

    /**
     * key is resource path
     * value is an abstract resource
     */
    private val resourceCluster : MutableMap<String, RestResourceNode> = mutableMapOf()


    /**
     * key is table name
     * value is a list of existing data of PKs in DB
     */
    private val dataInDB : MutableMap<String, MutableList<DataRowDto>> = mutableMapOf()

    /**
     * key is table name
     * value is the table
     */
    private val tables : MutableMap<String, Table> = mutableMapOf()


    /**
     * init resource nodes based on [actionCluster] and [sqlInsertBuilder]
     */
    fun initResourceCluster(actionCluster : Map<String, Action>, sqlInsertBuilder: SqlInsertBuilder? = null, config: EMConfig) {
        if (resourceCluster.isNotEmpty()) return

        if(config.extractSqlExecutionInfo) sqlInsertBuilder?.extractExistingTables(tables)

        actionCluster.values.forEach { u ->
            if (u is RestCallAction) {
                val resource = resourceCluster.getOrPut(u.path.toString()) {
                    RestResourceNode(
                        u.path.copy(),
                        initMode =
                        if(config.probOfEnablingResourceDependencyHeuristics > 0.0 && config.doesApplyNameMatching) InitMode.WITH_DERIVED_DEPENDENCY
                        else if(config.doesApplyNameMatching) InitMode.WITH_TOKEN
                        else if (config.probOfEnablingResourceDependencyHeuristics > 0.0) InitMode.WITH_DEPENDENCY
                        else InitMode.NONE, employNLP = config.enableNLPParser)
                }
                resource.actions.add(u)
            }
        }
        resourceCluster.values.forEach{it.initAncestors(resourceCluster.values.toList())}

        resourceCluster.values.forEach{it.init()}
    }

    fun syncDataInDb(sqlInsertBuilder: SqlInsertBuilder?){
        sqlInsertBuilder?.extractExistingPKs(dataInDB)
    }

    fun getCluster() = resourceCluster.toMap()

    fun getTableInfo() = tables.toMap()

    fun getResourceNode(action: RestCallAction, nullCheck: Boolean = false) : RestResourceNode? = getResourceNode(action.path.toString(), nullCheck)

    fun getResourceNode(key: String, nullCheck: Boolean = false) : RestResourceNode?{
        if (!nullCheck) return resourceCluster[key]
        return resourceCluster[key]?:throw IllegalStateException("cannot find the resource node with $key")
    }

    fun getDataInDb(tableName: String) : MutableList<DataRowDto>?{
        val found = dataInDB.filterKeys { k-> k.equals(tableName, ignoreCase = true) }.keys
        if (found.isEmpty()) return null
        assert(found.size == 1)
        return dataInDB.getValue(found.first())
    }

    fun getTableByName(name : String) = tables.keys.find { it.equals(name, ignoreCase = true) }?.run { tables[this] }

}