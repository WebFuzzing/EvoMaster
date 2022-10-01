package org.evomaster.core.taint

import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.client.java.instrumentation.shared.StringSpecialization
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo
import org.evomaster.client.java.instrumentation.shared.TaintType
import org.evomaster.core.database.DbAction
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.string.StringGene
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

        if (individual.seeMainExecutableActions().size < additionalInfoList.size) {
            throw IllegalArgumentException("Less main actions than info entries")
        }

        if (log.isTraceEnabled){
            log.trace("do taint analysis for individual which contains dbactions: {} and rest actions: {}",
                individual.seeInitializingActions().joinToString(",") {
                    if (it is DbAction) it.getResolvedName() else it.getName()
                },
                individual.seeAllActions().joinToString(","){
                    if (it is RestCallAction) it.resolvedPath() else it.getName()
                }
            )
            log.trace("do taint analysis for {} additionalInfoList: {}",
                additionalInfoList.size, additionalInfoList.flatMap { a-> a.stringSpecializations.keys }.joinToString(","))
        }

        /*
    The old approach of checking the taint only for current main action was quite limiting:
    1) it would ignore taint in SQL actions
    2) a previous HTTP call could put a tainted value in the DB, read by a following action.

    So, regardless of the main action in which the taint was detected, we check its gene in ALL
    actions in the individual, including _previous_ ones. An optimization would be to ignore the _following_
    actions.
    Ideally, it should not be a problem, as tainted values are supposed to be unique. This is not currently
    enforce, so with low chances it could happened that 2 different genes have same tainted value.
    "Likely" rare, and "likely" with little to no side-effects if it happens (we ll see if it ll be indeed
    the case).

    Note, even if we force the invariant that 2 genes cannot share the same taint in a individual, we cannot guarantee
    of the taint values detected in the SUT. The string there might be manipulated (although it is _extremely_ unlike
    that a manipulated taint would still pass the taint regex check...)
         */

        val allTaintableGenes : List<StringGene> = individual.seeAllActions().flatMap { a->
            a.seeTopGenes().flatMap { it.flatView() }
                .filterIsInstance<StringGene>()
        }

        for (i in 0 until additionalInfoList.size) {

            val dto = additionalInfoList[i]
            if (dto.stringSpecializations == null || dto.stringSpecializations.isEmpty()) {
                continue
            }

            val specsMap = dto.stringSpecializations.entries
                    .map {
                        it.key to it.value.map { s ->
                            StringSpecializationInfo(
                                    StringSpecialization.valueOf(s.stringSpecialization),
                                    s.value,
                                    TaintType.valueOf(s.type))
                        }
                    }.toMap()


            handleSingleGenes(specsMap, allTaintableGenes, randomness)

            handleMultiGenes(specsMap, allTaintableGenes, randomness)
        }
    }

    private fun handleMultiGenes(specsMap: Map<String, List<StringSpecializationInfo>>, allTaintableGenes : List<StringGene>, randomness: Randomness) {

        val specs = specsMap.entries
                .flatMap { it.value }
                .filter { it.type == TaintType.PARTIAL_MATCH }
                .toSet()

        for (s in specs) {

            val genes = allTaintableGenes.filter {
                        specsMap.entries
                                .filter { e -> e.key.contains(it.getValueAsRawString(), true) }
                                .any { e -> e.value.any { d -> d == s } }
                    }

            if(genes.size <= 1){
                continue
            }

            /*
                TODO handling this properly is very complex. Something to do in the future,
                but for now we just keep a very basic, ad-hoc solution
             */

            if(!s.stringSpecialization.isRegex || genes.size != 2){
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

            val left = "("+s.value.subSequence(0, pos).toString() + ")$"
            val right = "^(" + s.value.subSequence(pos + divider.length, s.value.length).toString()+")"

            val taintInput = specsMap.entries.first { it.value.any { it == s } }.key

            val choices = if(taintInput.indexOf(genes[0].getValueAsRawString()) == 0 ){
                listOf(left, right)
            } else {
                listOf(right, left)
            }

            try {
                genes[0].addSpecializations(
                        genes[0].getValueAsRawString(),
                        listOf(StringSpecializationInfo(StringSpecialization.REGEX_WHOLE, choices[0])),
                        randomness)
                genes[1].addSpecializations(
                        genes[1].getValueAsRawString(),
                        listOf(StringSpecializationInfo(StringSpecialization.REGEX_WHOLE, choices[1])),
                        randomness)
            }catch (e: Exception){
                LoggingUtil.uniqueWarn(log, "Cannot handle partial match on regex: ${s.value}")
            }
        }
    }

    private fun handleSingleGenes(specsMap: Map<String, List<StringSpecializationInfo>>, allTaintableGenes: List<StringGene>, randomness: Randomness) {
        for (entry in specsMap.entries) {

            val taintedInput = entry.key

            if (entry.value.isEmpty()) {
                throw IllegalArgumentException("No specialization info for value $taintedInput")
            }

            //TODO what was the difference between these 2?
            val fullMatch = specsMap[taintedInput]!!.filter { it.type == TaintType.FULL_MATCH }
            val partialMatch = specsMap[taintedInput]!!.filter { it.type == TaintType.PARTIAL_MATCH }

            if (fullMatch.isNotEmpty()) {

                val genes = allTaintableGenes
                        .filter { it.getValueAsRawString().equals(taintedInput, true) }

                addSpecializationToGene(genes, taintedInput, fullMatch, randomness)
            }

            //partial match on single genes
            if (partialMatch.isNotEmpty()) {

                val genes = allTaintableGenes
                        .filter { taintedInput.contains(it.getValueAsRawString(), true) }

                addSpecializationToGene(genes, taintedInput, partialMatch, randomness)
            }
        }
    }

    private fun addSpecializationToGene(
        genes: List<StringGene>,
        taintedInput: String,
        specializations: List<StringSpecializationInfo>,
        randomness: Randomness
    ) {
        if (genes.isEmpty()) {
            /*
                        This can happen if the taint input is manipulated, but still with
                        same prefix and postfix. However, it would be extremely rare, and for sure
                        not in any of E2E, unless we explicitly write one for it
                    */
            log.warn("No taint input found '{}'", taintedInput)
            assert(false) // crash in tests, but not production
        } else {
            if (genes.size > 1) {
                //shouldn't really be a problem... but let keep track for it, for now at least
                log.warn("More than 2 gens have the taint '{}'", taintedInput)
            }
            genes.forEach { it.addSpecializations(taintedInput, specializations, randomness) }
        }
    }
}