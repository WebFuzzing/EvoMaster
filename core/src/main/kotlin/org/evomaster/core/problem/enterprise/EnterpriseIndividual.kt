package org.evomaster.core.problem.enterprise

import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.api.ApiWsIndividual
import org.evomaster.core.problem.externalservice.ApiExternalServiceAction
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.search.*
import org.evomaster.core.search.gene.utils.GeneUtils
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
    children: MutableList<out ActionComponent>,
    childTypeVerifier: (Class<*>) -> Boolean,
    groups : GroupsOfChildren<StructuralElement> = getEnterpriseTopGroups(children,children.size,0)
) : Individual(
    trackOperator,
    index,
    children,
    childTypeVerifier,
    groups
) {
    companion object{
        private val log : Logger = LoggerFactory.getLogger(ApiWsIndividual::class.java)

        /**
         * Return group definition for the given children.
         * The first [sizeDb] are assumed to be database actions, followed by [sizeMain] main actions
         */
        fun getEnterpriseTopGroups(
            children: List<ActionComponent>,
            sizeMain: Int,
            sizeDb: Int
        ) : GroupsOfChildren<StructuralElement>{

            if(children.size != sizeDb + sizeMain){
                throw IllegalArgumentException("Group size mismatch. Expected a total of ${children.size}, but" +
                        " got main=$sizeMain and db=$sizeDb")
            }
            if(sizeDb < 0){
                throw IllegalArgumentException("Negative size for sizeDb: $sizeDb")
            }
            if(sizeMain < 0){
                throw IllegalArgumentException("Negative size for sizeMain: $sizeMain")
            }

            //TODO in future ll need to refactor to handle multiple databases and NoSQL ones
            val db = ChildGroup<StructuralElement>(GroupsOfChildren.INITIALIZATION_SQL,{e -> e is ActionComponent && e.flatten().all { a -> a is DbAction }},
                if(sizeDb==0) -1 else 0 , if(sizeDb==0) -1 else sizeDb-1
            )

            val main = ChildGroup<StructuralElement>(GroupsOfChildren.MAIN, {e -> e !is DbAction && e !is ApiExternalServiceAction },
                if(sizeMain == 0) -1 else sizeDb, if(sizeMain == 0) -1 else sizeDb + sizeMain - 1)

            return GroupsOfChildren(children, listOf(db, main))
        }
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
                .filter { it !is DbAction && it !is ApiExternalServiceAction }
            ActionFilter.INIT -> groupsView()!!.getAllInGroup(GroupsOfChildren.INITIALIZATION_SQL).flatMap { (it as ActionComponent).flatten() }
            // WARNING: this can still return DbAction and External ones...
            ActionFilter.NO_INIT -> groupsView()!!.getAllInGroup(GroupsOfChildren.MAIN).flatMap { (it as ActionComponent).flatten() }
            ActionFilter.ONLY_SQL -> seeAllActions().filterIsInstance<DbAction>()
            ActionFilter.NO_SQL -> seeAllActions().filter { it !is DbAction }
            ActionFilter.ONLY_EXTERNAL_SERVICE -> seeAllActions().filterIsInstance<ApiExternalServiceAction>()
            ActionFilter.NO_EXTERNAL_SERVICE -> seeAllActions().filter { it !is ApiExternalServiceAction }
        }
    }

    fun seeMainActionComponents() : List<ActionComponent>{
        return groupsView()!!.getAllInGroup(GroupsOfChildren.MAIN) as List<ActionComponent>
    }


    fun addMainActionInEmptyEnterpriseGroup(relativePosition: Int = -1, action: Action){
        val main = GroupsOfChildren.MAIN
        val g = EnterpriseActionGroup(mutableListOf(action), action.javaClass)

        if (relativePosition < 0) {
            addChildToGroup(g, main)
        } else{
            val base = groupsView()!!.startIndexForGroupInsertionInclusive(main)
            val position = base + relativePosition
            addChildToGroup(position, action, main)
        }
    }

    fun removeMainActionGroupAt(relativePosition: Int){
        val main = GroupsOfChildren.MAIN
        val base = groupsView()!!.startIndexForGroupInsertionInclusive(main)
        val position = base + relativePosition
        killChildByIndex(position)
    }

    /**
     * return a list of all db actions in [this] individual
     * that include all initializing actions plus db actions among main actions.
     *
     * NOTE THAT if EMConfig.probOfApplySQLActionToCreateResources is 0.0, this method
     * would be same with [seeInitializingActions]
     */
    fun seeDbActions() : List<DbAction> = seeActions(ActionFilter.ONLY_SQL) as List<DbAction>

    /**
     * return a list of all external service actions in [this] individual
     * that include all the initializing actions among the main actions
     */
    fun seeExternalServiceActions() : List<ApiExternalServiceAction> = seeActions(ActionFilter.ONLY_EXTERNAL_SERVICE) as List<ApiExternalServiceAction>

    override fun verifyInitializationActions(): Boolean {
        return DbActionUtils.verifyActions(seeInitializingActions().filterIsInstance<DbAction>())
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

    override fun size() = seeMainExecutableActions().size

    private fun getLastIndexOfDbActionToAdd(): Int =
        groupsView()!!.endIndexForGroupInsertionInclusive(GroupsOfChildren.INITIALIZATION_SQL)

    private fun getFirstIndexOfDbActionToAdd(): Int =
        groupsView()!!.startIndexForGroupInsertionInclusive(GroupsOfChildren.INITIALIZATION_SQL)

    /**
     * add [actions] at [relativePosition]
     * if [relativePosition] = -1, append the [actions] at the end
     */
    fun addInitializingDbActions(relativePosition: Int=-1, actions: List<Action>){
        if (relativePosition < 0)  {
            addChildrenToGroup(getLastIndexOfDbActionToAdd(), actions, GroupsOfChildren.INITIALIZATION_SQL)
        } else{
            addChildrenToGroup(getFirstIndexOfDbActionToAdd()+relativePosition, actions, GroupsOfChildren.INITIALIZATION_SQL)
        }
    }

    private fun resetInitializingActions(actions: List<DbAction>){
        killChildren { it is DbAction }
        // TODO: Can be merged with DbAction later
        addChildrenToGroup(getLastIndexOfDbActionToAdd(), actions, GroupsOfChildren.INITIALIZATION_SQL)
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