package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SqlLineGene(
    name: String,
    p: SqlPointGene = SqlPointGene(
        "p",
        x = FloatGene("x", value = 0.0f),
        y = FloatGene("y", value = 0.0f)
    ),
    q: SqlPointGene = SqlPointGene(
        "q",
        x = FloatGene("x", value = 1.0f),
        y = FloatGene("y", value = 1.0f)
    )
) : SqlAbstractGeometricGene(name, p, q) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlLineGene::class.java)
    }


    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene = SqlLineGene(
        name,
        p.copy() as SqlPointGene,
        q.copy() as SqlPointGene
    )

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlLineGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            {this.p.copyValueFrom(other.p)
                    && this.q.copyValueFrom(other.q)}, true
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlLineGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.p.containsSameValueAs(other.p)
                && this.q.containsSameValueAs(other.q)
    }

    override fun setValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlLineGene -> {
                p.setValueBasedOn(gene.p) &&
                        q.setValueBasedOn(gene.q)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind PointGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        super.randomize(randomness, tryToForceNewValue)
        /*
         * Lines cannot contain the same p,q points
         */
        if (p.x.equals(q.x) && p.y.equals(q.y)) {
            p.x.value = p.x.value + 1
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