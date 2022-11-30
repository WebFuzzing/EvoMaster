package org.evomaster.core.search.gene.optional

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.interfaces.MergeableGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A gene that holds many potential candidates
 */

class MultipleChoicesGene<T>(
        name: String,
        private val geneChoices: List<T>,
        combinedChoices: MutableList<Int> = mutableListOf(0),
        val maximumChoices : Int = 2, //inclusive
        val minimumChoices : Int = 1 // inclusive

) : CompositeFixedGene(name, geneChoices) where T : Gene {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MultipleChoicesGene::class.java)
    }

    var combinedChoices: MutableList<Int> = combinedChoices
        private set

    val merageableMap : List<List<Int>>

    init {
        if (geneChoices.isEmpty()) {
            throw IllegalArgumentException("The list of gene choices cannot be empty")
        }

        if (combinedChoices.any { it < 0 || it >= geneChoices.size}) {
            throw IllegalArgumentException("Active index must be between 0 and ${geneChoices.size - 1}")
        }

        merageableMap = geneChoices.map { g-> if (g !is MergeableGene) listOf() else geneChoices.filter { g.isMergeableWith(it) }.map { geneChoices.indexOf(it) }}

        if (!validateChoices(combinedChoices))
            throw IllegalArgumentException("genes at ${combinedChoices.joinToString(",")} are not mergeable with each other")

        if (minimumChoices < 1 || minimumChoices > combinedChoices.size)
            throw IllegalArgumentException("minimumChoices must be between 1 and ${geneChoices.size}, but it is $minimumChoices")

        if (maximumChoices < 1 || maximumChoices > combinedChoices.size)
            throw IllegalArgumentException("maximumChoices must be between 1 and ${geneChoices.size}, but it is $maximumChoices")

        if (minimumChoices > maximumChoices)
            throw IllegalArgumentException("minimumChoices ($minimumChoices) must not be more than maximumChoices ($maximumChoices)")
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        combinedChoices.clear()
        val size = if (minimumChoices == maximumChoices) maximumChoices else randomness.nextInt(minimumChoices, maximumChoices)
        combinedChoices.addAll(randomness.choose(geneChoices, size).map { geneChoices.indexOf(it) })


        if(!initialized){
            geneChoices
                .filter { it.isMutable() }
                .forEach { it.randomize(randomness, tryToForceNewValue) }
        } else {
            combinedChoices.forEach {
                geneChoices[it].apply {
                    if (isMutable())
                        randomize(randomness, tryToForceNewValue)
                }
            }
        }
    }

    override fun mutablePhenotypeChildren(): List<Gene> {
        return combinedChoices.sorted().map { geneChoices[it] }
    }

    override fun <T> getWrappedGene(klass: Class<T>) : T?  where T : Gene{
        TODO()
    }


    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {

        if (randomness.nextBoolean() || !isCombinedSizeMutable()){
            //used next
            val replaced = combinedChoices.map { (it + 1) % geneChoices.size }
            combinedChoices.clear()
            combinedChoices.addAll(replaced)
        }else{
            if (combinedChoices.size > minimumChoices && (randomness.nextBoolean() || combinedChoices.size == maximumChoices)){
                val removed = randomness.nextInt(combinedChoices.size)
                combinedChoices.removeAt(removed)
            }else{
                val added = randomness.nextInt(geneChoices.size - combinedChoices.size)
                combinedChoices.add((geneChoices.indices).filter { combinedChoices.contains(it) }[added])
            }
        }
        return true
    }

    private fun isCombinedSizeMutable() = minimumChoices < maximumChoices

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return randomness.nextBoolean(0.1)
    }



    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        TODO()
    }


    override fun getValueAsRawString(): String {
        TODO()
    }


    override fun copyValueFrom(other: Gene) {
        if (other !is MultipleChoicesGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        } else if (geneChoices.size != other.geneChoices.size) {
            throw IllegalArgumentException("Cannot copy value from another choice gene with  ${other.geneChoices.size} choices (current gene has ${geneChoices.size} choices)")
        } else if (geneChoices.first()::class.java == other.geneChoices.first()::class.java){
            throw IllegalArgumentException("cannot copy value from another choice gene which have different types of gene (${geneChoices.first()::class.java.name} vs. ${other.geneChoices.first()::class.java.name})")
        }else {
            combinedChoices.clear()
            combinedChoices.addAll(other.combinedChoices)
            for (i in geneChoices.indices) {
                this.geneChoices[i].copyValueFrom(other.geneChoices[i])
            }
        }
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is MultipleChoicesGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        if (this.combinedChoices.size == other.combinedChoices.size && this.combinedChoices.containsAll(other.combinedChoices)) {
            return false
        }

        return combinedChoices.all {
            this.geneChoices[it]
                    .containsSameValueAs(other.geneChoices[it])
        }
    }


    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene !is MultipleChoicesGene<*>) return false

    }


    override fun copyContent(): Gene = MultipleChoicesGene(
        name,this.geneChoices.map { it.copy() }.toList(), combinedChoices, maximumChoices, minimumChoices
    )


    override fun isLocallyValid() = geneChoices.all { it.isLocallyValid() }

    // all selected genes should be printable
    override fun isPrintable() = combinedChoices.all { geneChoices[it].isPrintable() }

    private fun validateChoices(indexes : List<Int>) : Boolean{
        if (indexes.size == 1) return true
        if (indexes.any {  geneChoices[it] !is MergeableGene }) return false
        return indexes.all { x-> merageableMap[x].containsAll(indexes.filter { it != x }) }
    }

}