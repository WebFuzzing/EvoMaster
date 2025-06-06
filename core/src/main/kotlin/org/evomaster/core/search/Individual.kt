package org.evomaster.core.search

import org.evomaster.core.EMConfig
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionUtils
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.enterprise.EnterpriseIndividual
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.externalservice.ApiExternalServiceAction
import org.evomaster.core.search.action.*
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.interfaces.TaintableGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchGlobalState
import org.evomaster.core.search.service.monitor.ProcessMonitorExcludeField
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.tracer.Traceable
import org.evomaster.core.search.tracer.TraceableElementCopyFilter
import org.evomaster.core.search.tracer.TrackOperator
import org.evomaster.core.search.tracer.TrackingHistory
import org.evomaster.core.utils.CollectionUtils
import org.slf4j.LoggerFactory

/**
 * An individual for the search.
 * Also called "chromosome" in evolutionary algorithms.
 * In our context, most of the time an Individual will represent
 * a single test case, composed by 1 or more "actions" (eg, calls
 * to a RESTful API, SQL operations on a database or WireMock setup)
 *
 * @property trackOperator presents which operator creates the individual, e.g., sampler
 * @property index presents when the individual is created
 * @param children specify the children of the individual with the constructor
 *
 */
abstract class Individual(
    @ProcessMonitorExcludeField
    override var trackOperator: TrackOperator? = null,
    override var index: Int = Traceable.DEFAULT_INDEX,
    children: MutableList<out ActionComponent>,
    childTypeVerifier: (Class<*>) -> Boolean = {k -> ActionComponent::class.java.isAssignableFrom(k)},
    groups : GroupsOfChildren<StructuralElement>? = null
) : Traceable,
    StructuralElement(
        children,
        childTypeVerifier,
        groups
    ), RootElement{


    /**
     * this counter is used to generate ids for actions, ie, its children
     */
    protected var counter = 0

    companion object{
        private val log = LoggerFactory.getLogger(Individual::class.java)

        const val LOCAL_ID_PREFIX_ACTION = "ACTION_COMPONENT"
        const val LOCAL_ID_PREFIX_GENE = "GENE"
    }


    /**
     * presents the evaluated results of the individual once the individual is tracked (i.e., [EMConfig.enableTrackIndividual]).
     *
     * Note that if the evalutedIndividual is tracked (i.e., [EMConfig.enableTrackEvaluatedIndividual]),
     * e do not recommend to track the individual
     */
    override var evaluatedResult: EvaluatedMutation? = null

    /**
     * presents the history of the individual once the individual is tracked (i.e., [EMConfig.enableTrackIndividual]).
     *
     * Note that if the evalutedIndividual is tracked (i.e., [EMConfig.enableTrackEvaluatedIndividual]),
     * we do not recommend to track the individual
     */
    @ProcessMonitorExcludeField
    override var tracking: TrackingHistory<out Traceable>? = null

    /**
     * Mainly used for debugging. keep track from which MIO population this individual was sampled from.
     * if generated at random, or by any other mean, then it is null
     */
    var populationOrigin : String? = null

    /**
     * Reference of the singleton in this search for global state.
     *
     * Note: due to avoiding major refactoring of all samplers and places where individual are instantiated,
     * eg, in unit tests, this is a nullable ref. ie, in some cases, it can be missing, and the code
     * does not assume its presence.
     *
     * However, when running actual search with MIO, its presence is checked
     */
    @ProcessMonitorExcludeField
    var searchGlobalState : SearchGlobalState? = null
        private set

    /**
     * get local id based on the given counter
     */
    private fun getLocalId(obj: StructuralElement, counter: Int) : String {

         val prefix  = when (obj) {
             is ActionComponent -> LOCAL_ID_PREFIX_ACTION
             is Gene -> LOCAL_ID_PREFIX_GENE
             else -> throw IllegalStateException("Only Generate local id for ActionComponent and Gene")
         }

        return "${prefix}_$counter"
    }
    /**
     * Make a deep copy of this individual
     */
    final override fun copy(): Individual{
        val copy = super.copy()
        if (copy !is Individual)
            throw IllegalStateException("mismatched type: the type should be Individual, but it is ${this::class.java.simpleName}")

        // for local ids
        copy.counter = counter

        copy.populationOrigin = this.populationOrigin
        copy.searchGlobalState = this.searchGlobalState
        return copy
    }

    /**
     * do Initialize LocalId if any inclusive child which is ActionComponent or Gene
     * does not have the assigned local id
     */
    fun doInitializeLocalId(){
        handleLocalIdsForAddition(children)
    }

    fun doGlobalInitialize(searchGlobalState : SearchGlobalState){
        if (!areAllLocalIdsAssigned())
            doInitializeLocalId()

        this.searchGlobalState = searchGlobalState

        //make sure that seeded individuals get skipped here, as global initialize might change them
        if(this is EnterpriseIndividual && this.sampleType != SampleType.SEEDED && this.sampleType != SampleType.PREDEFINED) {
            seeTopGenes().forEach { it.doGlobalInitialize() }
        }

        //make sure to disable those genes when initializing a new individual
        val time = searchGlobalState.time.percentageUsedBudget()
        seeFullTreeGenes().filterIsInstance<OptionalGene>()
            .filter { time > it.searchPercentageActive }
            .forEach { it.forbidSelection() }

        resolveAllTempData()

        computeTransitiveBindingGenes()
    }

    fun resolveAllTempData(){
        seeAllActions().forEach {
            val resolved = it.resolveTempData()
            if(!resolved){
                throw IllegalStateException("Failed to resolve temp data in ${it.getName()}")
            }
        }
    }

    fun isInitialized() : Boolean{
        return areAllGeneInitialized()
    }

    private fun areAllGeneInitialized() = seeFullTreeGenes().all { it.initialized }


    /**
     * Make sure that all invariants in this individual are satisfied, otherwise throw exception.
     * All invariants should always be satisfied after any modification of the individual.
     * If not, this is a bug.
     */
    fun verifyValidity(){

        groupsView()?.verifyGroups()

        SqlActionUtils.checkActions(seeInitializingActions().filterIsInstance<SqlAction>())

        seeAllActions().forEach { a ->
            if(!a.isGloballyValid()){
                throw IllegalStateException("Action ${a.getName()} does not satisfy global validity constraints")
            }
            a.seeTopGenes().forEach { g ->
                if(!g.isGloballyValid()){
                    throw IllegalStateException("Global validity failure: invalid gene named '${g.name}' in action ${a.getName()}")
                }
            }
        }

        if(!areAllValidLocalIds()){
            throw IllegalStateException("There are invalid local ids")
        }
    }

    override fun copyContent(): Individual {
        throw IllegalStateException("${this::class.java.simpleName}: copyContent() IS NOT IMPLEMENTED")
    }

    /**
     * Return a view of all the top Genes in this chromosome/individual.
     * This can be filtered out based on the actions in which these genes appear
     */
    abstract fun seeTopGenes(filter: ActionFilter = ActionFilter.ALL): List<Gene>

    /**
     * Given the filter, return all genes, not just the top ones, but the full trees
     */
    fun seeFullTreeGenes(filter: ActionFilter = ActionFilter.ALL) : List<Gene>{
        return seeTopGenes(filter).flatMap { it.flatView() }
    }

    /**
     * An estimation of the "size" of this individual.
     * Longer/bigger individuals are usually considered worse,
     * unless they cover more coverage targets
     */
    abstract fun size(): Int


    open fun doInitialize(randomness: Randomness? = null){
            seeAllActions()
                .forEach { it.doInitialize(randomness) }
    }

    /**
     * @return actions based on the specified [filter].
     * By default, only [ActionFilter.ALL] and [ActionFilter.NO_INIT] are supported.
     */
    open fun seeActions(filter: ActionFilter) : List<Action>{
        if(filter == ActionFilter.ALL || filter == ActionFilter.NO_INIT){
            return seeAllActions()
        }
        LoggingUtil.uniqueWarn(log,"Default implementation only support ALL filter")
        return listOf()
    }


    /**
     * Return a view of all the "actions" defined in this individual.
     * Note: each action could be composed by 0 or more genes
     */
    fun seeAllActions(): List<Action>{
        return (children as List<ActionComponent>).flatMap { it.flatten() }
    }

    /**
     * return a view of the main actions that are executable, like API calls,
     * and not setups like DB and external services.
     * These actions represent the "calls" made toward the SUT, and define the length
     * of the test cases (regardless of the other initializing actions).
     * All these actions are under the child group [ActionFilter.MAIN_EXECUTABLE]
     *
     * This method can be overridden to return the concrete action type and not the abstract [MainAction]
     */
    open fun seeMainExecutableActions() : List<MainAction>{
        val list = seeActions(ActionFilter.MAIN_EXECUTABLE) as List<MainAction>
        org.evomaster.core.Lazy.assert { list.all { it.shouldCountForFitnessEvaluations() } }
        return list
    }

    /**
     * Remove a main action, using relative index between 0 and this.size()
     */
    open fun removeMainExecutableAction(relativeIndex: Int){
        if(seeInitializingActions().isNotEmpty()){
            throw IllegalStateException("For cases in which there are initializing actions, this method must be overridden")
            //also MUST be overwritten if direct children might have subtrees with more than one main action, like in case of RestResource
        }
        //if there is no init action, then the relativeIndex is an actual index
        killChildByIndex(relativeIndex)
    }

    /**
     * Return a view of all initializing actions done before the main
     * ones. Example: these could set up database before doing HTTP
     * calls toward the SUT.
     * A test does not require to have initializing actions.
     */
    fun seeInitializingActions(): List<EnvironmentAction> = seeActions(ActionFilter.INIT) as List<EnvironmentAction>



    /**
     * @return a sequence of actions which are not in initialization and
     * except structure mutator, an index of any action in this sequence is determinate after the construction
     *
     * Note that the method is particular used by impact collections for the individual
     */
    fun seeFixedMainActions() = seeActions(ActionFilter.NO_INIT).filterNot { it is ApiExternalServiceAction }


    /**
     * @return a view of actions which are not in initialization and
     * the index of the action is dynamic without mutation, such as external service
     *
     * for an individual, the external service could be updated based on fitness evaluation,
     * then newly added external service could result in a dynamic index for the actions.
     * then we categorize all such actions as a return of the method
     *
     * Note that the method is particular used by impact collections for the individual
     */
    fun seeDynamicMainActions() = seeActions(ActionFilter.NO_INIT).filterIsInstance<ApiExternalServiceAction>()


    /**
     * Determine if the structure (ie the actions) of this individual
     * can be mutated (eg, add/remove main actions).
     * Note: even if this is false, it would still be possible to
     * mutate the genes in those actions
     */
    open fun canMutateStructure() = false


    /**
     * Returns true if the initialization actions
     * are correct (i.e. all constraints are satisfied)
     */
    abstract fun verifyInitializationActions(): Boolean

    /**
     * Attempts to repair the initialization actions.
     * Initialization actions must pass the verifyInitializationAction()
     * test after this method is invoked.
     */
    abstract fun repairInitializationActions(randomness: Randomness)

    override fun copy(options: TraceableElementCopyFilter): Traceable {
        val copy = copy()
        return when(options){
            TraceableElementCopyFilter.NONE -> copy
            TraceableElementCopyFilter.WITH_TRACK->{
                copy.wrapWithTracking(evaluatedResult, tracking?.copy())
                copy
            }
            TraceableElementCopyFilter.WITH_ONLY_EVALUATED_RESULT ->{
                copy.wrapWithEvaluatedResults(evaluatedResult)
                copy
            }
            else -> throw IllegalArgumentException("NOT support $options")
        }
    }

    override fun next(next: Traceable, copyFilter: TraceableElementCopyFilter, evaluatedResult: EvaluatedMutation): Traceable? {
        tracking?: throw IllegalStateException("cannot create next due to unavailable tracking info")

        val nextInTracking = (next.copy(copyFilter) as Individual).also { this.wrapWithEvaluatedResults(evaluatedResult) }
        pushLatest(nextInTracking)

        val new = (next as Individual).copy()
//        new.wrapWithTracking(
//                evaluatedResult = evaluatedResult,
//                trackingHistory = tracking?.copy(copyFilter)
//        )

        new.wrapWithTracking(
                evaluatedResult = evaluatedResult,
                trackingHistory = tracking
        )

        return new
    }
    /**
     * @return whether this individual has same actions with [other]
     */
    open fun sameActions(other: Individual, excludeInitialization : Boolean = false) : Boolean{
        if (!excludeInitialization || seeInitializingActions().size != other.seeInitializingActions().size)
            return false
        if (seeAllActions().size != other.seeAllActions().size)
            return false
        if (!excludeInitialization || (0 until seeInitializingActions().size).any { seeInitializingActions()[it].getName() != other.seeInitializingActions()[it].getName() })
            return false
        if ((0 until seeAllActions().size).any { seeAllActions()[it].getName() != other.seeAllActions()[it].getName() })
            return false
        return true
    }

    /**
     * @return whether there exist any actions in the individual,
     *  e.g., if false, the individual might be composed of a sequence of genes.
     */
    @Deprecated("Now individuals always have actions as children")
    open fun hasAnyAction()  = seeAllActions().isNotEmpty()


    open fun cleanBrokenBindingReference(){
        val all = seeFullTreeGenes()
        all.filter { it.isBoundGene() }.forEach { b->
            b.cleanBrokenReference(all)
        }
    }

    /**
     * remove all binding all genes in this individual
     */
    fun removeAllBindingAmongGenes(){
        seeFullTreeGenes().forEach { it.cleanBinding() }
    }


    /**
     * @return a gene in [this] based on the [gene] in [individual]
     */
    open fun findGene(individual: Individual, gene: Gene): Gene?{
        // individuals should be same type
        if (individual::class.java.name != this::class.java.name) return null

        val allgenes = individual.seeFullTreeGenes()
        val all = seeFullTreeGenes()

        if (allgenes.size != all.size) return null

        val index = allgenes.indexOf(gene)
        if (index == -1){
            throw IllegalArgumentException("given gene (${gene.name}) does not belong to the individual which contains ${allgenes.joinToString(","){it.name}}")
        }

        val found = all[index]
        if (!gene.possiblySame(found))
            return null

        return found
    }

    /**
     * verify whether all binding genes are in this individual
     */
    fun verifyBindingGenes() : Boolean{
        val all = seeTopGenes(ActionFilter.ALL).flatMap{it.flatView()}
        all.forEach { g->
            val inside = g.bindingGeneIsSubsetOf(all)
            if (!inside)
                return false
        }
        return true
    }

    fun fixGeneBindingsIfNeeded() : Boolean{
        if (!verifyBindingGenes()){
            cleanBrokenBindingReference()
            computeTransitiveBindingGenes()
            return true
        }
        return false
    }


    /**
     * @return an action based on the specified [localId]
     */
    fun findActionByLocalId(localId : String): Action?{
        return seeAllActions().find { it.getLocalId() == localId }
    }

    /**
     * @param isFromInit represents whether the action to target is from init
     * @param actionIndex represents whether the action to target is part of fixedIndexMain group otherwise it is null
     * @param localId represents whether the action to target is part of dynamicMain group otherwise it is null
     * @return an action based on the given info
     */
    fun findAction(isFromInit: Boolean, actionIndex: Int?, localId: String?): Action?{
        if (actionIndex == null && localId == null)
            throw IllegalArgumentException("the actionIndex or localId must be specified to find the action")

        if (isFromInit){
            if (actionIndex == null) {
                throw IllegalArgumentException("actionIndex must be specified in order to find the action from init")
            }
            return if (seeInitializingActions().size > actionIndex)
                 seeInitializingActions()[actionIndex]
            else
//                throw IllegalArgumentException("the specified actionIndex ($actionIndex) exceeds the existing init actions(${seeInitializingActions().size})")
                null
        }else {
            return if (actionIndex == null)
                findActionByLocalId(localId!!)
            else if (seeFixedMainActions().size > actionIndex)
                seeFixedMainActions()[actionIndex]
            else null
        }
    }

    /**
     * @return whether all action components are assigned with valid local ids
     */
    fun areValidActionLocalIds() : Boolean{
        return areAllLocalIdsAssigned()
                && flatView().run { this.map { it.getLocalId() }.toSet().size == this.size }
    }

    fun areAllValidLocalIds() : Boolean{
        val all = flatViewAllStructuralElements()
        val ids = all.map { it.getLocalId() }
        if(ids.contains(NONE_LOCAL_ID)){
            return false
        }
        val duplicates = CollectionUtils.duplicates(ids)
        //check for duplicates
        return duplicates.isEmpty()
    }


    private fun flatViewAllStructuralElements() : List<StructuralElement>{
        return flatView().flatMap {
            if(it !is Action) {
                listOf(it)
            } else {
                // there might be the case whereby the action does not have genes eg, schedule task
                if (it.seeTopGenes().isEmpty())
                    listOf(it)
                else
                    it.seeTopGenes().flatMap {g ->  g.flatView() }
            }
        }
    }

    /**
     * @return if local ids are not initialized
     */
    private fun areAllLocalIdsNotInitialized() : Boolean{
        return flatViewAllStructuralElements().all { !it.hasLocalId ()}
    }

    private fun flatView() : List<ActionComponent>{
        return children
            .flatMap { (it as? ActionComponent)?.flatView() ?: throw IllegalStateException("children of individual must be ActionComponent, but it is ${it::class.java.name}") }
    }

    private fun areAllLocalIdsAssigned() : Boolean{
        return flatViewAllStructuralElements().all { it.hasLocalId ()}
    }

    /**
     * set local ids for all ActionComponents
     */
    private fun setLocalIdsForChildren(children: List<ActionComponent>, withGene: Boolean){
        children.forEach {
            setLocalIdForStructuralElement(listOf(it).plus(
                if (it is Action && withGene){
                    it.seeTopGenes().flatMap { t-> t.flatView() }
                }else listOf()))
        }
    }

    private fun setLocalIdForStructuralElement(elements: List<StructuralElement>){
        elements.forEach {
            counter++
            it.setLocalId(getLocalId(it, counter))
        }
    }


    /**
     * handle local ids of children or descendant to add.
     * These elements might not be mounted yet inside the individual when this method is called
     */
    fun handleLocalIdsForAddition(elements: Collection<StructuralElement>) {
        elements.forEach { child ->

            if (child is Action) {
                if (!child.hasLocalId()) {
                    setLocalIdsForChildren(listOf(child), true)
                }
            } else if (child is ActionTree){
                child.flatView().forEach { a ->
                    if (a is Action) {
                        if (!a.hasLocalId()) {
                            setLocalIdsForChildren(listOf(a), true)
                        }
                    } else {
                        if (!a.hasLocalId()) {
                            setLocalIdsForChildren(listOf(a), false)
                        }
                        // local id can be assigned for flatten of the tree
                        // only if the tree itself and none of its flatten do not have local id
                        if (a.flatten().none { it.hasLocalId() }) {
                            setLocalIdsForChildren(a.flatten(), true)
                        } else if (!a.flatten().all { it.hasLocalId() }) {
                            throw IllegalStateException("local ids of ActionTree are partially assigned")
                        }
                    }
                }
            } else if (child is Gene) {
                if (child.flatView().none { it.hasLocalId() })
                    setLocalIdForStructuralElement(child.flatView())
                else if (!child.flatView().all { it.hasLocalId() })
                    throw IllegalStateException("local ids of Gene to add are partially assigned")
            } else if (child is Param){
                setLocalIdForStructuralElement(child.genes.flatMap { it.flatView() })
            } else {
                throw IllegalStateException("children of an individual must be ActionComponent, but it is ${child::class.java.name}")
            }
        }
    }

    /**
     * @return Initializing actions with its relative index
     * note that relative index indicates the index in terms of [seeInitializingActions()]
     */
    fun getRelativeIndexedInitActions() : List<Pair<Action, Int>>{
        return seeInitializingActions().mapIndexed { index, action -> action to index }
    }

    /**
     * @return non-init actions with its relative index
     * note that relative index indicates the index in terms of [seeFixedMainActions()]
     */
    fun getRelativeIndexedNonInitAction() : List<Pair<Action, Int?>>{
        return seeActions(ActionFilter.NO_INIT).map {
            if (seeFixedMainActions().contains(it))
                it to seeFixedMainActions().indexOf(it)
            else
                it to null
        }
    }

    /**
     * @return given [actions] with its relative index
     * note that relative index indicates the index in terms of [seeFixedMainActions()] and [seeInitializingActions]
     */
    fun getRelativeInitAndFixedMainIndex(actions: List<Action>) : List<Pair<Action, Int?>>{
        return actions.map {
            if (seeInitializingActions().contains(it))
                it to seeInitializingActions().indexOf(it)
            else if (seeFixedMainActions().contains(it))
                it to seeFixedMainActions().indexOf(it)
            else if (seeDynamicMainActions().contains(it))
                it to null
            else
                throw IllegalStateException("cannot find the action (name: ${it.getName()}) in this individual")
        }
    }


    /**
     * @return whether all top genes of [this] individual are locally valid
     */
    fun areAllTopGenesLocallyValid() : Boolean{
        return seeTopGenes().all { it.isLocallyValid() }
    }

    /**
     * compute transitive binding relationship for all genes in this individual
     */
    fun computeTransitiveBindingGenes(){
        seeTopGenes().forEach(Gene::computeAllTransitiveBindingGenes)
    }

    /**
     * When a test case is executed, we might discover few things, like new query parameters or
     * string specializations due to taint analysis.
     *
     * @return counter of new discovered info
     */
    fun numberOfDiscoveredInfoFromTestExecution() : Int {
        return seeFullTreeGenes()
            .asSequence()
            .filterIsInstance<TaintableGene>()
            .filter { it is Gene && it.staticCheckIfImpactPhenotype() } //in case disabled since then
            .count { it.hasDormantGenes() }
        //TODO other info like discovered query-parameters/headers
    }
}
