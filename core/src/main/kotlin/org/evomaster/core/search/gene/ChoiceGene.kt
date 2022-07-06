package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class ChoiceGene<T>(name: String,
                 private val choices: List<T>,
                 activeChoice: Int = 0

) : CompositeFixedGene(name, choices) where T : Gene {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ChoiceGene::class.java)
    }

    var activeChoice: Int = activeChoice
        private set

    init {
        if (activeChoice < 0 || activeChoice >= choices.size) {
            throw IllegalArgumentException("Active choice must be between 0 and ${choices.size - 1}")
        }
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean, allGenes: List<Gene>) {
        activeChoice = randomness.nextInt(choices.size)
        if (choices[activeChoice].isMutable()) {
            choices[activeChoice].randomize(randomness, tryToForceNewValue, allGenes)
        }
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) = innerGene()

    override fun innerGene() =
            listOf(choices[activeChoice])


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return choices[activeChoice]
                .getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
    }

    override fun getValueAsRawString(): String {
        return choices[activeChoice]
                .getValueAsRawString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is ChoiceGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        } else if (choices.size != other.choices.size) {
            throw IllegalArgumentException("Cannot copy value from another choice gene with  ${other.choices.size} choices (current gene has ${choices.size} choices)")
        } else {
            this.activeChoice = other.activeChoice
            for (i in choices.indices) {
                this.choices[i].copyValueFrom(other.choices[i])
            }
        }
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is ChoiceGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        if (this.activeChoice != other.activeChoice) {
            return false
        }

        return this.choices[activeChoice]
                .containsSameValueAs(other.choices[activeChoice])
    }

    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is ChoiceGene<*> && gene.choices.size == choices.size) {
            var result = true
            choices.indices.forEach { i ->
                val r = choices[i].bindValueBasedOn(gene.choices[i])
                if (!r)
                    LoggingUtil.uniqueWarn(log, "cannot bind disjunctions (name: ${choices[i].name}) at index $i")
                result = result && r
            }

            activeChoice = gene.activeChoice
            return result
        }

        LoggingUtil.uniqueWarn(log, "cannot bind ChoiceGene with ${gene::class.java.simpleName}")
        return false
    }

    override fun copyContent(): Gene = ChoiceGene(
            name,
            activeChoice = this.activeChoice,
            choices = this.choices.map { it.copy() }.toList()
    )


}