package org.evomaster.core.search.impact

import org.evomaster.core.search.gene.Gene

/**
 * created by manzh on 2019-09-09
 *
 */
class MutatedGeneWithContext (
        val current : Gene,
        val action : String,
        val position : Int,
        val previous : Gene
)