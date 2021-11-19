package org.evomaster.core.search.gene.geometric

import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LineGene(
    name: String,
    p: PointGene = PointGene(
        "p",
        x = FloatGene("x", value = 0.0f),
        y = FloatGene("y", value = 0.0f)
    ),
    q: PointGene = PointGene(
        "q",
        x = FloatGene("x", value = 1.0f),
        y = FloatGene("y", value = 1.0f)
    )
) : PQGeometricAbstractGene(name, p, q) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(LineGene::class.java)
    }

    override fun copyContent(): Gene = LineGene(
        name,
        p.copyContent() as PointGene,
        q.copyContent() as PointGene
    )

    override fun copyValueFrom(other: Gene) {
        if (other !is LineGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.p.copyValueFrom(other.p)
        this.q.copyValueFrom(other.q)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is LineGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.p.containsSameValueAs(other.p)
                && this.q.containsSameValueAs(other.q)
    }

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is LineGene -> {
                p.bindValueBasedOn(gene.p) &&
                        q.bindValueBasedOn(gene.q)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind PointGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        super.randomize(randomness, forceNewValue, allGenes)
        /*
         * Lines cannot contain the same p,q points
         */
        if (p.x.equals(q.x) && p.y.equals(q.y)) {
            p.x.value = p.x.value + 1
        }
    }
}