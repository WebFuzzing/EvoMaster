package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SqlBoxGene(
    name: String,
    p: SqlPointGene = SqlPointGene("p"),
    q: SqlPointGene = SqlPointGene("q")
) : SqlAbstractGeometricGene(name, p, q) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlBoxGene::class.java)
    }


    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene = SqlBoxGene(
        name,
        p.copy() as SqlPointGene,
        q.copy() as SqlPointGene
    )

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is SqlBoxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        return updateValueOnlyIfValid(
            {this.p.unsafeCopyValueFrom(other.p) && this.q.unsafeCopyValueFrom(other.q)}, true
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlBoxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.p.containsSameValueAs(other.p)
                && this.q.containsSameValueAs(other.q)
    }

    override fun unsafeSetFromStringValue(gene: Gene): Boolean {
        return when {
            gene is SqlBoxGene -> {
                p.unsafeSetFromStringValue(gene.p) &&
                        q.unsafeSetFromStringValue(gene.q)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind PointGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

}