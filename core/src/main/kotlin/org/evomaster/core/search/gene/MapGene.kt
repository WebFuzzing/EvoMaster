package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.ImpactMutationSelection
import org.evomaster.core.search.impact.value.collection.CollectionGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.log


class MapGene<T>(
        name: String,
        val template: T,
        val maxSize: Int = 5,
        var elements: MutableList<T> = mutableListOf()
) : Gene(name)
        where T : Gene {

    private var keyCounter = 0

    init {
        if (elements.size > maxSize) {
            throw IllegalArgumentException(
                    "More elements (${elements.size}) than allowed ($maxSize)")
        }
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(MapGene::class.java)
    }

    override fun copy(): Gene {
        return MapGene<T>(name,
                template.copy() as T,
                maxSize,
                elements.map { e -> e.copy() as T }.toMutableList()
        )
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is MapGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.elements = other.elements.map { e -> e.copy() as T }.toMutableList()
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is MapGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.elements.size == other.elements.size
                && this.elements.zip(other.elements) { thisElem, otherElem ->
            thisElem.containsSameValueAs(otherElem)
        }.all { it == true }
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        //maybe not so important here to complicate code to enable forceNewValue

        elements.clear()
        val n = randomness.nextInt(maxSize)
        (0 until n).forEach {
            val gene = template.copy() as T
            gene.randomize(randomness, false)
            gene.name = "key_${keyCounter++}"
            elements.add(gene)
        }
    }

    override fun isMutable(): Boolean {
        //it wouldn't make much sense to have 0, but let's just be safe here
        return maxSize > 0
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {

        if(elements.isEmpty() || (elements.size < maxSize && randomness.nextBoolean(0.1))){
            val gene = template.copy() as T
            gene.randomize(randomness, false)
            gene.name = "key_${keyCounter++}"
            elements.add(gene)
        } else if(elements.size > 0 && randomness.nextBoolean(0.1)){
            elements.removeAt(randomness.nextInt(elements.size))
        } else {
            val gene = randomness.choose(elements)
            gene.standardMutation(randomness, apc, allGenes)
        }
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
        return "{" +
                elements.filter { f ->
                    f !is CycleObjectGene &&
                            (f !is OptionalGene || f.isActive)
                }.map { f ->
                    """
                    "${f.name}":${f.getValueAsPrintableString(targetFormat = targetFormat)}
                    """
                }.joinToString(",") +
                "}"
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if (excludePredicate(this)) listOf(this)
        else listOf(this).plus(elements.flatMap { g -> g.flatView(excludePredicate) })
    }

    override fun archiveMutation(randomness: Randomness, allGenes: List<Gene>, apc: AdaptiveParameterControl, selection: ImpactMutationSelection, impact: GeneImpact?, geneReference: String, archiveMutator: ArchiveMutator, evi: EvaluatedIndividual<*>) {
        var add = elements.isEmpty()
        var delete = elements.size == maxSize

        val modifySize = if (impact != null && impact is CollectionGeneImpact){
            val probOfMutateSize = if (impact.sizeImpact.timesToManipulate == 0) 0.1
            else impact.sizeImpact.timesOfImpact/impact.sizeImpact.timesToManipulate.toDouble().run {
                when {
                    this == 0.0 -> 0.05
                    this > 0.5 -> 0.2
                    else -> this
                }
            }
            randomness.nextBoolean(probOfMutateSize)
        }else {
            randomness.nextBoolean(0.1)
        }
        if (modifySize){
            val p = randomness.nextBoolean()
            add = add || p
            delete = delete || !p
        }

        if (add && add == delete)
            log.warn("add and delete an element cannot happen in a mutation")

        when{
            add ->{
                val gene = template.copy() as T
                gene.randomize(randomness, false)
                gene.name = "key_${keyCounter++}"
                elements.add(gene)
            }
            delete ->{
                elements.removeAt(randomness.nextInt(elements.size))
            }
            else -> {
                val gene = randomness.choose(elements)
                gene.archiveMutation(randomness, allGenes, apc, selection, null, geneReference, archiveMutator, evi)
            }
        }
    }
}