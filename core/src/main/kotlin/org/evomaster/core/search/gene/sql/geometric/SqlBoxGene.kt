package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
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

    override fun copyContent(): Gene = SqlBoxGene(
        name,
        p.copyContent() as SqlPointGene,
        q.copyContent() as SqlPointGene
    )

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlBoxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.p.copyValueFrom(other.p)
        this.q.copyValueFrom(other.q)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlBoxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.p.containsSameValueAs(other.p)
                && this.q.containsSameValueAs(other.q)
    }

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlBoxGene -> {
                p.bindValueBasedOn(gene.p) &&
                        q.bindValueBasedOn(gene.q)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind PointGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }


}