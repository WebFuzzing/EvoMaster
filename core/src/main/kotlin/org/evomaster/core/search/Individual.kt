package org.evomaster.core.search

import org.evomaster.core.EMConfig
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.external.service.ExternalServiceAction
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchGlobalState
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.tracer.Traceable
import org.evomaster.core.search.tracer.TraceableElementCopyFilter
import org.evomaster.core.search.tracer.TrackOperator
import org.evomaster.core.search.tracer.TrackingHistory
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
abstract class Individual(override var trackOperator: TrackOperator? = null,
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
    }

    init {
        if (isLocalIdsNotAssigned())
            setLocalIdsForChildren(children.flatMap { it.flatView() })
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
    var searchGlobalState : SearchGlobalState? = null


    /**
     * get local id based on the given counter
     */
    fun getLocalId(counter: Int) : String = "Action_COMPONENT_$counter"

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

    fun doGlobalInitialize(){

        //TODO make sure that seeded individual get skipped here

        seeGenes().forEach { it.doGlobalInitialize() }
    }

    fun isInitialized() : Boolean{
        return seeGenes().all { it.initialized }
                && areAllLocalIdsAssigned() // local ids must be assigned
    }


    /**
     * Make sure that all invariants in this individual are satisfied, otherwise throw exception.
     * All invariants should always be satisfied after any modification of the individual.
     * If not, this is a bug.
     */
    fun verifyValidity(){

        groupsView()?.verifyGroups()

        if(!DbActionUtils.verifyActions(seeInitializingActions().filterIsInstance<DbAction>())){
            throw IllegalStateException("Initializing actions break SQL constraints")
        }

        seeAllActions().forEach { a ->
            a.seeTopGenes().forEach { g ->
                if(!g.isGloballyValid()){
                    throw IllegalStateException("Invalid gene ${g.name} in action ${a.getName()}")
                }
            }
        }
    }

    override fun copyContent(): Individual {
        throw IllegalStateException("${this::class.java.simpleName}: copyContent() IS NOT IMPLEMENTED")
    }

    enum class GeneFilter { ALL, NO_SQL, ONLY_SQL, ONLY_EXTERNAL_SERVICE }

    /**
     * Return a view of all the Genes in this chromosome/individual
     */
    abstract fun seeGenes(filter: GeneFilter = GeneFilter.ALL): List<out Gene>

    /**
     * An estimation of the "size" of this individual.
     * Longer/bigger individuals are usually considered worse,
     * unless they cover more coverage targets
     */
    abstract fun size(): Int


    open fun doInitialize(randomness: Randomness? = null){
        //TODO refactor with seeAllActions

//        sequence<Action> {
//            seeInitializingActions()
//            seeActions()
//            seeDbActions()
//        }.toSet().forEach { it.doInitialize(randomness) }

        seeInitializingActions().plus(seeAllActions()).plus(seeDbActions())
                .toSet()
        .forEach { it.doInitialize(randomness) }
    }

    /**
     * @return actions based on the specified [filter]
     *
     * TODO refactor [seeAllActions], [seeInitializingActions] and [seeDbActions] based on this fun
     */
    open fun seeActions(filter: ActionFilter) : List<Action>{
        if(filter == ActionFilter.ALL || filter == ActionFilter.NO_EXTERNAL_SERVICE || filter == ActionFilter.NO_INIT
                || filter == ActionFilter.NO_SQL){
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
     * This method can be overridden to return the concrete action type and not the abstract [Action]
     */
    open fun seeMainExecutableActions() : List<Action>{
        val list = seeActions(ActionFilter.MAIN_EXECUTABLE)
        org.evomaster.core.Lazy.assert { list.all { it.shouldCountForFitnessEvaluations() } }
        return list
    }

    /**
     * Return a view of all initializing actions done before the main
     * ones. Example: these could set up database before doing HTTP
     * calls toward the SUT.
     * A test does not require to have initializing actions.
     */
    fun seeInitializingActions(): List<Action> = seeActions(ActionFilter.INIT)

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
     fun seeExternalServiceActions() : List<ExternalServiceAction> = seeActions(ActionFilter.ONLY_EXTERNAL_SERVICE) as List<ExternalServiceAction>

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
        val all = seeGenes(GeneFilter.ALL).flatMap { it.flatView() }
        all.filter { it.isBoundGene() }.forEach { b->
            b.cleanBrokenReference(all)
        }
    }


    /**
     * @return a gene in [this] based on the [gene] in [individual]
     */
    open fun findGene(individual: Individual, gene: Gene): Gene?{
        // individuals should be same type
        if (individual::class.java.name != this::class.java.name) return null

        val allgenes = individual.seeGenes().flatMap { it.flatView() }
        val all = seeGenes().flatMap { it.flatView() }

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
        val all = seeGenes(GeneFilter.ALL).flatMap{it.flatView()}
        all.forEach { g->
            val inside = g.bindingGeneIsSubsetOf(all)
            if (!inside)
                return false
        }
        return true
    }


    fun isValidIds() : Boolean{
        return (areAllLocalIdsAssigned()
                && seeActions(ActionFilter.ALL).run { this.map { it.getLocalId() }.toSet().size == this.size })
    }

    private fun isLocalIdsNotAssigned() : Boolean{
        return seeActions(filter = ActionFilter.ALL).all { it.getLocalId() == ActionComponent.NONE_ACTION_COMPONENT_ID}
    }

    private fun areAllLocalIdsAssigned() : Boolean{
        return seeActions(filter = ActionFilter.ALL).none { it.getLocalId() == ActionComponent.NONE_ACTION_COMPONENT_ID }
    }

    /**
     * set local ids for all ActionComponents
     */
    private fun setLocalIdsForChildren(children: List<ActionComponent>){
        children.forEach {
            counter++
            it.setLocalId(getLocalId(counter))
        }
    }


    // handle local ids in add child and children
    private fun handleLocalIdsForAddition(children: List<StructuralElement>){
        children.forEach {child->
            if (child is ActionComponent){
                if (child is Action && !child.hasLocalId())
                    setLocalIdsForChildren(listOf(child))

                child.flatView().filterIsInstance<ActionTree>().forEach { tree->
                    if (!tree.hasLocalId()){
                        setLocalIdsForChildren(listOf(tree))

                    if (tree.flatten().none { it.hasLocalId() })
                        setLocalIdsForChildren(child.flatten())
                    }
                }
            }else
                throw IllegalStateException("children of an individual must be ActionComponent, but it is ${child::class.java.name}")
        }
    }

    override fun addChild(child: StructuralElement) {
        handleLocalIdsForAddition(listOf(child))
        super.addChild(child)
    }

    override fun addChild(position: Int, child: StructuralElement) {
        handleLocalIdsForAddition(listOf(child))
        super.addChild(position, child)
    }

    override fun addChildren(position: Int, list: List<StructuralElement>) {
        handleLocalIdsForAddition(list)
        super.addChildren(position, list)
    }
}
