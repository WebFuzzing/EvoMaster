package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.core.search.gene.*
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness

abstract class SqlAbstractGeometricGene(
    name: String,
    val p: SqlPointGene,
    val q: SqlPointGene
) : CompositeFixedGene(name, mutableListOf(p, q)) {



    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        p.randomize(randomness, tryToForceNewValue)
        q.randomize(randomness, tryToForceNewValue)
    }


    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return "\"${getValueAsRawString()}\""
    }

    override fun getValueAsRawString(): String {
        return "(${p.getValueAsRawString()}, ${q.getValueAsRawString()})"
    }


}