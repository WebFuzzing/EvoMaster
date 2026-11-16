package org.evomaster.core.search.gene.utils

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.AssertionRxGene
import org.evomaster.core.search.gene.regex.DisjunctionListRxGene
import org.evomaster.core.search.gene.regex.DisjunctionRxGene
import org.evomaster.core.search.gene.regex.PatternCharacterBlockGene
import org.evomaster.core.search.gene.regex.QuantifierRxGene
import org.evomaster.core.search.gene.regex.RxTerm

object RxGeneUtils {

    /**
     * Like [Gene.flatView], but also includes the genes under the template of each [QuantifierRxGene],
     * which is not among the quantifier's children (so a quantifier with no atoms looks empty).
     * Only regex genes are handled (hence the [RxTerm] bound), templates of other kinds of genes are not included.
     *
     * The atoms of a quantifier are copies of its template, so what is in the template can appear more than once.
     * @see Gene.flatView
     */
    fun <T> flatViewWithQuantifierTemplates(gene: T): List<Gene> where T : Gene, T : RxTerm =
        expand(gene)

    /**
     * The numbers of the capture groups that are in [gene], or that are [gene] itself, including the ones inside the
     * templates of quantifiers (see [flatViewWithQuantifierTemplates]). Empty if there are none.
     */
    fun <T> captureGroupIndexesIn(gene: T): Set<Int> where T : Gene, T : RxTerm =
        flatViewWithQuantifierTemplates(gene)
            .mapNotNull { (it as? DisjunctionListRxGene)?.captureGroupIndex }
            .toSet()

    /**
     * Whether [gene] can always match the empty string regardless of what is around it. Unlike [RxTerm.canBeZeroWidth],
     * this can be false for assertions. For use in detecting unsatisfiable negative lookarounds, like "(?!a*)".
     */
    fun canAlwaysMatchEmpty(gene: Gene): Boolean = when (gene) {
        is DisjunctionListRxGene -> gene.disjunctions.any { canAlwaysMatchEmpty(it) }
        is DisjunctionRxGene -> gene.terms.all { canAlwaysMatchEmpty(it) }
        is QuantifierRxGene -> gene.min == 0 || canAlwaysMatchEmpty(gene.template)
        is AssertionRxGene -> gene.innerGene?.let { canAlwaysMatchEmpty(it) } ?: false
        is PatternCharacterBlockGene -> gene.canBeZeroWidth
        else -> false
    }

    private fun expand(gene: Gene): List<Gene> =
        gene.flatView().flatMap { g ->
            if (g is QuantifierRxGene) listOf(g) + expand(g.template) else listOf(g)
        }
}
