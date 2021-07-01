package org.evomaster.core.problem.httpws.service

import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.tracer.TrackOperator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.max

abstract class HttpWsIndividual (
    private val dbInitialization: MutableList<DbAction> = mutableListOf(),
    trackOperator: TrackOperator? = null,
    index : Int = -1,
    children: List<out StructuralElement>
): Individual(trackOperator, index, children){

    companion object{
        private val log : Logger = LoggerFactory.getLogger(HttpWsIndividual::class.java)
    }

    override fun seeInitializingActions(): List<DbAction> {
        return dbInitialization
    }

    override fun repairInitializationActions(randomness: Randomness) {

        /**
         * First repair SQL Genes (i.e. SQL Timestamps)
         */
        if (log.isTraceEnabled)
            log.trace("invoke GeneUtils.repairGenes")

        GeneUtils.repairGenes(this.seeGenes(GeneFilter.ONLY_SQL).flatMap { it.flatView() })

        /**
         * Now repair database constraints (primary keys, foreign keys, unique fields, etc.)
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

    /**
     * add [actions] at [position]
     * if [position] = -1, append the [actions] at the end
     */
    fun addInitializingActions(position: Int=-1, actions: List<DbAction>){
        if (position == -1)  dbInitialization.addAll(actions)
        else{
            dbInitialization.addAll(position, actions)
        }
        addChildren(actions)
    }

    private fun resetInitializingActions(actions: List<DbAction>){
        dbInitialization.clear()
        dbInitialization.addAll(actions)
        addChildren(actions)
    }

    fun removeAll(actions: List<DbAction>) {
        dbInitialization.removeAll(actions)
    }
}