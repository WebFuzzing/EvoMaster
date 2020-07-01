package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.GeneUtils.getDelta
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.geneMutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveGeneMutator
import org.evomaster.core.search.service.mutator.geneMutation.SubsetGeneSelectionStrategy
import org.evomaster.core.search.service.mutator.geneMutation.archive.GeneArchieMutationInfo
import org.evomaster.core.search.service.mutator.geneMutation.archive.IntegerGeneArchiveMutationInfo


class IntegerGene(
        name: String,
        value: Int = 0,
        /** Inclusive */
        val min: Int = Int.MIN_VALUE,
        /** Inclusive */
        val max: Int = Int.MAX_VALUE,

        val mutationInfo : GeneArchieMutationInfo = GeneArchieMutationInfo()
) : NumberGene<Int>(name, value) {

    override fun copy(): Gene {
        return IntegerGene(name, value, min, max, mutationInfo.copy())
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is IntegerGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is IntegerGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        val z = 1000
        val range = max.toLong() - min.toLong() + 1L

        val a: Int
        val b: Int

        if (range > z && randomness.nextBoolean(0.95)) {
            //if very large range, might want to sample small values around 0 most of the times
            if (min <= 0 && max >= z) {
                a = 0
                b = z
            } else if (randomness.nextBoolean()) {
                a = min
                b = min + z
            } else {
                a = max - z
                b = max
            }
        } else {
            a = min
            b = max
        }

        value = if (forceNewValue) {
            randomness.nextInt(a, b, value)
        } else {
            randomness.nextInt(a, b)
        }
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): Boolean {

        //check maximum range. no point in having a delta greater than such range
        val range: Long = if (enableAdaptiveGeneMutation){
            additionalGeneMutationInfo?:throw IllegalArgumentException("additionalGeneMutationInfo should not be null when enable adaptive gene mutation")
            (additionalGeneMutationInfo.archiveGeneMutator.identifyMutation(this, additionalGeneMutationInfo.targets) as IntegerGeneArchiveMutationInfo).run {
                this.valueMutation.preferMax.toLong() - this.valueMutation.preferMin.toLong()
            }
        } else max.toLong() - min.toLong()

        //choose an i for 2^i modification
        val delta = getDelta(randomness, apc, range)

        val sign = when (value) {
            max -> -1
            min -> +1
            else -> randomness.choose(listOf(-1, +1))
        }

        val res: Long = (value.toLong()) + (sign * delta)

        value = when {
            res > max -> max
            res < min -> min
            else -> res.toInt()
        }

        //TODO MAN add archive-based mutation
        return true
    }


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        return value.toString()
    }

    override fun archiveMutationUpdate(original: Gene, mutated: Gene, targetsEvaluated: Map<Int, EvaluatedMutation>, archiveMutator: ArchiveGeneMutator) {

        original as? IntegerGene ?: throw IllegalStateException("$original should be IntegerGene")
        mutated as? IntegerGene ?: throw IllegalStateException("$mutated should be IntegerGene")


        val previous = original.value
        val current = mutated.value

        val isMutated = this == mutated

        targetsEvaluated.forEach { (t, u) ->
            val archiveMutationInfo = mutationInfo.getArchiveMutationInfo(this, t) as? IntegerGeneArchiveMutationInfo ?: throw IllegalStateException("mutation info for StringGene should be IntegerGeneArchiveMutationInfo")
            val marchiveMutationInfo = mutated.mutationInfo.getArchiveMutationInfo(this, t) as? IntegerGeneArchiveMutationInfo ?: throw IllegalStateException("mutation info for StringGene should be IntegerGeneArchiveMutationInfo")

            if (!isMutated && marchiveMutationInfo.valueMutation.reached)
                archiveMutationInfo.valueMutation.reached = marchiveMutationInfo.valueMutation.reached

            /*
                1) current.length is not in min..max, but current is better -> reset
                2) lengthMutation is optimal, but current is better -> reset
            */
            val becomeBetter = u == EvaluatedMutation.BETTER_THAN
            if (becomeBetter && (
                            current !in archiveMutationInfo.valueMutation.preferMin..archiveMutationInfo.valueMutation.preferMax
                            )) {
                archiveMutationInfo.valueMutation.reset(min, max)
                archiveMutationInfo.plusDependencyInfo()
                return
            }
            archiveMutationInfo.valueMutation.updateBoundary(original.value, mutated.value, becomeBetter)

            if (0 == archiveMutator.validateCandidates(archiveMutationInfo.valueMutation.preferMin, archiveMutationInfo.valueMutation.preferMax, exclude = setOf(previous, current).toList())) {
                archiveMutationInfo.valueMutation.reached = true
            }
        }

    }

    override fun reachOptimal(targets: Set<Int>): Boolean {
        return mutationInfo.reachOptimal(targets)
    }
}