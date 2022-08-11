package org.evomaster.core.problem.enterprise

import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.api.service.ApiWsIndividual
import org.evomaster.core.problem.external.service.ExternalServiceAction
import org.evomaster.core.search.*
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.tracer.TrackOperator
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Top-level representation for a test case involving an enterprise application.
 * An enterprise application could be a single web service (eg REST,GraphQL and RPC),
 * a whole microservice architecture (eg, accessed via an API Gateway), or a backend for
 * a frontend application (eg web pages on browser or mobile app).
 *
 * Regardless of the enterprise type, a common need is the ability to initialize data into databases
 * (eg SQL and NoSQL) before the "actions" on the SUT are executed (eg API calls and browser clicks).
 *
 * Setting up mocks of external services (eg using WireMock when the SUT communicate with another API) is done
 * per action, and not here in the initialization phase.
 */
abstract class EnterpriseIndividual(
    /**
     * a tracked operator to manipulate the individual (nullable)
     */
    trackOperator: TrackOperator? = null,
    /**
     * an index of individual indicating when the individual is initialized during the search
     * negative number means that such info is not collected
     */
    index: Int = -1,
    /**
     * a list of children of the individual
     */
    children: List<out ActionComponent>
) : Individual(
    trackOperator,
    index,
    children,
    GroupsOfChildren(
        children,
        listOf(
            //TODO in future ll need to refactor to handle multiple databases and NoSQL ones
            ChildGroup(GroupsOfChildren.INITIALIZATION_SQL,{e -> e is ActionComponent && e.flatten().all { a -> a is DbAction }}),
            // This assumes/requires that all initial children are of type MAIN
            ChildGroup(GroupsOfChildren.MAIN, {e -> e !is DbAction && e !is ExternalServiceAction}, if(children.isEmpty()) -1 else 0, children.size-1)
        )
    )
) {
    companion object{
        private val log : Logger = LoggerFactory.getLogger(ApiWsIndividual::class.java)
    }

    /**
     * a list of db actions for its Initialization
     */
    private val dbInitialization: List<DbAction>
        get() {
            return groupsView()!!.getAllInGroup(GroupsOfChildren.INITIALIZATION_SQL)
                .flatMap { (it as ActionComponent).flatten() }
                .map { it as DbAction }
        }


    final override fun seeActions(filter: ActionFilter) : List<Action>{
        return when (filter) {
            ActionFilter.ALL -> seeAllActions()
            ActionFilter.MAIN_EXECUTABLE -> groupsView()!!.getAllInGroup(GroupsOfChildren.MAIN)
                .flatMap { (it as ActionComponent).flatten() }
                .filter { it !is DbAction && it !is ExternalServiceAction }
            ActionFilter.INIT -> seeInitializingActions()
            // WARNING: this can still return DbAction and External ones...
            ActionFilter.NO_INIT -> groupsView()!!.getAllInGroup(GroupsOfChildren.MAIN).flatMap { (it as ActionComponent).flatten() }
            ActionFilter.ONLY_SQL -> seeInitializingActions().filterIsInstance<DbAction>()
            ActionFilter.NO_SQL -> seeAllActions().filter { it !is DbAction }
            ActionFilter.ONLY_EXTERNAL_SERVICE -> seeAllActions().filterIsInstance<ExternalServiceAction>()
            ActionFilter.NO_EXTERNAL_SERVICE -> seeAllActions().filter { it !is ExternalServiceAction }
        }
    }


    override fun repairInitializationActions(randomness: Randomness) {

        /*
            TODO this need updating / refactoring
         */

        /**
         * First repair SQL Genes (i.e. SQL Timestamps)
         */
        if (log.isTraceEnabled)
            log.trace("invoke GeneUtils.repairGenes")

        GeneUtils.repairGenes(this.seeGenes(GeneFilter.ONLY_SQL).flatMap { it.flatView() })

        /**
         * Now repair database constraints (primary keys, foreign keys, unique fields, etc.).
         *
         * Note: this is only for DB Actions in the initialization phase, and NOT if there is any
         * afterwards (eg in resource-based handling).
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

    private fun getLastIndexOfDbActionToAdd(): Int = children.indexOfLast { it is DbAction } + 1

    /**
     * add [actions] at [position]
     * if [position] = -1, append the [actions] at the end
     */
    @Deprecated("")
    fun addInitializingActions(position: Int=-1, actions: List<Action>){
        if (position == -1)  {
            addChildren(getLastIndexOfDbActionToAdd(), actions)
        } else{
            addChildren(position, actions)
        }
    }

    private fun resetInitializingActions(actions: List<DbAction>){
        killChildren { it is DbAction }
        addChildren(getLastIndexOfDbActionToAdd(), actions)
    }

    /**
     * remove specified dbactions i.e., [actions] from [dbInitialization]
     */
    fun removeInitDbActions(actions: List<DbAction>) {
        killChildren { it is DbAction && actions.contains(it)}
    }

    /**
     * @return a list table names which are used to insert data directly
     */
    open fun getInsertTableNames(): List<String>{
        return dbInitialization.filterNot { it.representExistingData }.map { it.table.name }
    }
}