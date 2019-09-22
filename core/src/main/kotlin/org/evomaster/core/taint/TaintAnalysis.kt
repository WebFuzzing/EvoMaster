package org.evomaster.core.taint

import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.client.java.instrumentation.shared.StringSpecialization
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo
import org.evomaster.client.java.instrumentation.shared.TaintType
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory


object TaintAnalysis {

    private val log: Logger = LoggerFactory.getLogger(TaintAnalysis::class.java)

    fun doTaintAnalysis(individual: Individual,
                        additionalInfoList: List<AdditionalInfoDto>,
                        randomness: Randomness) {

        /*
            Analyze if any tainted value was used in the SUT in some special way.
            If that happened, then such info would end up in the AdditionalInfoDto.
            Then, we would extend the genotype (but not the phenotype!!!) of this test.
         */

        if (individual.seeActions().size < additionalInfoList.size) {
            throw IllegalArgumentException("Less actions than info entries")
        }

        for (i in 0 until additionalInfoList.size) {

            val dto = additionalInfoList[i]
            if (dto.stringSpecializations == null || dto.stringSpecializations.isEmpty()) {
                continue
            }

            val action = individual.seeActions()[i]

            val specsMap = dto.stringSpecializations.entries
                    .map {
                        it.key to it.value.map { s ->
                            StringSpecializationInfo(
                                    StringSpecialization.valueOf(s.stringSpecialization),
                                    s.value,
                                    TaintType.valueOf(s.type))
                        }
                    }.toMap()

            handleSingleGenes(specsMap, action, randomness)

            handleMultiGenes(specsMap, action, randomness)
        }
    }

    private fun handleMultiGenes(specsMap: Map<String, List<StringSpecializationInfo>>, action: Action, randomness: Randomness) {

        val specs = specsMap.entries
                .flatMap { it.value }
                .filter { it.type == TaintType.PARTIAL_MATCH }
                .toSet()

        for (s in specs) {

            val genes = action.seeGenes()
                    .flatMap { it.flatView() }
                    .filterIsInstance<StringGene>()
                    .filter {
                        specsMap.entries
                                .filter { e -> e.key.contains(it.getValueAsRawString()) }
                                .any { e -> e.value.any { d -> d == s } }
                    }

            if(genes.size <= 1){
                continue
            }

            /*
                TODO handling this properly is very complex. Something to do in the future,
                but for now we just keep a very basic, ad-hoc solution
             */

            if(s.stringSpecialization != StringSpecialization.REGEX || genes.size != 2){
                continue
            }

            /*
                TODO we just handle for now this special case, but we would need a general approach
             */
            val divider = "\\Q-\\E"
            val pos = s.value.indexOf(divider)

            if(pos < 0){
                continue
            }

            val left = s.value.subSequence(0, pos).toString() + ")$"
            val right = "^(" + s.value.subSequence(pos + divider.length, s.value.length).toString()

            val taintInput = specsMap.entries.first { it.value.any { it == s } }.key

            val choices = if(taintInput.indexOf(genes[0].getValueAsRawString()) == 0 ){
                listOf(left, right)
            } else {
                listOf(right, left)
            }

            try {
                genes[0].addSpecializations(
                        genes[0].getValueAsRawString(),
                        listOf(StringSpecializationInfo(StringSpecialization.REGEX, choices[0])),
                        randomness)
                genes[1].addSpecializations(
                        genes[1].getValueAsRawString(),
                        listOf(StringSpecializationInfo(StringSpecialization.REGEX, choices[1])),
                        randomness)
            }catch (e: Exception){
                LoggingUtil.uniqueWarn(log, "Cannot handle partial match on regex: ${s.value}")
            }
        }
    }

    private fun handleSingleGenes(specsMap: Map<String, List<StringSpecializationInfo>>, action: Action, randomness: Randomness) {
        for (entry in specsMap.entries) {

            val taintedInput = entry.key

            if (entry.value.isEmpty()) {
                throw IllegalArgumentException("No specialization info for value $taintedInput")
            }


            val fullMatch = specsMap[taintedInput]!!.filter { it.type == TaintType.FULL_MATCH }
            val partialMatch = specsMap[taintedInput]!!.filter { it.type == TaintType.PARTIAL_MATCH }

            if (fullMatch.isNotEmpty()) {

                val genes = action.seeGenes()
                        .flatMap { it.flatView() }
                        .filterIsInstance<StringGene>()
                        .filter { it.getValueAsRawString() == taintedInput }

                if (genes.isEmpty()) {
                    /*
                            This can happen if the taint input is manipulated, but still with
                            same prefix and postfix
                         */
                    log.debug("No taint input '$taintedInput'")
                } else {
                    genes.forEach { it.addSpecializations(taintedInput, fullMatch, randomness) }
                }
            }

            //partial match on single genes
            if (partialMatch.isNotEmpty()) {

                val genes = action.seeGenes()
                        .flatMap { it.flatView() }
                        .filterIsInstance<StringGene>()
                        .filter { taintedInput.contains(it.getValueAsRawString()) }

                if (genes.isEmpty()) {
                    /*
                            This can happen if the taint input is manipulated, but still with
                            same prefix and postfix
                         */
                    log.debug("No taint input '$taintedInput'")
                } else {
                    genes.forEach { it.addSpecializations(taintedInput, partialMatch, randomness) }
                }
            }
        }
    }
}