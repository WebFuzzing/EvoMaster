package org.evomaster.core.search.gene.collection

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.value.collection.EnumGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
    var index: Int = 0,
    /**
     * At times, to simplify things, we might use strings to represent other types (eg numbers),
     * to avoid specifying exact types. Still, should not be printed out as string.
     * Recall that an enum is just a group of constants that cannot be mutated
     */
    private val treatAsNotString : Boolean = false
) : SimpleGene(name) {

    companion object {

        /**
         * WARNING: mutable static state. But as it is just a cache, it is not a problem.
         * Furthermore, although the set is mutable, the lists inside are not (more specifically,
         * they are read-only copies).
         */
        private val cache: MutableSet<List<*>> = mutableSetOf()

        private val log: Logger = LoggerFactory.getLogger(EnumGene::class.java)

    }

    val values: List<T>

    init {

        if (data.isEmpty()) {
            /*
                in industrial case study, the Enum could have empty values,
                then provide warn info instead of throwing exception
             */
            //throw IllegalArgumentException("Empty list of values")
            log.warn("Enum Gene (name: $name) has empty list of values")
            values = listOf()
        }else{
            val list = data
                .toSet() // we want no duplicate
                .toList() // need ordering to specify index of selection, so Set would not do
                .sorted() // sort, to make meaningful list comparisons
                .map { if (it is String) it.intern() as T else it } //if strings, make sure to intern them

            /*
               we need to make sure that, if we are adding a list that has content equal to
               an already present list in the cache, we only use this latter
             */
            synchronized(cache) {
                values = if (cache.contains(list)) {
                    cache.find { it == list }!! as List<T> // equality based on content, not reference
                } else {
                    cache.add(list)
                    list
                }
            }

            if (index < 0 || index >= values.size) {
                throw IllegalArgumentException("Invalid index: $index")
            }
        }

        if(treatAsNotString && values.isNotEmpty() && values[0] !is String){
            throw IllegalArgumentException("Enum values should had been of type String")
        }
    }

    override fun isLocallyValid() : Boolean{
        return (index >= 0 && index < values.size) || values.isEmpty()
    }

    override fun isMutable(): Boolean {
        return values.size > 1
    }

    override fun copyContent(): Gene {
        //recall: "values" is immutable
        return EnumGene<T>(name, values, index, treatAsNotString)
    }

    override fun setValueWithRawString(value: String) {
        try {
            this.index = value.toInt()
        }catch (e: NumberFormatException){
            val foundIndex = values.indexOfFirst { it.equals(value) }
            if (foundIndex != -1)
                this.index = foundIndex
            else
                throw IllegalStateException("cannot find an index in this Enum with $value")
        }
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        if(values.isEmpty()) return

        val k = if (tryToForceNewValue && values.size > 1) {
            randomness.nextInt(0, values.size - 1, index)
        } else {
            randomness.nextInt(0, values.size - 1)
        }

        index = k
    }

    override fun shallowMutate(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        mwc: MutationWeightControl,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {

        /*
            FIXME
            we need to make a decision here...
            should we check enableAdaptiveGeneMutation here???
            point is, this is a value mutation, with no child involved.
            however, this is exactly the same behavior as in ChoiceGene, although there there are children.
            we shouldn't re-invent the wheel...
            so here we are re-using impact collection instead of ArchiveMutation just for simplicity, as we can
            get more info from each selected enum value that impacted the fitness.
         */

        if (additionalGeneMutationInfo?.impact != null && additionalGeneMutationInfo.impact is EnumGeneImpact) {
            val candidates = (0 until values.size).filter { index != it }
            val impacts = candidates.map {
                additionalGeneMutationInfo.impact.values[it]
            }.toList()
            val weights = additionalGeneMutationInfo.archiveGeneSelector.impactBasedOnWeights(
                impacts,
                additionalGeneMutationInfo.targets
            )
            if (weights.isNotEmpty()) {
                val selects =
                    mwc.selectSubsetWithWeight(candidates.mapIndexed { index, i -> candidates[index] to weights[index] }
                        .toMap(), true, 1.0)
                index = randomness.choose(selects)
                return true
            }
        }

        val next = (index + 1) % values.size
        index = next
        return true
    }


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {

        val res = values[index]
        if (res is String && !treatAsNotString) {
            return "\"$res\""
        } else {
            return res.toString()
        }
    }

    override fun getValueAsRawString(): String {
        return values[index].toString()
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is EnumGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        val current = this.index
        this.index = other.index
        if (!isLocallyValid()){
            this.index = current
            return false
        }

        return true
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is EnumGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        //FIXME what if compared to another enum with different values???
        return this.index == other.index
    }


    override fun bindValueBasedOn(gene: Gene): Boolean {
        when {
            gene is EnumGene<*> -> index == gene.index
            gene is StringGene && gene.getSpecializationGene() != null -> return bindValueBasedOn(gene.getSpecializationGene()!!)
            else -> {
                // since the binding is derived, it is not always true.
                log.info("cannot bind EnumGene with ${gene::class.java.simpleName}")
                return false
            }
        }
        return true
    }
}