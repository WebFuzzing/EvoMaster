package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.DifferentGeneInHistory
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * A gene implementing this interface must handle history based mutations.
 * Not all genes support it
 */
interface HistoryBasedMutation {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Gene::class.java)
    }


    fun applyHistoryBasedMutation(
        gene: Gene, //| HistoryBasedMutation
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