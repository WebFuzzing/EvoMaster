package org.evomaster.core.search.service.mutator.genemutation

import org.evomaster.core.search.gene.Gene

/**
 * created by manzh on 2020-09-16
 */
class DifferentGeneInHistory(gene: Gene, geneInHistory: Gene)
    : IllegalArgumentException("History issue: gene is ${gene::class.java.simpleName} but its history was ${geneInHistory::class.java.simpleName}")