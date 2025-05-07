package org.evomaster.core.problem.rest.resource

import org.evomaster.client.java.controller.api.dto.database.operations.DataRowDto
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionUtils
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.util.inference.SimpleDeriveResourceBinding
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.service.Randomness

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
     * @param actionCluster specified all actions in the sut
     * @param sqlInsertBuilder specified the database info
     * @param config indicates the EM configuration which will be used to init [actionCluster] and [sqlInsertBuilder]
     *          e.g., whether to init derived table for each of resource
     */
    fun initResourceCluster(actionCluster : Map<String, Action>, sqlInsertBuilder: SqlInsertBuilder? = null, config: EMConfig) {
        if (resourceCluster.isNotEmpty()) return

        if (sqlInsertBuilder != null) syncDataInDb(sqlInsertBuilder)

        if(config.extractSqlExecutionInfo) {
            // reset tables based on table info in [sqlInsertBuilder]
            sqlInsertBuilder?.extractExistingTables(tables)
        }

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

    fun reset(){
        resourceCluster.clear()
        dataInDB.clear()
        tables.clear()
    }


    /**
     * derive related table for each resource node
     */
    fun initRelatedTables(){
        resourceCluster.values.forEach { r->
            SimpleDeriveResourceBinding.deriveResourceToTable(r, getTableInfo())
        }
    }

    /**
     * synchronize the existing data in database based on [sqlInsertBuilder]
     */
    fun syncDataInDb(sqlInsertBuilder: SqlInsertBuilder?){
        sqlInsertBuilder?.extractExistingPKs(dataInDB)
    }

    /**
     * @return all resource nodes
     */
    fun getCluster() = resourceCluster.toMap()

    /**
     * @return all table info
     */
    fun getTableInfo() = tables.toMap()

    /**
     * @return resource node based on the specified [action]
     * @param nullCheck specified whether to throw an exception when the requested resource does not exist
     */
    fun getResourceNode(action: RestCallAction, nullCheck: Boolean = false) : RestResourceNode? = getResourceNode(action.path.toString(), nullCheck)

    /**
     * @return resource node based on the specified [key]
     * @param key specified the resource node to get
     * @param nullCheck specified whether to throw an exception when the requested resource does not exist
     */
    fun getResourceNode(key: String, nullCheck: Boolean = false) : RestResourceNode?{
        if (!nullCheck) return resourceCluster[key]
        return resourceCluster[key]?:throw IllegalStateException("cannot find the resource node with $key")
    }

    /**
     * @return existing rows of the table based on the specified [tableName]
     */
    fun getDataInDb(tableName: String) : MutableList<DataRowDto>?{
        val found = dataInDB.filterKeys { k-> k.equals(tableName, ignoreCase = true) }.keys
        if (found.isEmpty()) return null
        Lazy.assert{found.size == 1}
        return dataInDB.getValue(found.first())
    }

    /**
     * @return table class based on specified [name]
     */
    fun getTableByName(name : String) = tables.keys.find { it.equals(name, ignoreCase = true) }?.run { tables[this] }


    /**
     * create db actions based on specified tables, i.e., [tables]
     * @param tables specifies tables
     * @param sqlInsertBuilder is used to create dbactions
     * @param previous specifies the previous dbactions
     * @param doNotCreateDuplicatedAction specifies whether it is allowed to create duplicate db actions
     * @param isInsertion specifies that db actions are SELECT or INSERT
     *          note that if there does not exist data for the table, we will employ INSERT instead of SELECT
     * @param randomness is employed to randomize the created actions or select an existing data
     * @param forceSynDataInDb specified whether to force synchronizing data. By default, we disable it since it is quite time-consuming.
     *
     * for instance, set could be A and B, and B refers to A
     * if [doNotCreateDuplicatedAction], the returned list would be A and B
     * else the return list would be A, A and B. the additional A is created for B
     */
    fun createSqlAction(tables: List<String>,
                        sqlInsertBuilder: SqlInsertBuilder,
                        previous: List<SqlAction>,
                        doNotCreateDuplicatedAction: Boolean,
                        isInsertion: Boolean = true,
                        randomness: Randomness,
                        forceSynDataInDb: Boolean = false,
                        useExtraSqlDbConstraints: Boolean = false,
                        enableSingleInsertionForTable : Boolean = false
    ) : MutableList<SqlAction>{
        val sorted = SqlActionUtils.sortTable(tables.mapNotNull { getTableByName(it) }.run { if (doNotCreateDuplicatedAction) this.distinct() else this })
        val added = mutableListOf<SqlAction>()
        val preTables = previous.filter { !isInsertion || !it.representExistingData }.map { it.table.name }.toMutableList()


        if (!isInsertion && sorted.none { t-> (getDataInDb(t.name)?.size?: 0) > 0 }){
            if (forceSynDataInDb)
                syncDataInDb(sqlInsertBuilder)
            if (sorted.none { t-> (getDataInDb(t.name)?.size?: 0) > 0 }){
                return mutableListOf()
            }
        }

        sorted.forEach { t->
            if (!doNotCreateDuplicatedAction || preTables.none { p-> p.equals(t.name, ignoreCase = true) }){
                val actions = if (!isInsertion){
                    if ((getDataInDb(t.name)?.size ?: 0) == 0) {
                        null
                    } else {
                        val candidates = getDataInDb(t.name)!!
                        val row = randomness.choose(candidates)
                        val a = sqlInsertBuilder.extractExistingByCols(t.name, row, useExtraSqlDbConstraints)
                        if(a != null)
                            listOf(a)
                        else
                            null
                    }
                } else{
                    sqlInsertBuilder.createSqlInsertionAction(t.name, useExtraSqlDbConstraints = useExtraSqlDbConstraints, enableSingleInsertionForTable=enableSingleInsertionForTable)
                            .onEach { a -> a.doInitialize(randomness) }
                }

                if (!actions.isNullOrEmpty()){
                    //actions.forEach {it.doInitialize()}
                    added.addAll(actions)
                    preTables.addAll(actions.filter { !isInsertion || !it.representExistingData }.map { it.table.name })
                }
            }
        }

        return added
    }

    fun doesCoverAll(individual: RestIndividual): Boolean{
        return individual.getResourceCalls().map {
            it.getResolvedKey()
        }.toSet().size >= resourceCluster.size
    }

}