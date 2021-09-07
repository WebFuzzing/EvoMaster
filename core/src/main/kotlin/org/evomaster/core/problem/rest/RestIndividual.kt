package org.evomaster.core.problem.rest

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.problem.httpws.service.HttpWsIndividual
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.resource.SamplerSpecification
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.ActionFilter.*
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.tracer.Traceable
import org.evomaster.core.search.tracer.TraceableElementCopyFilter
import org.evomaster.core.search.tracer.TrackOperator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 *
 * @property trackOperator indicates which operator create this individual.
 * @property index indicates when the individual is created, ie, using num of evaluated individual.
 */
class RestIndividual(
        //val actions: MutableList<RestAction>,
        private val resourceCalls: MutableList<RestResourceCalls>,
        val sampleType: SampleType,
        val sampleSpec: SamplerSpecification? = null,
        dbInitialization: MutableList<DbAction> = mutableListOf(),

        trackOperator: TrackOperator? = null,
        index : Int = -1
): HttpWsIndividual (dbInitialization, trackOperator, index, mutableListOf<StructuralElement>().apply {
    addAll(dbInitialization); addAll(resourceCalls)
}) {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(RestIndividual::class.java)
    }

    constructor(
            actions: MutableList<out Action>,
            sampleType: SampleType,
            dbInitialization: MutableList<DbAction> = mutableListOf(),
            trackOperator: TrackOperator? = null,
            index : Int = Traceable.DEFAULT_INDEX) :
            this(
                    actions.map { RestResourceCalls(actions= mutableListOf(it as RestCallAction)) }.toMutableList(),
                    sampleType,
                    null,
                    dbInitialization,
                    trackOperator,
                    index
            )


    override fun copyContent(): Individual {
        return RestIndividual(
                resourceCalls.map { it.copyContent() }.toMutableList(),
                sampleType,
                sampleSpec?.copy(),
                seeInitializingActions().map { d -> d.copyContent() as DbAction } as MutableList<DbAction>,
                trackOperator,
                index
        )
    }

    override fun getChildren(): List<StructuralElement> = seeInitializingActions().plus(resourceCalls)

    override fun canMutateStructure(): Boolean {
        return sampleType == SampleType.RANDOM ||
                sampleType == SampleType.SMART_GET_COLLECTION ||
                sampleType == SampleType.SMART_RESOURCE
    }

    /**
     * Note that if resource-mio is enabled, [dbInitialization] of a RestIndividual is always empty, since DbActions are created
     * for initializing an resource for a set of actions on the same resource.
     * This effects on a configuration with respect to  [EMConfig.geneMutationStrategy] is ONE_OVER_N when resource-mio is enabled.
     *
     * In another word, if resource-mio is enabled, whatever [EMConfig.geneMutationStrategy] is, it always follows "GeneMutationStrategy.ONE_OVER_N_BIASED_SQL"
     * strategy.
     *
     * TODO : modify return genes when GeneFilter is one of [GeneFilter.ALL] and [GeneFilter.ONLY_SQL]
     */
    override fun seeGenes(filter: GeneFilter): List<out Gene> {

        return when (filter) {
            GeneFilter.ALL -> seeDbActions().flatMap(DbAction::seeGenes).plus(seeActions().flatMap(Action::seeGenes))
            GeneFilter.NO_SQL -> seeActions().flatMap(Action::seeGenes)
            GeneFilter.ONLY_SQL -> seeDbActions().flatMap(DbAction::seeGenes)
        }
    }

    enum class ResourceFilter { ALL, NO_SQL, ONLY_SQL, ONLY_SQL_INSERTION, ONLY_SQL_EXISTING }

    /**
     * @return involved resources in [this] RestIndividual
     * for all [resourceCalls], we return their resource keys to represent the resource
     * for dbActions, we employ their table names to represent the resource
     *
     * NOTE that for [RestResourceCalls], there might exist DbAction as well.
     * But since the dbaction is to prepare resource for the endpoint whose goal is equivalent with POST here,
     * we do not consider such dbactions as separated resources from the endpoint.
     */
    fun seeResource(filter: ResourceFilter) : List<String>{
        return when(filter){
            ResourceFilter.ALL -> seeInitializingActions().map { it.table.name }.plus(
                getResourceCalls().map { it.getResourceNodeKey() }
            )
            ResourceFilter.NO_SQL -> getResourceCalls().map { it.getResourceNodeKey() }
            ResourceFilter.ONLY_SQL -> seeInitializingActions().map { it.table.name }
            ResourceFilter.ONLY_SQL_EXISTING -> seeInitializingActions().filter { it.representExistingData }.map { it.table.name }
            ResourceFilter.ONLY_SQL_INSERTION -> seeInitializingActions().filterNot { it.representExistingData }.map { it.table.name }
        }
    }

    /*
        TODO Tricky... should dbInitialization somehow be part of the size?
        But they are merged in a single operation in a single call...
        need to think about it
     */

    override fun size() = seeActions().size

    override fun seeActions(): List<RestCallAction> = resourceCalls.flatMap { it.seeActions(NO_INIT) as List<RestCallAction> }

    override fun seeDbActions(): List<DbAction> {
        return seeInitializingActions().plus(resourceCalls.flatMap { c-> c.seeActions(ONLY_SQL) as List<DbAction> })
    }

    override fun verifyInitializationActions(): Boolean {
        return DbActionUtils.verifyActions(seeInitializingActions())
    }

    override fun copy(copyFilter: TraceableElementCopyFilter): RestIndividual {
        val copy = copy() as RestIndividual
        when(copyFilter){
            TraceableElementCopyFilter.NONE-> {}
            TraceableElementCopyFilter.WITH_TRACK, TraceableElementCopyFilter.DEEP_TRACK  ->{
                copy.wrapWithTracking(null, tracking!!.copy())
            }else -> throw IllegalStateException("${copyFilter.name} is not implemented!")
        }
        return copy
    }

    /**
     * During mutation, the values used for parameters are changed, but the values attached to the respective used objects are not.
     * This function copies the new (mutated) values of the parameters into the respective used objects, to ensure that the objects and parameters are coherent.
     * The return value is true if everything went well, and false if some values could not be copied. It is there for debugging only.
     */

    /*
    fun enforceCoherence(): Boolean {

        //BMR: not sure I can use flatMap here. I am using a reference to the action object to get the relevant gene.
        seeActions().forEach { action ->
            action.seeGenes().forEach { gene ->
                try {
                    val innerGene = when (gene::class) {
                        OptionalGene::class -> (gene as OptionalGene).gene
                        DisruptiveGene::class -> (gene as DisruptiveGene<*>).gene
                        else -> gene
                    }
                    val relevantGene = usedObjects.getRelevantGene((action as RestCallAction), innerGene)
                    when (action::class) {
                        RestCallAction::class -> {
                            when (relevantGene::class) {
                                OptionalGene::class -> (relevantGene as OptionalGene).gene.copyValueFrom(innerGene)
                                DisruptiveGene::class -> (relevantGene as DisruptiveGene<*>).gene.copyValueFrom(innerGene)
                                ObjectGene::class -> relevantGene.copyValueFrom(innerGene)
                                else -> relevantGene.copyValueFrom(innerGene)
                            }
                        }
                    }
                }
                catch (e: Exception){
                    // TODO BMR: EnumGene is not handled well and ends up here.
                     return false
                }
            }
        }
        return true
    }

     */


    /**
     * for each call, there exist db actions for preparing resources.
     * however, the db action might refer to a db action which is not in the same call.
     * In this case, we need to repair the fk of db actions among calls.
     *
     * TODO not sure whether build binding between fk and pk
     */
    fun repairDbActionsInCalls(){
        val previous = mutableListOf<DbAction>()
        resourceCalls.forEach { c->
            c.repairFK(previous)
            previous.addAll(c.seeActions(ONLY_SQL) as List<DbAction>)
        }
    }

    fun getResourceCalls() : List<RestResourceCalls> = resourceCalls.toList()


    /****************************** manipulate resource call in an individual *******************************************/
    /**
     * remove the resource at [position]
     */
    fun removeResourceCall(position : Int) {
        if(position >= resourceCalls.size)
            throw IllegalArgumentException("position is out of range of list")
        val removed = resourceCalls.removeAt(position)
        removed.removeThisFromItsBindingGenes()
    }

    /**
     * add [restCalls] at [position], if [position] == -1, append the [restCalls] at the end
     */
    fun addResourceCall(position: Int = -1, restCalls : RestResourceCalls) {
        if (position == -1){
            resourceCalls.add(restCalls)
        }else{
            if(position > resourceCalls.size)
                throw IllegalArgumentException("position is out of range of list")
            resourceCalls.add(position, restCalls)
        }
        addChild(restCalls)
    }

    /**
     * replace the resourceCall at [position] with [resourceCalls]
     */
    fun replaceResourceCall(position: Int, restCalls: RestResourceCalls){
        if(position > resourceCalls.size)
            throw IllegalArgumentException("position is out of range of list")

        removeResourceCall(position)
        addResourceCall(position, restCalls)
    }

    /**
     * switch the resourceCall at [position1] and the resourceCall at [position2]
     */
    fun swapResourceCall(position1: Int, position2: Int){
        if(position1 > resourceCalls.size || position2 > resourceCalls.size)
            throw IllegalArgumentException("position is out of range of list")
        if(position1 == position2)
            throw IllegalArgumentException("It is not necessary to swap two same position on the resource call list")
        val first = resourceCalls[position1]
        resourceCalls[position1] = resourceCalls[position2]
        resourceCalls[position2] = first
    }

    fun getActionIndexes(actionFilter: ActionFilter, resourcePosition: Int) = getResourceCalls()[resourcePosition].seeActions(ALL).map {
        seeActions(actionFilter).indexOf(it)
    }

    private fun validateSwap(first : Int, second : Int) : Boolean{
        val position = getResourceCalls()[first].shouldBefore.map { r ->
            getResourceCalls().indexOfFirst { it.getAResourceKey() == r }
        }

        if(!position.none { it > second }) return false

        getResourceCalls().subList(0, second).find { it.shouldBefore.contains(getResourceCalls()[second].getAResourceKey()) }?.let {
            return getResourceCalls().indexOf(it) < first
        }
        return true

    }

    /**
     * @return movable position
     */
    fun getMovablePosition(position: Int) : List<Int>{
        return (getResourceCalls().indices)
                .filter {
                    if(it < position) validateSwap(it, position) else if(it > position) validateSwap(position, it) else false
                }
    }

    /**
     * @return whether the call at the position is movable
     */
    fun isMovable(position: Int) : Boolean{
        return (getResourceCalls().indices)
                .any {
                    if(it < position) validateSwap(it, position) else if(it > position) validateSwap(position, it) else false
                }
    }

    override fun seeActions(filter: ActionFilter): List<out Action> {
        return when(filter){
            ALL-> seeInitializingActions().plus(getResourceCalls().flatMap { it.seeActions(ALL) })
            NO_INIT -> getResourceCalls().flatMap { it.seeActions(ALL) }
            INIT -> seeInitializingActions()
            ONLY_SQL -> seeInitializingActions().plus(getResourceCalls().flatMap { it.seeActions(ONLY_SQL) })
            NO_SQL -> getResourceCalls().flatMap { it.seeActions(NO_SQL) }
        }
    }


    /**
     * @return possible swap positions of calls in this individual
     */
    fun extractSwapCandidates(): Map<Int, Set<Int>>{
        return getResourceCalls().mapIndexed { index, _ ->
            val range = handleSwapCandidates(this, index)
            index to range
        }.filterNot { it.second.isEmpty() }.toMap()
    }

    private fun handleSwapCandidates(ind: RestIndividual, indexToSwap: Int): Set<Int>{
        val swapTo = handleSwapTo(ind, indexToSwap)
        return swapTo.filter { t -> handleSwapTo(ind, t).contains(indexToSwap) }.toSet()
    }

    private fun handleSwapTo(ind: RestIndividual, indexToSwap: Int): Set<Int>{
        val before =  ind.getResourceCalls()[indexToSwap].shouldBefore.map { t->
            ind.getResourceCalls().indexOfFirst { f->
                f.getResourceNodeKey() == t
            }
        }.filter { it >=0 }.minOrNull()?:ind.getResourceCalls().size

        val after = ind.getResourceCalls()[indexToSwap].depends.map { t->
            ind.getResourceCalls().indexOfFirst { f->
                f.getResourceNodeKey() == t
            }
        }.filter { it >=0 }.maxOrNull()?:0

        if (after >= before) return emptySet()
        return (after until before).filter { it != indexToSwap }.toSet()
    }
}