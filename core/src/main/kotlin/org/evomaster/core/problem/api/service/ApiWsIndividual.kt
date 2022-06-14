package org.evomaster.core.problem.api.service

import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.tracer.TrackOperator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * the abstract individual for API based SUT, such as REST, GraphQL, RPC
 */
abstract class ApiWsIndividual (

    /**
     * a tracked operator to manipulate the individual (nullable)
     */
    trackOperator: TrackOperator? = null,
    /**
     * an index of individual indicating when the individual is initialized during the search
     * negative number means that such info is not collected
     */
    index : Int = -1,
    /**
     * a list of children of the individual
     */
    children: List<out StructuralElement>
): Individual(trackOperator, index, children){

    companion object{
        private val log : Logger = LoggerFactory.getLogger(ApiWsIndividual::class.java)
    }

    /**
     * a list of db actions for its Initialization
     */
    private val dbInitialization: List<DbAction>
        get() {return children.filterIsInstance<DbAction>()}

    override fun seeInitializingActions(): List<DbAction> {
        return dbInitialization
    }

    override fun repairInitializationActions(randomness: Randomness) {

        /**
         * First repair SQL Genes (i.e. SQL Timestamps)
         */
        if (log.isTraceEnabled)
            log.trace("invoke GeneUtils.repairGenes")

        GeneUtils.repairGenes(this.seeGenes(GeneFilter.ONLY_SQL).flatMap { it.flatView() })

        /**
         * Now repair database constraints (primary keys, foreign keys, unique fields, etc.)
         */
        if (!verifyInitializationActions()) {
            if (log.isTraceEnabled)
                log.trace("invoke GeneUtils.repairBrokenDbActionsList")
            val previous = dbInitialization.toMutableList()
            DbActionUtils.repairBrokenDbActionsList(previous, randomness)
            resetInitializingActions(previous)
            Lazy.assert{verifyInitializationActions()}
        }
    }

    override fun hasAnyAction(): Boolean {
        return super.hasAnyAction() || dbInitialization.isNotEmpty()
    }

    /**
     * add [actions] at [position]
     * if [position] = -1, append the [actions] at the end
     */
    fun addInitializingActions(position: Int=-1, actions: List<DbAction>){
        if (position == -1)  addChildren(actions)
        else{
            addChildren(position, actions)
        }
    }

    private fun resetInitializingActions(actions: List<DbAction>){
        killChildren { it is DbAction }
        addChildren(actions)
    }

    /**
     * remove specified dbactions i.e., [actions] from [dbInitialization]
     */
    fun removeInitDbActions(actions: List<DbAction>) {
        killChildren { it is DbAction }
    }

    /**
     * @return a list table names which are used to insert data directly
     */
    open fun getInsertTableNames(): List<String>{
        return dbInitialization.filterNot { it.representExistingData }.map { it.table.name }
    }
}