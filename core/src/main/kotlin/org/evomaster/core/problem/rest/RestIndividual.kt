package org.evomaster.core.problem.rest

import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.resource.SamplerSpecification
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.tracer.TraceableElementCopyFilter
import org.evomaster.core.search.tracer.TrackOperator


class RestIndividual(
        //val actions: MutableList<RestAction>,
        private val resourceCalls: MutableList<RestResourceCalls>,
        val sampleType: SampleType,
        val sampleSpec: SamplerSpecification? = null,
        val dbInitialization: MutableList<DbAction> = mutableListOf(),
        trackOperator: TrackOperator? = null,
        traces: MutableList<out RestIndividual>? = null
): Individual (trackOperator, traces) {

    constructor(
            actions: MutableList<out Action>,
            sampleType: SampleType,
            dbInitialization: MutableList<DbAction> = mutableListOf(),
            //usedObjects: UsedObjects = UsedObjects(),
            trackOperator: TrackOperator? = null,
            traces: MutableList<out RestIndividual>? = null) :
            this(
                    actions.map { RestResourceCalls(actions= mutableListOf(it as RestAction)) }.toMutableList(),
                    sampleType,
                    null,
                    dbInitialization,
                    trackOperator,
                    traces
            )


    override fun copy(): Individual {
        return RestIndividual(
                resourceCalls.map { it.copy() }.toMutableList(),
                sampleType,
                sampleSpec?.copy(),
                dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>
        )
    }

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
            GeneFilter.ALL -> dbInitialization.flatMap(DbAction::seeGenes).plus(seeActions().flatMap(Action::seeGenes))
            GeneFilter.NO_SQL -> seeActions().flatMap(Action::seeGenes)
            GeneFilter.ONLY_SQL -> dbInitialization.flatMap(DbAction::seeGenes)
        }
    }

    /*
        TODO Tricky... should dbInitialization somehow be part of the size?
        But they are merged in a single operation in a single call...
        need to think about it
     */

    override fun size() = seeActions().size

    override fun seeActions(): List<RestAction> = resourceCalls.flatMap { it.actions }

    override fun seeInitializingActions(): List<Action> {
        return dbInitialization
    }

    override fun verifyInitializationActions(): Boolean {
        return DbActionUtils.verifyActions(dbInitialization.filterIsInstance<DbAction>())
    }


    override fun repairInitializationActions(randomness: Randomness) {

        /**
         * First repair SQL Genes (i.e. SQL Timestamps)
         */
        GeneUtils.repairGenes(this.seeGenes(GeneFilter.ONLY_SQL).flatMap { it.flatView() })

        /**
         * Now repair database constraints (primary keys, foreign keys, unique fields, etc.)
         */
        if (!verifyInitializationActions()) {
            DbActionUtils.repairBrokenDbActionsList(dbInitialization, randomness)
            Lazy.assert{verifyInitializationActions()}
        }
    }

    override fun next(trackOperator: TrackOperator, maxLength : Int) : RestIndividual?{
        return RestIndividual(
                resourceCalls.map { it.copy() }.toMutableList(),
                sampleType,
                sampleSpec?.copy(),
                dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>,
                trackOperator,
                if (getTracking() == null)
                    mutableListOf()
                else if (getTracking()!!.size == maxLength) {
                    getTracking()!!.subList(1, maxLength).plus(this).map { (it as RestIndividual).copy() as RestIndividual }.toMutableList()
                }else
                    getTracking()!!.plus(this).map { (it as RestIndividual).copy() as RestIndividual }.toMutableList()
        )
    }

    override fun copy(copyFilter: TraceableElementCopyFilter): RestIndividual {
        return when(copyFilter){
            TraceableElementCopyFilter.NONE-> copy() as RestIndividual
            TraceableElementCopyFilter.WITH_TRACK, TraceableElementCopyFilter.DEEP_TRACK  ->{
                getTracking()?: throw IllegalStateException("the individual is initialized or invoked incorrectly regarding tracking")
                RestIndividual(
                        resourceCalls.map { it.copy() }.toMutableList(),
                        sampleType,
                        sampleSpec?.copy(),
                        dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>,
                        trackOperator,
                        getTracking()!!.map { (it as RestIndividual).copy() as RestIndividual }.toMutableList()
                )
            }else -> throw IllegalStateException("${copyFilter.name} is not implemented!")
        }
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



    fun getResourceCalls() : List<RestResourceCalls> = resourceCalls.toList()

    /****************************** manipulate resource call in an individual *******************************************/
    fun removeResourceCall(position : Int) {
        if(position >= resourceCalls.size)
            throw IllegalArgumentException("position is out of range of list")
        resourceCalls.removeAt(position)
    }

    fun addResourceCall(position: Int, restCalls : RestResourceCalls) {
        if(position > resourceCalls.size)
            throw IllegalArgumentException("position is out of range of list")
        resourceCalls.add(position, restCalls)
    }

    fun addResourceCall(restCalls : RestResourceCalls) {
        resourceCalls.add(restCalls)
    }

    fun replaceResourceCall(position: Int, restCalls: RestResourceCalls){
        if(position > resourceCalls.size)
            throw IllegalArgumentException("position is out of range of list")
        resourceCalls.set(position, restCalls)
    }

    fun swapResourceCall(position1: Int, position2: Int){
        if(position1 > resourceCalls.size || position2 > resourceCalls.size)
            throw IllegalArgumentException("position is out of range of list")
        if(position1 == position2)
            throw IllegalArgumentException("It is not necessary to swap two same position on the resource call list")
        val first = resourceCalls[position1]
        resourceCalls.set(position1, resourceCalls[position2])
        resourceCalls.set(position2, first)
    }


    fun repairDBActions(sqlInsertBuilder: SqlInsertBuilder?){
        val previousDbActions = mutableListOf<DbAction>()

        getResourceCalls().filter { it.dbActions.isNotEmpty() }.forEach {
            var result = try{
                DbActionUtils.verifyActions(it.dbActions) && DbActionUtils.verifyActions(previousDbActions.plus(it.dbActions))
            }catch (e : IllegalArgumentException ){false}

            if(!result){
                val created = mutableListOf<DbAction>()
                it.dbActions.forEach { db->
                    DbActionUtils.repairFK(db, previousDbActions, created, sqlInsertBuilder)
                    previousDbActions.add(db)
                }
                it.dbActions.addAll(0, created)

            }else{
                previousDbActions.addAll(it.dbActions)
            }
        }

        if(!DbActionUtils.verifyForeignKeys(previousDbActions))
            throw IllegalStateException("referred fk cannot be found!")
    }

    private fun validateSwap(first : Int, second : Int) : Boolean{
        val position = getResourceCalls()[first].shouldBefore.map { r ->
            getResourceCalls().indexOfFirst { it.resourceInstance?.getAResourceKey() == r }
        }

        if(!position.none { it > second }) return false

        getResourceCalls().subList(0, second).find { it.shouldBefore.contains(getResourceCalls()[second].resourceInstance?.getAResourceKey()) }?.let {
            return getResourceCalls().indexOf(it) < first
        }
        return true

    }

    /**
     * @return movable position
     */
    fun getMovablePosition(position: Int) : List<Int>{
        return (0..(getResourceCalls().size-1))
                .filter {
                    if(it < position) validateSwap(it, position) else if(it > position) validateSwap(position, it) else false
                }
    }

    /**
     * @return whether the call at the position is movable
     */
    fun isMovable(position: Int) : Boolean{
        return (0..(getResourceCalls().size-1))
                .any {
                    if(it < position) validateSwap(it, position) else if(it > position) validateSwap(position, it) else false
                }
    }

    /**
     *  Return the distinct auth info on cookie-based login in all actions
     *  of this individual
     */
    fun getCookieLoginAuth() =  this.seeActions()
            .filterIsInstance<RestCallAction>()
            .filter { it.auth.cookieLogin != null }
            .map { it.auth.cookieLogin!! }
            .distinctBy { it.username }

}