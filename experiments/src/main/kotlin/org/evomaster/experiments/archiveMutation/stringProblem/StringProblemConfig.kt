package org.evomaster.experiments.archiveMutation.stringProblem

import org.evomaster.core.search.service.mutator.geneMutation.CharPool

/**
 * created by manzh on 2019-09-26
 */
class StringProblemConfig(
        val numTarget : Int,
        val sLength : Int,
        val maxLength: Int ,
        val rateOfImpact : Double,
        val charPool: CharPool = CharPool.ALL
)