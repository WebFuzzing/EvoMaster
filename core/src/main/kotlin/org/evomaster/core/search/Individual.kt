package org.evomaster.core.search

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.external.service.ExternalServiceAction
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchGlobalState
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.tracer.Traceable
import org.evomaster.core.search.tracer.TraceableElementCopyFilter
import org.evomaster.core.search.tracer.TrackOperator
import org.evomaster.core.search.tracer.TrackingHistory

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
                          children: List<StructuralElement>
) : Traceable, StructuralElement(children.toMutableList()), RootElement{

    /**
     * this counter is used to generate ids for actions, ie, its children
     */
    protected var counter = 0

    init {
        if (isLocalIdsNotAssigned())
            setLocalIdsForChildrenAsActions(seeActions(filter = ActionFilter.ALL))
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
    fun getLocalId(counter: Int) : String = "Action_$counter"

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



    /**
     * @return actions based on the specified [filter]
     *
     * TODO refactor [seeActions], [seeInitializingActions] and [seeDbActions] based on this fun
     *
     * Man: note that seeActions(filter = ActionFilter.ALL) is used to initialize localIds for actions
     */
    open fun seeActions(filter: ActionFilter) : List<out Action>{
        return seeActions()
    }

    open fun doInitialize(randomness: Randomness? = null){
        //TODO refactor with seeAllActions

//        sequence<Action> {
//            seeInitializingActions()
//            seeActions()
//            seeDbActions()
//        }.toSet().forEach { it.doInitialize(randomness) }

        seeInitializingActions().plus(seeActions()).plus(seeDbActions())
                .toSet()
        .forEach { it.doInitialize(randomness) }
    }

    /**
     * Return a view of all the "actions" defined in this individual.
     * Note: each action could be composed by 0 or more genes
     */
    abstract fun seeActions(): List<out Action>

    /**
     * Return a view of all initializing actions done before the main
     * ones. Example: these could set up database before doing HTTP
     * calls toward the SUT.
     * A test does not require to have initializing actions.
     */
    open fun seeInitializingActions(): List<Action> = listOf()

    /**
     * return a list of all db actions in [this] individual
     * that include all initializing actions plus db actions among rest actions.
     *
     * NOTE THAT if EMConfig.probOfApplySQLActionToCreateResources is 0.0, this method
     * would be same with [seeInitializingActions]
     */
    open fun seeDbActions() : List<DbAction> = seeInitializingActions().filterIsInstance<DbAction>()

    /**
     * return a list of all external service actions in [this] individual
     * that include all the initializing actions plus external service actions
     * among rest actions
     */
    open fun seeExternalServiceActions() : List<ExternalServiceAction> = seeInitializingActions().filterIsInstance<ExternalServiceAction>()

    /**
     * Determine if the structure (ie the actions) of this individual
     * can be mutated (eg, add/remove actions).
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
        if (seeActions().size != other.seeActions().size)
            return false
        if (!excludeInitialization || (0 until seeInitializingActions().size).any { seeInitializingActions()[it].getName() != other.seeInitializingActions()[it].getName() })
            return false
        if ((0 until seeActions().size).any { seeActions()[it].getName() != other.seeActions()[it].getName() })
            return false
        return true
    }

    /**
     * @return whether there exist any actions in the individual,
     *  e.g., if false, the individual might be composed of a sequence of genes.
     */
    open fun hasAnyAction()  = seeActions().isNotEmpty()


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
        return seeActions(filter = ActionFilter.ALL).all { it.getLocalId() == Action.NONE_ACTION_ID }
    }

    private fun areAllLocalIdsAssigned() : Boolean{
        return seeActions(filter = ActionFilter.ALL).none { it.getLocalId() == Action.NONE_ACTION_ID }
    }

    private fun setLocalIdsForChildrenAsActions(children: List<Action>){
        children.forEach {
            counter++
            it.setLocalId(getLocalId(counter))
        }
    }


    // handle local ids in add child and children

    private fun handleLocalIdsForAddition(children: List<StructuralElement>){
        children.forEach {child->
            if (child is Action && !child.hasLocalId()){
                setLocalIdsForChildrenAsActions(listOf(child))
            }

            // Man: if remove RestResourceCalls, this is need to be refactored
            if (child is RestResourceCalls && child.seeActions(ActionFilter.ALL).none { it.hasLocalId() })
                setLocalIdsForChildrenAsActions(child.seeActions(ActionFilter.ALL))
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

    // handle removal based on dependencies among actions

    /**
     * @param childrenToRemove a list of elements to remove
     * @return a list of elements together with dependent actions to remove (ie, childrenToRemove plus actions which depends on childrenToRemove)
     */
    private fun getActionToRemove(childrenToRemove : List<StructuralElement>): MutableList<StructuralElement>{

        val allActions = seeActions(ActionFilter.ALL)

        val elementsToRemove = mutableListOf<StructuralElement>()

        childrenToRemove.forEach { element->

            if (element is Action){
                if (elementsToRemove.filterIsInstance<Action>().none { it.getLocalId() ==  element.getLocalId()})
                    elementsToRemove.add(element)

                element.dependentActions.forEach {r->
                    if (elementsToRemove.none { it is Action && it.getLocalId() != r }){
                        val dActions = allActions.filter { it.getLocalId() == r }
                        getActionToRemove(dActions).forEach {d->
                            if (d is Action && elementsToRemove.filterIsInstance<Action>().none { it.getLocalId() ==  d.getLocalId()}){
                                if (!children.contains(d)){
                                    throw IllegalStateException("the action to remove with id (${d.getLocalId()}) is not part of children of this individual")
                                }
                                elementsToRemove.add(element)
                            }
                        }
                    }
                }
            }else{

                if (!elementsToRemove.contains(element))
                    elementsToRemove.add(element)

                if (element is RestResourceCalls){
                    element.seeActions(ActionFilter.ALL).forEach {a->
                        a.dependentActions.forEach { r->
                            if (elementsToRemove.none { it is Action && it.getLocalId() != r }){
                                val dActions = allActions.filter { it.getLocalId() == r }
                                getActionToRemove(dActions).forEach {d->
                                    if (d is Action && elementsToRemove.filterIsInstance<Action>().none { it.getLocalId() ==  d.getLocalId()}){
                                        if (!children.contains(d)){
                                            throw IllegalStateException("the action to remove with id (${d.getLocalId()}) is not part of children of this individual")
                                        }
                                        elementsToRemove.add(element)
                                    }
                                }
                            }
                        }
                    }

                    // TODO might need to handle it at RestResourceCall level
                }
            }
        }


        return elementsToRemove
    }

    override fun killChild(child: StructuralElement) {
        val dependedToRemove = getActionToRemove(listOf(child))
        if (dependedToRemove.isNotEmpty()){
            dependedToRemove.filter { children.contains(it) }.forEach {d->
                super.killChild(d)
            }
        }
    }

    override fun killChildByIndex(index: Int): StructuralElement {
        val removed = super.killChildByIndex(index)
        val dependedToRemove = getActionToRemove(listOf(removed)).filter { it != removed }
        if (dependedToRemove.isNotEmpty()){
            dependedToRemove.forEach { d->
                super.killChild(d)
            }
        }
        return removed
    }
}
