package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness


class DisjunctionListRxGene(
        val disjunctions: List<DisjunctionRxGene>
) : RxAtom("disjunction_list") {

    var activeDisjunction: Int = 0

    override fun copy(): Gene {
        val copy = DisjunctionListRxGene(disjunctions.map { it.copy() as DisjunctionRxGene })
        copy.activeDisjunction = this.activeDisjunction
        return copy
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        /*
            two complementary options: choose a different disjunction,
            and/or randomize content of such disjunction
         */

        if (disjunctions.size > 1) {
            activeDisjunction = randomness.nextInt(0, disjunctions.size-1)
        }

        disjunctions[activeDisjunction].randomize(randomness, forceNewValue, allGenes)
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {
        if(disjunctions.size > 1 && randomness.nextBoolean(0.1)){
            //activate the next disjunction
            activeDisjunction = (activeDisjunction + 1) % disjunctions.size
        } else {
            disjunctions[activeDisjunction].standardMutation(randomness, apc, allGenes)
        }

    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
        if (disjunctions.isEmpty()) {
            return ""
        }
        return disjunctions[activeDisjunction]
                .getValueAsPrintableString(previousGenes, mode, targetFormat)
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is DisjunctionListRxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        this.activeDisjunction = other.activeDisjunction
        for (i in 0 until disjunctions.size) {
            this.disjunctions[i].copyValueFrom(other.disjunctions[i])
        }
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is DisjunctionListRxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        if (this.activeDisjunction != other.activeDisjunction) {
            return false
        }

        return this.disjunctions[activeDisjunction]
                .containsSameValueAs(other.disjunctions[activeDisjunction])
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this)
        else listOf(this).plus(disjunctions.flatMap { it.flatView(excludePredicate) })
    }
}