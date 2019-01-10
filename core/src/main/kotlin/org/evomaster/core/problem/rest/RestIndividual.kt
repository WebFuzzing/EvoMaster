package org.evomaster.core.problem.rest

import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.tracer.TraceableElement



open class RestIndividual : Individual {

    val actions: MutableList<RestAction>
    val sampleType: SampleType
    val dbInitialization: MutableList<DbAction>

    constructor(actions: MutableList<RestAction>, sampleType: SampleType, dbInitialization: MutableList<DbAction>, description: String, traces : MutableList<out RestIndividual>) : super(description, traces){
        this.actions = actions
        this.sampleType = sampleType
        this.dbInitialization = dbInitialization
    }

    constructor(actions: MutableList<RestAction>, sampleType: SampleType, description: String) :   this(actions, sampleType, mutableListOf(), description, mutableListOf())
    constructor(actions: MutableList<RestAction>, sampleType: SampleType, dbInitialization: MutableList<DbAction> = mutableListOf()) : this(actions, sampleType, dbInitialization, sampleType.toString(), mutableListOf())
    constructor(actions: MutableList<RestAction>, sampleType: SampleType, description: String, traces : MutableList<out RestIndividual>) : this(actions, sampleType, mutableListOf<DbAction>(), description, traces)


    override fun copy(): Individual {
        return RestIndividual(
                actions.map { a -> a.copy() as RestAction } as MutableList<RestAction>,
                sampleType,
                dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>
        )
    }

    override fun canMutateStructure(): Boolean {
        return sampleType == SampleType.RANDOM ||
                sampleType == SampleType.SMART_GET_COLLECTION
    }


    override fun seeGenes(filter: GeneFilter): List<out Gene> {

        return when (filter) {
            GeneFilter.ALL -> dbInitialization.flatMap(DbAction::seeGenes)
                    .plus(actions.flatMap(RestAction::seeGenes))

            GeneFilter.NO_SQL -> actions.flatMap(RestAction::seeGenes)
            GeneFilter.ONLY_SQL -> dbInitialization.flatMap(DbAction::seeGenes)
        }
    }

    /*
        TODO Tricky... should dbInitialization somehow be part of the size?
        But they are merged in a single operation in a single call...
        need to think about it
     */

    override fun size() = actions.size

    override fun seeActions(): List<out Action> {
        return actions
    }

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
        GeneUtils.repairGenes(this.seeGenes(Individual.GeneFilter.ONLY_SQL).flatMap { it.flatView() })

        /**
         * Now repair database constraints (primary keys, foreign keys, unique fields, etc.)
         */
        if (!verifyInitializationActions()) {
            DbActionUtils.repairBrokenDbActionsList(dbInitialization, randomness)
            Lazy.assert{verifyInitializationActions()}
        }
    }

    override fun next(description : String) : TraceableElement?{
        if(isCapableOfTracking()){
            val copyTraces = mutableListOf<RestIndividual>()
            if(!isRoot()){
                val size = getTrack()?.size?:0
                (0 until if(maxlength != -1 && size > maxlength - 1) maxlength-1  else size).forEach {
                    copyTraces.add(0, (getTrack()!![size-1-it] as RestIndividual).copy() as RestIndividual)
                }
            }
            copyTraces.add(this)
            return RestIndividual(
                    actions.map { a -> a.copy() as RestAction } as MutableList<RestAction>,
                    sampleType,
                    dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>,
                    description,
                    copyTraces)
        }
        return copy()
    }

    override fun copy(withTrack: Boolean): RestIndividual {
        when(withTrack){
            false-> return copy() as RestIndividual
            else ->{
                getTrack()?:return copy() as RestIndividual
                val copyTraces = mutableListOf<RestIndividual>()
                getTrack()?.forEach {
                    copyTraces.add((it as RestIndividual).copy() as RestIndividual)
                }
                return RestIndividual(
                        actions.map { a -> a.copy() as RestAction } as MutableList<RestAction>,
                        sampleType,
                        dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>,
                        getDescription(),
                        copyTraces
                )
            }
        }
    }

}