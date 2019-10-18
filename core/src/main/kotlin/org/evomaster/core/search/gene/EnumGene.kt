package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import org.evomaster.core.search.impact.value.collection.EnumGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.evomaster.core.search.service.mutator.geneMutation.IntMutationUpdate

/**
 * Gene in which 1 out of N constant values is chosen.
 *
 * Not only the type in an enumeration must be sortable, but also
 * must be immutable.
 * This  is fine for String and numeric values.
 */
class EnumGene<T : Comparable<T>>(
        name: String,
        data: Collection<T>,
        var index: Int = 0
) : Gene(name) {

    companion object {

        /**
         * WARNING: mutable static state. But as it is just a cache, it is not a problem.
         * Furthermore, although the set is mutable, the lists inside are not (more specifically,
         * they are read-only copies).
         */
         private val cache : MutableSet<List<*>> = mutableSetOf()
    }

    val values : List<T>

    init {

        if (data.isEmpty()) {
            throw IllegalArgumentException("Empty list of values")
        }

        val list = data
                .toSet() // we want no duplicate
                .toList() // need ordering to specify index of selection, so Set would not do
                .sorted() // sort, to make meaningful list comparisons
                .map { if(it is String) it.intern() as T else it} //if strings, make sure to intern them

        /*
           we need to make sure that, if we are adding a list that has content equal to
           an already present list in the cache, we only use this latter
         */
        values = if(cache.contains(list)){
            cache.find { it == list }!! as List<T> // equality based on content, not reference
        } else {
            cache.add(list)
            list
        }

        if (index < 0 || index >= values.size) {
            throw IllegalArgumentException("Invalid index: $index")
        }
    }

    override fun isMutable(): Boolean {
        return values.size > 1
    }

    override fun copy(): Gene {
        //recall: "values" is immutable
        return EnumGene<T>(name, values, index)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        val k = if (forceNewValue) {
            randomness.nextInt(0, values.size - 1, index)
        } else {
            randomness.nextInt(0, values.size - 1)
        }

        index = k
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {

        val next = (index+1) % values.size
        index = next
    }

    override fun archiveMutation(randomness: Randomness, allGenes: List<Gene>, apc: AdaptiveParameterControl, selection: GeneMutationSelectionMethod, impact: GeneImpact?, geneReference: String, archiveMutator: ArchiveMutator, evi: EvaluatedIndividual<*>, targets: Set<Int>) {
        if (!archiveMutator.applyArchiveSelection() || values.size == 2 || impact == null || impact !is EnumGeneImpact){
            standardMutation(randomness, apc, allGenes)
            return
        }

        val candidates = (0 until values.size).filter { index != it }.map {
            Pair(it, impact.values[it])
        }

        val selects = archiveMutator.selectGenesByArchive(candidates, 1.0/(values.size - 1), targets)
        index = randomness.choose(selects)

    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {

        val res = values[index]
        if(res is String){
            return "\"$res\""
        } else {
            return res.toString()
        }
    }

    override fun getValueAsRawString(): String {
        return values[index].toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is EnumGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.index = other.index
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is EnumGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.index == other.index
    }

    //TODO when archive-based mutation is enabled
    override fun reachOptimal(): Boolean {
        return values.size == 1
    }
}