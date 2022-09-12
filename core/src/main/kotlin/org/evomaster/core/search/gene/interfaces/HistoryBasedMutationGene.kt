package org.evomaster.core.search.gene.interfaces

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.DifferentGeneInHistory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A gene implementing this interface must handle history based mutations.
 * Not all genes support it
 */
interface HistoryBasedMutationGene {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Gene::class.java)
    }


    fun applyHistoryBasedMutation(
        gene: Gene, //| HistoryBasedMutation   //FIXME remove this input
        additionalGeneMutationInfo: AdditionalGeneMutationInfo
    ): Boolean {

        if (additionalGeneMutationInfo.hasHistory()) {
            try {
                additionalGeneMutationInfo.archiveGeneMutator.historyBasedValueMutation(
                    additionalGeneMutationInfo,
                    gene,
                    gene.getAllGenesInIndividual() //FIXME
                )
                return true
            } catch (e: DifferentGeneInHistory) {
                LoggingUtil.uniqueWarn(
                    log,
                    e.message ?: "Fail to employ adaptive gene value mutation due to failure in handling its history"
                )
            }
        }


        return false
    }
}