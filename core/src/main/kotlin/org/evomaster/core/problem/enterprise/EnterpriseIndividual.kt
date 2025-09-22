package org.evomaster.core.problem.enterprise

import org.evomaster.core.Lazy
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionUtils
import org.evomaster.core.mongo.MongoDbAction
import org.evomaster.core.problem.api.ApiWsIndividual
import org.evomaster.core.problem.externalservice.ApiExternalServiceAction
import org.evomaster.core.problem.externalservice.HostnameResolutionAction
import org.evomaster.core.scheduletask.ScheduleTaskAction
import org.evomaster.core.search.*
import org.evomaster.core.search.action.*
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.tracer.TrackOperator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*


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
    //see https://discuss.kotlinlang.org/t/private-setter-for-var-in-primary-constructor/3640/11
    private var sampleTypeField: SampleType,
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
    childTypeVerifier: EnterpriseChildTypeVerifier,
    /**
     * if no group definition is specified, then it is assumed that all action are for the MAIN group
     */
    groups : GroupsOfChildren<StructuralElement> = getEnterpriseTopGroups(children,children.size,0, 0, 0, 0, 0),
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
         * The first [sizeSQL] are assumed to be database actions, followed by [sizeMain] main actions
         */
        fun getEnterpriseTopGroups(
            children: List<ActionComponent>,
            sizeMain: Int,
            sizeSQL: Int,
            sizeMongo: Int,
            sizeDNS: Int,
            sizeScheduleTasks: Int,
            sizeCleanUp: Int,
        ) : GroupsOfChildren<StructuralElement>{

            if(children.size != sizeSQL +sizeMongo + sizeDNS + sizeScheduleTasks + sizeMain + sizeCleanUp){
                throw IllegalArgumentException("Group size mismatch. Expected a total of ${children.size}, but" +
                        " got main=$sizeMain,  sql=$sizeSQL, mongo=$sizeMongo, dns=$sizeDNS, scheduleTasks=$sizeScheduleTasks, sizeCleanUp=$sizeCleanUp")
            }
            if(sizeSQL < 0){
                throw IllegalArgumentException("Negative size for sizeSQL: $sizeSQL")
            }
            if(sizeMongo < 0){
                throw IllegalArgumentException("Negative size for sizeMongo: $sizeMain")
            }
            if(sizeDNS < 0){
                throw IllegalArgumentException("Negative size for sizeDNS: $sizeMain")
            }
            if(sizeScheduleTasks < 0){
                throw IllegalArgumentException("Negative size for sizeScheduleTasks: $sizeMain")
            }
            if(sizeMain < 0){
                throw IllegalArgumentException("Negative size for sizeMain: $sizeMain")
            }
            if(sizeCleanUp < 0){
                throw IllegalArgumentException("Negative size for sizeCleanUp: $sizeCleanUp")
            }

            /*
                TODO in future ll need to refactor to handle multiple databases, possibly handled with
                one action group per database...
                but is it really common that a SUT directly access several databases of same kind?
             */

            val startIndexSQL = children.indexOfFirst { a -> a is SqlAction }
            val endIndexSQL = children.indexOfLast { a -> a is SqlAction }
            val db = ChildGroup<StructuralElement>(GroupsOfChildren.INITIALIZATION_SQL,{e -> e is ActionComponent && e.flatten().all { a -> a is SqlAction }},
                if(sizeSQL==0) -1 else startIndexSQL , if(sizeSQL==0) -1 else endIndexSQL
            )

            val startIndexMongo = children.indexOfFirst { a -> a is MongoDbAction }
            val endIndexMongo = children.indexOfLast { a -> a is MongoDbAction }
            val mongodb = ChildGroup<StructuralElement>(GroupsOfChildren.INITIALIZATION_MONGO,{e -> e is ActionComponent && e.flatten().all { a -> a is MongoDbAction }},
                if(sizeMongo==0) -1 else startIndexMongo , if(sizeMongo==0) -1 else endIndexMongo
            )

            val startIndexDns = children.indexOfFirst { a -> a is HostnameResolutionAction }
            val endIndexDns = children.indexOfLast { a -> a is HostnameResolutionAction }
            val dns = ChildGroup<StructuralElement>(GroupsOfChildren.INITIALIZATION_DNS,{e -> e is ActionComponent && e.flatten().all { a -> a is HostnameResolutionAction }},
                if(sizeDNS==0) -1 else startIndexDns , if(sizeDNS==0) -1 else endIndexDns
            )

            val startIndexScheduleTasks = children.indexOfFirst { a -> a is ScheduleTaskAction }
            val endIndexScheduleTasks = children.indexOfLast { a -> a is ScheduleTaskAction }
            val schedule = ChildGroup<StructuralElement>(GroupsOfChildren.INITIALIZATION_SCHEDULE_TASK,{e -> e is ActionComponent && e.flatten().all { a -> a is ScheduleTaskAction }},
                if(sizeScheduleTasks==0) -1 else startIndexScheduleTasks , if(sizeScheduleTasks==0) -1 else endIndexScheduleTasks
            )

            val initSize = sizeSQL+sizeMongo+sizeDNS+sizeScheduleTasks
            val startIndexMain = initSize
            val endIndexMain =  initSize + sizeMain - 1

            val main = ChildGroup<StructuralElement>(GroupsOfChildren.MAIN, {e -> e !is EnvironmentAction },
                if(sizeMain == 0) -1 else startIndexMain, if(sizeMain == 0) -1 else initSize + sizeMain - 1)

            val cleanup = ChildGroup<StructuralElement>(GroupsOfChildren.CLEANUP, {e -> true},
                if(sizeCleanUp == 0) -1 else endIndexMain+1, if(sizeCleanUp == 0) -1 else endIndexMain + sizeCleanUp)

            return GroupsOfChildren(children, listOf(db, mongodb, dns, schedule, main, cleanup))
        }
    }

    val sampleType get() = sampleTypeField

    /**
     * This should never happen directly during the search.
     * However, we might manually create new individuals by modifying and copying existing individuals.
     * In those cases it simple to modify the sample directly, instead of re-building with same actions
     * (which actually could be a possibility...).
     */
    fun modifySampleType(x: SampleType){
        sampleTypeField = x
    }

    /**
     * a list of db actions for its Initialization
     */
    private val sqlInitialization: List<SqlAction>
        get() {
            return groupsView()!!.getAllInGroup(GroupsOfChildren.INITIALIZATION_SQL)
                .flatMap { (it as ActionComponent).flatten() }
                .map { it as SqlAction }
        }


    /**
     * Make sure that no secondary type is used in the main actions, and that only [EnterpriseActionGroup]
     * are used.
     *
     * Ideally, a flattening should not impact fitness, but, in few cases, it might :(
     * This happens eg in Rest Resource, if a middle SQL action is moved into initialization group and impact
     * state of previous REST actions (an example of this did happen for example for CrossFkEMTest...).
     * This means that, after a flattening, to be on safe side should recompute fitness
     *
     * @return true if there is potential (but not necessarily) issues, and fitness might be stale
     */
    fun ensureFlattenedStructure() : Boolean{

        val before = seeAllActions().size

        val issues = doFlattenStructure()

        //make sure the flattening worked
        Lazy.assert { isFlattenedStructure() }
        //no base action should have been lost
        Lazy.assert { seeAllActions().size == before }

        /*
            FIXME There is some major bugs in Gene regarding
            copyValueFrom() and setFromDifferentGene()
            until fixed, this check will fail.
            but the fix will require some major refactoring and testing... it will take time
            TODO put back once fixed
         */
        //verifyValidity()

        return issues
    }

    protected open fun doFlattenStructure() : Boolean{
        //for most types, there is nothing to do.
        //can be overridden if needed
        return false
    }

    private fun isFlattenedStructure() : Boolean{
        return groupsView()!!.getAllInGroup(GroupsOfChildren.MAIN).all { it is EnterpriseActionGroup<*> }
    }


    final override fun seeActions(filter: ActionFilter) : List<Action>{
        return when (filter) {
            ActionFilter.ALL -> seeAllActions()
            ActionFilter.MAIN_EXECUTABLE -> groupsView()!!.getAllInGroup(GroupsOfChildren.MAIN)
                .flatMap { (it as ActionComponent).flatten() }
                .filter { it !is SqlAction && it !is ApiExternalServiceAction }
            ActionFilter.INIT ->
                groupsView()!!
                    .getAllInGroup(GroupsOfChildren.INITIALIZATION_SQL).flatMap { (it as ActionComponent).flatten() } + groupsView()!!
                    .getAllInGroup(GroupsOfChildren.INITIALIZATION_MONGO).flatMap { (it as ActionComponent).flatten()}+ groupsView()!!
                    .getAllInGroup(GroupsOfChildren.INITIALIZATION_DNS).flatMap { (it as ActionComponent).flatten()} + groupsView()!!
                    .getAllInGroup(GroupsOfChildren.INITIALIZATION_SCHEDULE_TASK).flatMap { (it as ActionComponent).flatten() }
            // WARNING: this can still return DbAction, MongoDbAction and External ones...
            ActionFilter.NO_INIT -> groupsView()!!.getAllInGroup(GroupsOfChildren.MAIN).flatMap { (it as ActionComponent).flatten() }
            ActionFilter.ONLY_SQL -> seeAllActions().filterIsInstance<SqlAction>()
            ActionFilter.ONLY_MONGO -> seeAllActions().filterIsInstance<MongoDbAction>()
            ActionFilter.NO_SQL -> seeAllActions().filter { it !is SqlAction }
            ActionFilter.ONLY_DB -> seeAllActions().filter { it is SqlAction || it is MongoDbAction }
            ActionFilter.NO_DB -> seeAllActions().filter { it !is SqlAction && it !is MongoDbAction }
            ActionFilter.ONLY_EXTERNAL_SERVICE -> seeAllActions().filterIsInstance<ApiExternalServiceAction>()
            ActionFilter.NO_EXTERNAL_SERVICE -> seeAllActions().filter { it !is ApiExternalServiceAction }.filter { it !is HostnameResolutionAction }
            ActionFilter.ONLY_DNS -> groupsView()!!.getAllInGroup(GroupsOfChildren.INITIALIZATION_DNS).flatMap { (it as ActionComponent).flatten()}
            ActionFilter.ONLY_SCHEDULE_TASK -> groupsView()!!.getAllInGroup(GroupsOfChildren.INITIALIZATION_SCHEDULE_TASK).flatMap { (it as ActionComponent).flatten() }
        }
    }


    fun seeCleanUpActions() : List<ActionComponent>{
        return groupsView()!!.getAllInGroup(GroupsOfChildren.CLEANUP) as List<ActionComponent>
    }

    fun seeMainActionComponents() : List<ActionComponent>{
        return groupsView()!!.getAllInGroup(GroupsOfChildren.MAIN) as List<ActionComponent>
    }


    fun addMainEnterpriseActionGroup(group: EnterpriseActionGroup<*>){
        val main = GroupsOfChildren.MAIN
        addChildToGroup(group, main)
    }

    fun addMainActionInEmptyEnterpriseGroup(relativePosition: Int = -1, action: MainAction){
        val main = GroupsOfChildren.MAIN
        val g = EnterpriseActionGroup(mutableListOf(action), action.javaClass)

        if (relativePosition < 0) {
            addChildToGroup(g, main)
        } else{
            val base = groupsView()!!.startIndexForGroupInsertionInclusive(main)
            val position = base + relativePosition
            addChildToGroup(position, g, main)
        }
    }

    fun removeMainActionGroupAt(relativePosition: Int){
        val main = GroupsOfChildren.MAIN
        val base = groupsView()!!.startIndexForGroupInsertionInclusive(main)
        val position = base + relativePosition
        killChildByIndex(position)
    }

    override fun removeMainExecutableAction(relativeIndex: Int){
        removeMainActionGroupAt(relativeIndex)
    }

    /**
     * @return SQL all actions before relativeIndex (exclusive) in GroupsOfChildren.MAIN
     */
    fun seeSQLActionBeforeIndex(relativeIndex: Int): List<SqlAction>{
        val main = GroupsOfChildren.MAIN
        val base = groupsView()!!.startIndexForGroupInsertionInclusive(main)
        return (0 until relativeIndex).flatMap {
            (children[base + it] as ActionComponent).flatten()
        }.filterIsInstance<SqlAction>()
    }

    /**
     * return a list of all db actions in [this] individual
     * that include all initializing actions plus db actions among main actions.
     *
     */
    fun seeSqlDbActions() : List<SqlAction> = seeActions(ActionFilter.ONLY_SQL) as List<SqlAction>

    fun seeMongoDbActions() : List<MongoDbAction> = seeActions(ActionFilter.ONLY_MONGO) as List<MongoDbAction>

    fun seeScheduleTaskActions() : List<ScheduleTaskAction> = seeActions(ActionFilter.ONLY_SCHEDULE_TASK) as List<ScheduleTaskAction>

    fun seeHostnameActions() : List<HostnameResolutionAction> = seeActions(ActionFilter.ONLY_DNS) as List<HostnameResolutionAction>

    /**
     * return a list of all external service actions in [this] individual
     * that include all the initializing actions among the main actions
     */
    fun seeExternalServiceActions() : List<ApiExternalServiceAction> = seeActions(ActionFilter.ONLY_EXTERNAL_SERVICE) as List<ApiExternalServiceAction>

    override fun verifyInitializationActions(): Boolean {
        return SqlActionUtils.verifyActions(seeInitializingActions().filterIsInstance<SqlAction>())
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

        GeneUtils.repairGenes(this.seeFullTreeGenes(ActionFilter.ONLY_SQL))

        /**
         * Now repair database constraints (primary keys, foreign keys, unique fields, etc.).
         *
         * Note: this is only for DB Actions in the initialization phase, and NOT if there is any
         * afterwards (eg in resource-based handling).
         */
        if (!verifyInitializationActions()) {
            if (log.isTraceEnabled)
                log.trace("invoke GeneUtils.repairBrokenDbActionsList")
            val previous = sqlInitialization.toMutableList()
            val relatedActionInMain = seeFixedMainActions().flatMap { it.flatten() }.filterIsInstance<SqlAction>()
            SqlActionUtils.repairBrokenDbActionsList(previous.plus(relatedActionInMain).toMutableList(), randomness)
            resetInitializingActions(previous)
            Lazy.assert{verifyInitializationActions()}
        }
    }

    override fun hasAnyAction(): Boolean {
        return super.hasAnyAction() || sqlInitialization.isNotEmpty()
    }

    override fun size() = seeMainExecutableActions().size

    private fun getLastIndexOfDbActionToAdd(): Int =
        groupsView()!!.endIndexForGroupInsertionInclusive(GroupsOfChildren.INITIALIZATION_SQL)

    private fun getLastIndexOfMongoDbActionToAdd(): Int =
        groupsView()!!.endIndexForGroupInsertionInclusive(GroupsOfChildren.INITIALIZATION_MONGO)

    private fun getLastIndexOfHostnameResolutionActionToAdd(): Int =
        groupsView()!!.endIndexForGroupInsertionInclusive(GroupsOfChildren.INITIALIZATION_DNS)

    private fun getLastIndexOfScheduleTaskActionToAdd(): Int =
        groupsView()!!.endIndexForGroupInsertionInclusive(GroupsOfChildren.INITIALIZATION_SCHEDULE_TASK)

    private fun getFirstIndexOfDbActionToAdd(): Int =
        groupsView()!!.startIndexForGroupInsertionInclusive(GroupsOfChildren.INITIALIZATION_SQL)

    private fun getFirstIndexOfMongoDbActionToAdd(): Int =
        groupsView()!!.startIndexForGroupInsertionInclusive(GroupsOfChildren.INITIALIZATION_MONGO)

    private fun getFirstIndexOfHostnameResolutionActionToAdd(): Int =
        groupsView()!!.startIndexForGroupInsertionInclusive(GroupsOfChildren.INITIALIZATION_DNS)

    private fun getFirstIndexOfScheduleTaskActionToAdd(): Int =
        groupsView()!!.startIndexForGroupInsertionInclusive(GroupsOfChildren.INITIALIZATION_SCHEDULE_TASK)


    /**
     * Add all input initializing actions to the current ones in this individual.
     *
     * At time, some actions must be "unique".
     * In those cases, we don't crash this function, but rather return the number of
     * unneeded actions that are ignored
     */
    fun addInitializingActions(actions: List<EnvironmentAction>): Int {

        val invalid = actions.filter { it !is SqlAction && it !is MongoDbAction && it !is HostnameResolutionAction }
        if(invalid.isNotEmpty()){
            throw IllegalArgumentException("Invalid ${invalid.size} environment actions of type:" +
                    " ${invalid.map { it::class.java.simpleName }.toSet().joinToString(", ")}")
        }

        addInitializingDbActions(actions = actions.filterIsInstance<SqlAction>())
        addInitializingMongoDbActions(actions = actions.filterIsInstance<MongoDbAction>())
        addInitializingScheduleTaskActions(actions = actions.filterIsInstance<ScheduleTaskAction>())

        //we don't need duplicates in hostname actions
        val hostnameActions = actions
            .filterIsInstance<HostnameResolutionAction>()
        val uniqueHostnames = hostnameActions
            .filter { dns -> this.seeHostnameActions().none { it.hostname == dns.hostname } }

        addInitializingHostnameResolutionActions(actions = uniqueHostnames)

        return  hostnameActions.size - uniqueHostnames.size
    }


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

    fun addInitializingScheduleTaskActions(relativePosition: Int=-1, actions: List<Action>){
        if (relativePosition < 0)  {
            addChildrenToGroup(getLastIndexOfScheduleTaskActionToAdd(), actions, GroupsOfChildren.INITIALIZATION_SCHEDULE_TASK)
        } else{
            addChildrenToGroup(getFirstIndexOfScheduleTaskActionToAdd()+relativePosition, actions, GroupsOfChildren.INITIALIZATION_SCHEDULE_TASK)
        }
    }

    fun addInitializingMongoDbActions(relativePosition: Int=-1, actions: List<Action>){
        if (relativePosition < 0)  {
            addChildrenToGroup(getLastIndexOfMongoDbActionToAdd(), actions, GroupsOfChildren.INITIALIZATION_MONGO)
        } else{
            addChildrenToGroup(getFirstIndexOfMongoDbActionToAdd()+relativePosition, actions, GroupsOfChildren.INITIALIZATION_MONGO)
        }
    }

    fun addInitializingHostnameResolutionActions(relativePosition: Int=-1, actions: List<Action>) {
        if (relativePosition < 0) {
            addChildrenToGroup(getLastIndexOfHostnameResolutionActionToAdd(), actions, GroupsOfChildren.INITIALIZATION_DNS)
        } else {
            addChildrenToGroup(getFirstIndexOfHostnameResolutionActionToAdd()+relativePosition, actions, GroupsOfChildren.INITIALIZATION_DNS)
        }
    }

    fun addCleanUpAction(action: ActionComponent){
        addChildToGroup(action, GroupsOfChildren.CLEANUP)
    }

    private fun resetInitializingActions(actions: List<SqlAction>){
        killChildren { it is SqlAction }
        // TODO: Can be merged with DbAction later
        addChildrenToGroup(getLastIndexOfDbActionToAdd(), actions, GroupsOfChildren.INITIALIZATION_SQL)
    }

    /**
     * remove specified dbactions i.e., [actions] from [sqlInitialization]
     */
    fun removeInitDbActions(actions: List<SqlAction>) {
        killChildren { it is SqlAction && actions.contains(it)}
    }

    /***
     * remove specified list of [HostnameResolutionAction] from the initializing actions.
     */
    fun removeHostnameResolutionAction(actions: List<HostnameResolutionAction>) {
        killChildren {  it is HostnameResolutionAction && actions.contains(it) }
    }

    /**
     * @return a list table names which are used to insert data directly
     */
    open fun getInsertTableNames(): List<String>{
        return sqlInitialization.filterNot { it.representExistingData }.map { it.table.name }
    }

    override fun seeTopGenes(filter: ActionFilter): List<Gene> {
        return seeActions(filter).flatMap { it.seeTopGenes() }
    }


    fun removeAllCleanUp(){
        killAllInGroup(GroupsOfChildren.CLEANUP)
    }

    /**
     * Initialize dynamically added cleanup actions.
     * Recompute everything for whole individual might not be feasible,
     * as usually this is needed in the middle of a fitness evaluation
     */
    fun initializeCleanUpActions(){

        handleLocalIdsForAddition(seeCleanUpActions())

        // TODO need to handle global initialization.
        // this is just a temporary solution, would need to call full doGlobalInitialize(), but that
        // needs refactoring to be applied to subset of actions.
    }
}
