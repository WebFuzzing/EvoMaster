package org.evomaster.core.problem.rpc

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.httpws.service.HttpWsIndividual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.tracer.TrackOperator

/**
 * individual for RPC service
 */
class RPCIndividual(
        /**
         * actions of the individual
         */
        val actions: MutableList<RPCCallAction>,
        /*
            TODO might add sample type here as REST (check later)
         */
        dbInitialization: MutableList<DbAction> = mutableListOf(),
        trackOperator: TrackOperator? = null,
        index : Int = -1
) : HttpWsIndividual(dbInitialization, trackOperator, index, actions) {

    override fun seeGenes(filter: GeneFilter): List<out Gene> {
        TODO("Not yet implemented")
    }

    override fun size(): Int {
        TODO("Not yet implemented")
    }

    override fun seeActions(): List<RPCCallAction> {
        TODO("Not yet implemented")
    }

    override fun verifyInitializationActions(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getChildren(): List<out StructuralElement> {
        TODO("Not yet implemented")
    }
}