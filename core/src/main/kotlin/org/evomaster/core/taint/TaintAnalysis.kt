package org.evomaster.core.taint

import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.client.java.instrumentation.shared.StringSpecialization
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo
import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.client.java.instrumentation.shared.TaintType
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.collection.*
import org.evomaster.core.search.gene.interfaces.TaintableGene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory


object TaintAnalysis {

    private val log: Logger = LoggerFactory.getLogger(TaintAnalysis::class.java)


    fun getRegexTaintedValues(action: Action): List<String> {
        return action.seeTopGenes()
                .flatMap { it.flatView() }
                .filterIsInstance<StringGene>()
                .filter { it.getSpecializationGene() != null && it.getSpecializationGene() is RegexGene }
                .map { it.getSpecializationGene()!!.getValueAsRawString() }
    }

    /**
     *   Analyze if any tainted value was used in the SUT in some special way.
     *   If that happened, then such info would end up in the AdditionalInfoDto.
     *   Then, we would extend the genotype (but not the phenotype!!!) of this test.
     */
    fun doTaintAnalysis(individual: Individual,
                        additionalInfoList: List<AdditionalInfoDto>,
                        randomness: Randomness,
                        enableConstraintHandling: Boolean) {


        if (individual.seeMainExecutableActions().size < additionalInfoList.size) {
            throw IllegalArgumentException("Less main actions than info entries")
        }

        if (log.isTraceEnabled) {
            log.trace("do taint analysis for individual which contains dbactions: {} and rest actions: {}",
                    individual.seeInitializingActions().joinToString(",") {
                        if (it is SqlAction) it.getResolvedName() else it.getName()
                    },
                    individual.seeAllActions().joinToString(",") {
                        if (it is RestCallAction) it.resolvedPath() else it.getName()
                    }
            )
            log.trace("do taint analysis for {} additionalInfoList: {}",
                    additionalInfoList.size, additionalInfoList.flatMap { a -> a.stringSpecializations.keys }.joinToString(","))
        }

        /*
    The old approach of checking the taint only for current main action was quite limiting:
    1) it would ignore taint in SQL actions
    2) a previous HTTP call could put a tainted value in the DB, read by a following action.

    So, regardless of the main action in which the taint was detected, we check its gene in ALL
    actions in the individual, including _previous_ ones. An optimization would be to ignore the _following_
    actions.
    Ideally, it should not be a problem, as tainted values are supposed to be unique. This is not currently
    enforce, so with low chances it could happen that 2 different genes have same tainted value.
    "Likely" rare, and "likely" with little to no side-effects if it happens (we ll see if it ll be indeed
    the case).

    Note, even if we force the invariant that 2 genes cannot share the same taint in an individual, we cannot guarantee
    of the taint values detected in the SUT. The string there might be manipulated (although it is _extremely_ unlike
    that a manipulated taint would still pass the taint regex check...).

    TODO: if we want to fix this, also need to keep in mind how taint is handled in SQL.
    Currently, embedded and external behaves differently. See HeuristicsCalculator
         */

        val allTaintableGenes: List<TaintableGene> =
                individual.seeAllActions()
                        .flatMap { a ->
                            a.seeTopGenes().flatMap { it.flatView() }
                                    .filterIsInstance<TaintableGene>()
                        }

        val inputVariables = individual.seeAllActions()
                .flatMap { getRegexTaintedValues(it) }
                .toSet()

        for (element in additionalInfoList) {

            if (element.stringSpecializations == null || element.stringSpecializations.isEmpty()) {
                continue
            }

            val specsMap = element.stringSpecializations.entries
                    .map {
                        it.key to it.value.map { s ->
                            StringSpecializationInfo(
                                    StringSpecialization.valueOf(s.stringSpecialization),
                                    s.value,
                                    TaintType.valueOf(s.type)
                            )
                        }
                    }.toMap()


            handleSingleGenes(specsMap, allTaintableGenes, randomness, inputVariables, enableConstraintHandling)

            handleMultiGenes(specsMap, allTaintableGenes, randomness, inputVariables, enableConstraintHandling)

            handleTaintedArrays(specsMap, allTaintableGenes, randomness, inputVariables, enableConstraintHandling)
        }
    }


    private fun handleTaintedArrays(
            specsMap: Map<String, List<StringSpecializationInfo>>,
            allTaintableGenes: List<TaintableGene>,
            randomness: Randomness,
            inputVariables: Set<String>,
            enableConstraintHandling: Boolean) {

        val taintedArrays = allTaintableGenes.filterIsInstance<TaintedArrayGene>()

        if (taintedArrays.isEmpty()) {
            return
        }

        for (entry in specsMap.entries) {

            val taintedInput = entry.key
            val specs = entry.value

            if (specs.isEmpty()) {
                throw IllegalArgumentException("No specialization info for value $taintedInput")
            }

            val genes = taintedArrays.filter { it.getPossiblyTaintedValue().equals(taintedInput, true) }
            if (genes.isEmpty()) {
                continue
            }

            if (specs.size > 1) {
                log.warn("More than one possible specialization for tainted array '$taintedInput': $specs")
            }

            val s = specs.find { it.stringSpecialization == StringSpecialization.JSON_OBJECT }
                    ?: randomness.choose(specs)

            val template = if (s.stringSpecialization == StringSpecialization.JSON_OBJECT) {
                val schema = s.value
                val t = schema.subSequence(0, schema.indexOf(":")).trim().toString()
                val ref = t.subSequence(1, t.length - 1).toString()
                RestActionBuilderV3.createGeneForDTO(ref, schema, RestActionBuilderV3.Options(enableConstraintHandling=enableConstraintHandling) )
            } else {
                /*
                    TODO this could be more sophisticated, like considering numeric and boolean arrays as well,
                    and already initializing the values in array with some of taints.
                    but, as this "likely" would be rare (ie JSON array of non-objects as root element in parsing),
                    no need for now.
                 */
                StringGene("element")
            }

            genes.forEach {
                it.resolveTaint(
                        ArrayGene(it.name, template.copy()).apply { doInitialize(randomness) }
                )
            }
        }
    }

    private fun handleMultiGenes(
            specsMap: Map<String, List<StringSpecializationInfo>>,
            allTaintableGenes: List<TaintableGene>,
            randomness: Randomness,
            inputVariables: Set<String>,
            enableConstraintHandling: Boolean) {

        val specs = specsMap.entries
                .flatMap { it.value }
                .filter { it.type == TaintType.PARTIAL_MATCH }
                .toSet()

        for (s in specs) {

            val genes = allTaintableGenes.filter {
                specsMap.entries
                        .filter { e -> e.key.contains(it.getPossiblyTaintedValue(), true) }
                        .any { e -> e.value.any { d -> d == s } }
            }.filterIsInstance<StringGene>()

            if (genes.size <= 1) {
                continue
            }

            /*
                TODO handling this properly is very complex. Something to do in the future,
                but for now we just keep a very basic, ad-hoc solution
             */

            if (!s.stringSpecialization.isRegex || genes.size != 2) {
                continue
            }

            /*
                TODO we just handle for now this special case, but we would need a general approach
             */
            val divider = "\\Q-\\E"
            val pos = s.value.indexOf(divider)

            if (pos < 0) {
                continue
            }

            val left = "(" + s.value.subSequence(0, pos).toString() + ")$"
            val right = "^(" + s.value.subSequence(pos + divider.length, s.value.length).toString() + ")"

            val taintInput = specsMap.entries.first { it.value.any { it == s } }.key

            val choices = if (taintInput.indexOf(genes[0].getValueAsRawString()) == 0) {
                listOf(left, right)
            } else {
                listOf(right, left)
            }

            try {
                genes[0].addSpecializations(
                        genes[0].getValueAsRawString(),
                        listOf(StringSpecializationInfo(StringSpecialization.REGEX_WHOLE, choices[0])),
                        randomness, enableConstraintHandling = enableConstraintHandling)
                genes[1].addSpecializations(
                        genes[1].getValueAsRawString(),
                        listOf(StringSpecializationInfo(StringSpecialization.REGEX_WHOLE, choices[1])),
                        randomness, enableConstraintHandling = enableConstraintHandling)
            } catch (e: Exception) {
                LoggingUtil.uniqueWarn(log, "Cannot handle partial match on regex: ${s.value}")
            }
        }
    }

    private fun handleSingleGenes(
            specsMap: Map<String, List<StringSpecializationInfo>>,
            allTaintableGenes: List<TaintableGene>,
            randomness: Randomness,
            inputVariables: Set<String>,
            enableConstraintHandling: Boolean) {
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
                        .filter {
                            (TaintInputName.isTaintInput(it.getPossiblyTaintedValue())
                                    || inputVariables.contains(it.getPossiblyTaintedValue()))
                                    && it.getPossiblyTaintedValue().equals(taintedInput, true)
                        }
                        .filterIsInstance<StringGene>()

                addSpecializationToGene(genes, taintedInput, fullMatch, randomness, enableConstraintHandling)
            }

            //partial match on single genes
            if (partialMatch.isNotEmpty()) {

                val genes = allTaintableGenes
                        .filter {
                            (TaintInputName.isTaintInput(it.getPossiblyTaintedValue())
                                    || inputVariables.contains(it.getPossiblyTaintedValue()))
                                    && taintedInput.contains(it.getPossiblyTaintedValue(), true)
                        }
                        .filterIsInstance<StringGene>()

                addSpecializationToGene(genes, taintedInput, partialMatch, randomness, enableConstraintHandling)
            }
        }
    }

    private fun addSpecializationToGene(
            genes: List<StringGene>,
            taintedInput: String,
            specializations: List<StringSpecializationInfo>,
            randomness: Randomness,
            enableConstraintHandling: Boolean
    ) {
        if (genes.isEmpty()) {
            /*
                        This can happen if the taint input is manipulated, but still with
                        same prefix and postfix. However, it would be extremely rare, and for sure
                        not in any of E2E, unless we explicitly write one for it
                    */
            //log.warn("No taint input found '{}'", taintedInput)
            /*
                FIXME put back once debug issue on Linux.
                The issue is that H2 is caching requests... our fix for that work on local machines (including
                Linux) but fails somehow on CI

                UPDATE: solution doesn't really work, as SQL commands are analyzed later, after put in a buffer.
                See: ExecutionTracer.shouldSkipTaint()
             */
            //assert(false) // crash in tests, but not production
        } else {
            if (genes.size > 1
                    && TaintInputName.isTaintInput(taintedInput)
                    && genes.none { x -> genes.any { y -> x.isDirectBoundWith(y) || x.is2DepthDirectBoundWith(y) || x.isAnyParentBoundWith(y) } }
            ) {
                //shouldn't really be a problem... but let keep track for it, for now at least.
                // note, cannot really guarantee that a taint from regex is unique, as regex could generate
                // any kind of string...
                // also if genes are bound, then of course going to be more than 2...
                log.warn("More than 2 genes have the taint '{}'", taintedInput)
                //FIXME possible bug in binding handling.
//                assert(false)
            }
            genes.forEach { it.addSpecializations(taintedInput, specializations, randomness, enableConstraintHandling = enableConstraintHandling) }
        }
    }
}