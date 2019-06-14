package org.evomaster.core.problem.graphql

import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.rest.*
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.tracer.TraceableElement
import org.evomaster.core.search.tracer.TrackOperator

class GraphqlIndividual (
        val actions: MutableList<GraphqlAction>,
        val sampleType: GraphqlSampleType,
        val dbInitialization: MutableList<DbAction> = mutableListOf(),
        trackOperator: TrackOperator? = null,
        traces : MutableList<out GraphqlIndividual>? = null
): Individual(trackOperator, traces) {

    override fun copy(): Individual {
        return GraphqlIndividual(
                actions.map { a -> a.copy() as GraphqlAction } as MutableList<GraphqlAction>,
                sampleType,
                dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>,
                trackOperator
        )
    }

    override fun canMutateStructure(): Boolean {
        return sampleType == SampleType.RANDOM
    }
    override fun seeGenes(filter: GeneFilter): List<out Gene> {

        return when (filter) {
            GeneFilter.ALL -> dbInitialization.flatMap(DbAction::seeGenes).plus(actions.flatMap(GraphqlAction::seeGenes))
            GeneFilter.NO_SQL -> actions.flatMap(GraphqlAction::seeGenes)
            GeneFilter.ONLY_SQL -> dbInitialization.flatMap(DbAction::seeGenes)

        }
    }

    override fun size() = actions.size

    override fun seeActions(): List<out Action> {
        return actions
    }

    override fun seeInitializingActions(): List<Action> {
        return dbInitialization
    }

    override fun verifyInitializationActions(): Boolean {
        //TODO needs to be refactored when DB code moved to its own module
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

    override fun next(trackOperator: TrackOperator) : TraceableElement?{
        getTrack()?:return GraphqlIndividual(
                actions.map { a -> a.copy() as GraphqlAction } as MutableList<GraphqlAction>,
                sampleType,
                dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>,
                trackOperator)
        return GraphqlIndividual(
                actions.map { a -> a.copy() as GraphqlAction } as MutableList<GraphqlAction>,
                sampleType,
                dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>,
                trackOperator,
                getTrack()!!.plus(this).map { (it as GraphqlIndividual).copy() as GraphqlIndividual }.toMutableList()
        )
    }
    override fun copy(withTrack: Boolean): GraphqlIndividual {
        when(withTrack){
            false-> return copy() as GraphqlIndividual
            else ->{
                getTrack()?:return copy() as GraphqlIndividual
                return GraphqlIndividual(
                        actions.map { a -> a.copy() as GraphqlAction } as MutableList<GraphqlAction>,
                        sampleType,
                        dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>,
                        trackOperator!!,
                        getTrack()!!.map { (it as GraphqlIndividual).copy() as GraphqlIndividual }.toMutableList()
                )
            }
        }
    }

}