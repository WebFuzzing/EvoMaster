package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness


/**
 * A building block representing one part of an Individual.
 * The terms "gene" comes from the evolutionary algorithm literature
 */
abstract class Gene(var name: String) {

    init{
        if(name.isBlank()){
            throw IllegalArgumentException("Empty name for Gene")
        }
    }

    /**
     *  A gene could be inside a gene, in a tree-like structure.
     *  So for each gene, but the root, we keep track of its parent.
     *
     *  When a gene X is created with a child Y, then X is responsible
     *  to mark itself as parent of Y
     */
    var parent : Gene? = null

    /**
     * Follow the parent's path until the root of gene tree,
     * which could be this same gene
     */
    fun getRoot() : Gene{
        var curr = this
        while(curr.parent != null){
            curr = curr.parent!!
        }
        return curr
    }

    /**
     * Make a copy of this gene.
     *
     * Note: the [parent] of this gene will be [null], but all children
     * will have the correct parent
     */
    abstract fun copy() : Gene

    /**
     * Specify if this gene can be mutated during the search.
     * Typically, it will be true, apart from some special cases.
     */
    open fun isMutable() = true

    /**
     * Specify if this gene should be printed in the output test.
     * In other words, if this genotype directly influences the
     * phenotype
     */
    open fun isPrintable() = true


    /**
     *   Randomize the content of this gene.
     *
     *   @param randomness the source of non-determinism
     *   @param forceNewValue whether we should force the change of value. When we do mutation,
     *          it could otherwise happen that a value is replace with itself
     *   @param allGenes if the gene depends on the other (eg a Foreign Key in SQL databases),
     *          we need to refer to them
     */
    abstract fun randomize(
            randomness: Randomness,
            forceNewValue: Boolean,
            allGenes: List<Gene> = listOf())

    /**
     * Apply a mutation to the current gene.
     * A mutation is just a small change.
     *
     *   @param randomness the source of non-determinism
     *   @param allGenes if the gene depends on the other (eg a Foreign Key in SQL databases),
     *          we need to refer to them
     */
    abstract fun standardMutation(
            randomness: Randomness,
            apc: AdaptiveParameterControl,
            allGenes: List<Gene> = listOf()
    )

    /**
     * Apply a archived-based mutation to the current gene.
     *
     * NOTE THAT if this method is not overridden, just default to standard mutation
     *
     *   @param randomness the source of non-determinism
     *   @param allGenes if the gene depends on the other (eg a Foreign Key in SQL databases),
     *          we need to refer to them
     *   @param evi the evaluated individual contains an evolution of the gene with fitness values
     *   @param selection how to select genes to mutate if [this] contains more than one genes(e.g., ObjectGene) or other characteristics(e.g., size of ArrayGene)
     *   @param impact info of impact of the gene if it has, but in some case impact might be null, e.g., an element at ArrayGene
     *   @param geneReference a reference (i.e., id generated) to find a gene in this history, which always refers to 'root' gene in the [evi]
     *   @param archiveMutator mutate genes using archive-based methods if the method is enabled or supports this type of [this] gene.
     */
    open fun archiveMutation(randomness: Randomness,
                             allGenes: List<Gene>,
                             apc: AdaptiveParameterControl,
                             selection: GeneMutationSelectionMethod,
                             impact: GeneImpact?,
                             geneReference: String,
                             archiveMutator: ArchiveMutator,
                             evi: EvaluatedIndividual<*>,
                             targets: Set<Int>){
        standardMutation(randomness, apc, allGenes)
    }

    /**
     * Return the value as a printable string.
     * Once printed, it would be equivalent to the actual value, eg
     *
     * 1 -> "1" -> printed as 1
     *
     * "foo" -> "\"foo\"" -> printed as "foo"
     *
     * @param previousGenes previous genes which are necessary to look at
     * to determine the actual value of this gene
     * @param mode some genes could be printed in different ways, like an
     * object printed as JSON or XML
     * @param targetFormat different target formats may have different rules
     * regarding what characters need to be escaped (e.g. the $ char in Kotlin)
     * If the [targetFormat] is set to null, no characters are escaped.
     */
    abstract fun getValueAsPrintableString(
            previousGenes: List<Gene> = listOf(),
            mode: GeneUtils.EscapeMode? = null,
            targetFormat: OutputFormat? = null
    ) : String


    open fun getValueAsRawString() = getValueAsPrintableString(targetFormat = null)
    /*
    Note: above, null target format means that no characters are escaped.
     */

    abstract fun copyValueFrom(other: Gene)

    /**
     * If this gene represents a variable, then return its name.
     */
    open fun getVariableName() = name

    /**
     * Genes might have other genes inside (eg, think of array).
     * @param excludePredicate is used to configure which genes you do not want to show genes inside.
     *      For instance, an excludePredicate is {gene : Gene -> (gene is TimeGene)}, then when flatView of a Gene including TimeGene,
     *      the genes inside e.g., hour: IntegerGene will be not viewed, but TimeGene will be viewed.
     * @return a recursive list of all nested genes, "this" included
     */
    open fun flatView(excludePredicate: (Gene) -> Boolean = {false}): List<Gene>{
        return listOf(this)
    }

    /**
     * Genes might contain a value that is also stored
     * in another gene of the same type.
     */
    abstract fun containsSameValueAs(other: Gene): Boolean

    /**
     * indicates if it is likely that the gene reaches its optimal value, i.e., all possible values have been evaluated during search in the context of its individual.
     * For instance, an enum has four items. If all values evaluated used during search, its 'Optimal' may be identified. But there may exist dependency among the genes
     * in an individual, 'Optimal' can be reset.
     */
    open fun reachOptimal() = false

    /**
     * based on evaluated results, update a preferred boundary for the gene
     */
    open fun archiveMutationUpdate(original: Gene, mutated: Gene, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator){
        //do nothing
    }
}