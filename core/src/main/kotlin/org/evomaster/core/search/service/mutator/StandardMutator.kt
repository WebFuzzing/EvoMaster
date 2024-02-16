package org.evomaster.core.search.service.mutator

import org.evomaster.core.EMConfig
import org.evomaster.core.EMConfig.GeneMutationStrategy.ONE_OVER_N
import org.evomaster.core.EMConfig.GeneMutationStrategy.ONE_OVER_N_BIASED_SQL
import org.evomaster.core.Lazy
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionUtils
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.api.ApiWsAction
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.api.param.UpdateForParam
import org.evomaster.core.problem.externalservice.rpc.RPCExternalServiceAction
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.graphql.GraphQLUtils
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.param.UpdateForBodyParam
import org.evomaster.core.problem.rest.resource.ResourceImpactOfIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.Individual.GeneFilter.ALL
import org.evomaster.core.search.Individual.GeneFilter.NO_SQL
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.TaintedArrayGene
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.EvaluatedInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.max

/**
 * make the standard mutator open for extending the mutator,
 *
 * e.g., in order to handle resource rest individual
 */
open class StandardMutator<T> : Mutator<T>() where T : Individual {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(StandardMutator::class.java)
    }

    override fun doesStructureMutation(evaluatedIndividual: EvaluatedIndividual<T>): Boolean {

        val prob = if(config.isMIO()){
            when (config.structureMutationProbStrategy) {
                EMConfig.StructureMutationProbStrategy.SPECIFIED -> config.structureMutationProbability
                EMConfig.StructureMutationProbStrategy.SPECIFIED_FS -> if (apc.doesFocusSearch()) config.structureMutationProFS else config.structureMutationProbability
                EMConfig.StructureMutationProbStrategy.DPC_TO_SPECIFIED_BEFORE_FS -> apc.getExploratoryValue(
                    config.structureMutationProbability,
                    config.structureMutationProFS
                )
                EMConfig.StructureMutationProbStrategy.DPC_TO_SPECIFIED_AFTER_FS -> apc.getDPCValue(
                    config.structureMutationProbability,
                    config.structureMutationProFS,
                    config.focusedSearchActivationTime,
                    1.0
                )
                EMConfig.StructureMutationProbStrategy.ADAPTIVE_WITH_IMPACT -> {
                    if (!apc.doesFocusSearch()) config.structureMutationProbability
                    else {
                        val impact = (evaluatedIndividual.impactInfo ?: throw IllegalStateException("lack of impact info"))
                        if (impact.impactsOfStructure.recentImprovement()
                            || impact.impactsOfStructure.sizeImpact.recentImprovement()
                            || (impact is ResourceImpactOfIndividual && (impact.resourceSizeImpact.any { it.value.recentImprovement() } || impact.sqlTableSizeImpact.any { it.value.recentImprovement() }))
                        ) config.structureMutationProbability
                        else 0.0
                    }
                }
            }
        }else
            config.structureMutationProbability

        return structureMutator.canApplyStructureMutator(evaluatedIndividual.individual) &&
//                (config.maxTestSize > 1) && // if the maxTestSize is 1, there is no point to do structure mutation
                randomness.nextBoolean(prob)
    }

    override fun genesToMutation(individual: T, evi: EvaluatedIndividual<T>, targets: Set<Int>): List<Gene> {
        val filterMutate = if (config.generateSqlDataWithSearch) ALL else NO_SQL
        val genes = individual.seeGenes(filterMutate).filter { it.isMutable() }
        return genes
    }

    override fun selectGenesToMutate(
        individual: T,
        evi: EvaluatedIndividual<T>,
        targets: Set<Int>,
        mutatedGenes: MutatedGeneSpecification?
    ): List<Gene> {
        // the genes that could be possibly chosen for mutation
        val geneCandidates = genesToMutation(individual, evi, targets)
        if (geneCandidates.isEmpty()) return mutableListOf()

        val filterN = when (config.geneMutationStrategy) {
            ONE_OVER_N -> ALL
            ONE_OVER_N_BIASED_SQL -> NO_SQL
        }
        // the actual chosen genes, that will be mutated
        val toMutate = mutableListOf<Gene>()

        if (!config.isEnabledWeightBasedMutation()) {
            val p = 1.0 / max(1, individual.seeGenes(filterN).filter { geneCandidates.contains(it) }.size)
            while (toMutate.isEmpty()) {
                geneCandidates.forEach { g ->
                    if (randomness.nextBoolean(p))
                        toMutate.add(g)
                }
            }
        } else {
            val enableAPC = config.isEnabledWeightBasedMutation()
                    && archiveGeneSelector.applyArchiveSelection()

            val noSQLGenes = individual.seeGenes(NO_SQL).filter { geneCandidates.contains(it) }
            val sqlGenes = geneCandidates.filterNot { noSQLGenes.contains(it) }
            while (toMutate.isEmpty()) {
                if (config.specializeSQLGeneSelection && noSQLGenes.isNotEmpty() && sqlGenes.isNotEmpty()) {
                    toMutate.addAll(
                        mwc.selectSubGene(
                            noSQLGenes,
                            enableAPC,
                            targets,
                            null,
                            individual,
                            evi,
                            forceNotEmpty = false,
                            numOfGroup = 2
                        )
                    )
                    toMutate.addAll(
                        mwc.selectSubGene(
                            sqlGenes,
                            enableAPC,
                            targets,
                            null,
                            individual,
                            evi,
                            forceNotEmpty = false,
                            numOfGroup = 2
                        )
                    )
                } else {
                    toMutate.addAll(
                        mwc.selectSubGene(
                            geneCandidates,
                            enableAPC,
                            targets,
                            null,
                            individual,
                            evi,
                            forceNotEmpty = false
                        )
                    )
                }
            }
        }

        if(config.taintForceSelectionOfGenesWithSpecialization){
            individual.seeGenes()
                .filterIsInstance<StringGene>()
                .filter { it.selectionUpdatedSinceLastMutation }
                .forEach {
                    if(!toMutate.contains(it)){
                        toMutate.add(it)
                    }
                }
        }

        return toMutate
    }

    private fun mutationPreProcessing(individual: T) {

        for(a in individual.seeAllActions()){
            val update =if(a is ApiWsAction) {
                a.parameters.find { it is UpdateForBodyParam } as? UpdateForBodyParam
            }else if (a is RPCExternalServiceAction){
                a.responses.find { it is UpdateForParam } as? UpdateForParam
            } else null
            if (update != null) {
                a.killChildren { it is UpdateForParam || (it is Param && update.isSameTypeWithUpdatedParam(it))  }
                val copy = update.getUpdatedParam()
                copy.resetLocalIdRecursively()
                a.addChild(copy)
            }

            val allGenes = a.seeTopGenes().flatMap { it.flatView() }

            //make sure that requested genes are activated
            allGenes.filterIsInstance<OptionalGene>()
                .filter { it.selectable && it.requestSelection }
                .forEach { it.isActive = true; it.requestSelection = false }

            allGenes.filterIsInstance<TaintedArrayGene>()
                .filter{!it.isActive && it.isResolved()}
                .forEach { it.activate() }

            //disable genes that should no longer be mutated
            val state = individual.searchGlobalState
            if(state != null) {
                val time = state.time.percentageUsedBudget()

                allGenes.filterIsInstance<CustomMutationRateGene<*>>()
                    .filter { it.probability > 0 && it.searchPercentageActive < time }
                    .forEach { it.preventMutation() }

                allGenes.filterIsInstance<OptionalGene>()
                    .filter { it.searchPercentageActive < time }
                    .forEach { it.forbidSelection() }
            }
        }
    }

    private fun innerMutate(
        individual: EvaluatedIndividual<T>,
        targets: Set<Int>,
        mutatedGene: MutatedGeneSpecification?
    ): T {

        val copy = individual.individual.copy() as T

        if(doesStructureMutation(individual)){
            if (log.isTraceEnabled){
                log.trace("structure mutator will be applied")
            }
            if (doesInitStructureMutation(individual))
                structureMutator.mutateInitStructure(copy, individual, mutatedGene, targets)
            else
                structureMutator.mutateStructure(copy, individual, mutatedGene, targets)
            return copy
        }

        mutationPreProcessing(copy)

        val selectGeneToMutate = selectGenesToMutate(copy, individual, targets, mutatedGene)

        if (selectGeneToMutate.isEmpty())
            return copy

        for (gene in selectGeneToMutate) {

            val adaptive = randomness.nextBoolean(config.probOfArchiveMutation)

            // enable weight based mutation when mutating gene
            val enableWGS = config.isEnabledWeightBasedMutation() && config.enableWeightBasedMutationRateSelectionForGene
            // enable gene selection when mutating gene, eg, ObjectGene
            val enableAGS = enableWGS && adaptive && config.isEnabledArchiveGeneSelection()
            // enable gene mutation based on history
            val enableAGM = adaptive && config.isEnabledArchiveGeneMutation()

            val selectionStrategy = when {
                enableAGS -> SubsetGeneMutationSelectionStrategy.ADAPTIVE_WEIGHT
                enableWGS && !enableAGS -> SubsetGeneMutationSelectionStrategy.DETERMINISTIC_WEIGHT
                else -> SubsetGeneMutationSelectionStrategy.DEFAULT
            }

            val additionInfo = mutationConfiguration(
                gene = gene,
                individual = copy,
                eval = individual,
                enableAGM = enableAGM,
                enableAGS = enableAGS,
                targets = targets,
                mutatedGene = mutatedGene
            )

            // plugin seeding response here
            val mutated = config.isEnabledMutatingResponsesBasedOnActualResponse() && harvestResponseHandler.harvestExistingGeneBasedOn(gene, config.probOfMutatingResponsesBasedOnActualResponse)

            if (!mutated)
                gene.standardMutation(
                    randomness,
                    apc,
                    mwc,
                    selectionStrategy,
                    enableAGM,
                    additionalGeneMutationInfo = additionInfo
                )

        }

        if (config.trackingEnabled()) tag(copy, time.evaluatedIndividuals)
        return copy
    }

    override fun mutate(
        individual: EvaluatedIndividual<T>,
        targets: Set<Int>,
        mutatedGenes: MutatedGeneSpecification?
    ): T {

        //  mutate the individual
        val mutatedIndividual = innerMutate(individual, targets, mutatedGenes)

        postActionAfterMutation(mutatedIndividual, mutatedGenes)

//        if (config.trackingEnabled()) tag(mutatedIndividual, time.evaluatedIndividuals)

        return mutatedIndividual
    }

    override fun postActionAfterMutation(mutatedIndividual: T, mutated: MutatedGeneSpecification?) {

        Lazy.assert {
            SqlActionUtils.verifyForeignKeys(
                mutatedIndividual.seeInitializingActions().filterIsInstance<SqlAction>()
            )
        }

        Lazy.assert {
            mutatedIndividual.seeAllActions()
                .flatMap { it.seeTopGenes() }
                .all {
                    GeneUtils.verifyRootInvariant(it) &&
                            !GeneUtils.hasNonHandledCycles(it)
                }
        }

        // repair the initialization actions (if needed)
        mutatedIndividual.repairInitializationActions(randomness)

        //Check that the repair was successful
        Lazy.assert { mutatedIndividual.verifyInitializationActions() }

        /*
            In GraphQL, each boolean selection in Objects MUST have at least one filed selected
         */
        if (mutatedIndividual is GraphQLIndividual) {
            GraphQLUtils.repairIndividual(mutatedIndividual)
        }

        if (!mutatedIndividual.verifyBindingGenes()) {
            mutatedIndividual.cleanBrokenBindingReference()
            Lazy.assert { mutatedIndividual.verifyBindingGenes() }
        }

        if (mutatedIndividual is RestIndividual)
            mutatedIndividual.repairDbActionsInCalls()

        // update MutatedGeneSpecification after the post-handling
        if(mutated?.repairInitAndDbSpecification(mutatedIndividual) == true){
            LoggingUtil.uniqueWarn(log, "DbActions which contain mutated gene are removed that might need a further check")
        }

    }

    /**
     * @param gene is selected to mutate
     * @param individual is the individual that contains the [gene]
     * @param eval is an sampled evaluated indvidual for the mutation
     * @param enableAGS indicates whether the archive-based selection is applied for the gene mutation
     * @param enableAGM indicates whether to apply archive-based gene mutation
     * @param targets is a set of targets to cover
     * @param mutatedGene contains what genes are mutated in this mutation
     */
    fun mutationConfiguration(
        gene: Gene, individual: T,
        eval: EvaluatedIndividual<T>,
        enableAGS: Boolean,
        enableAGM: Boolean,
        targets: Set<Int>, mutatedGene: MutatedGeneSpecification?, includeSameValue: Boolean = false
    ): AdditionalGeneMutationInfo? {

        val isFromInit = individual.seeInitializingActions().any { it.seeTopGenes().contains(gene) }
        val isDbInResourceCall = (individual as? RestIndividual)?.getResourceCalls()?.any {
            it.seeActions(ActionFilter.ONLY_SQL).any { d -> d.seeTopGenes().contains(gene) }
        } ?: false
        val isFixedAction = !isFromInit && individual.seeFixedMainActions().any { it.seeTopGenes().contains(gene) }

//        val filter = if (isFromInit) ActionFilter.INIT else ActionFilter.NO_INIT

        val value = try {
            if (gene.isPrintable()) gene.getValueAsPrintableString() else "null"
        } catch (e: Exception) {
            "exception"
        }

        val localId = if (isFromInit || isFixedAction) null else individual.seeDynamicMainActions()
            .find { it.seeTopGenes().contains(gene) }!!.getLocalId()

        val position = when {
            isFromInit -> individual.seeInitializingActions().indexOfFirst { it.seeTopGenes().contains(gene) }
            else -> individual.seeFixedMainActions().indexOfFirst { it.seeTopGenes().contains(gene) }
        }

        val resourcePosition = (individual as? RestIndividual)?.getResourceCalls()?.indexOfFirst {
            it.seeActions(ActionFilter.ALL).any { d -> d.seeTopGenes().contains(gene) }
        }

        mutatedGene?.addMutatedGene(
            isDb = isDbInResourceCall,
            isInit = isFromInit,
            valueBeforeMutation = value,
            gene = gene,
            position = if (isFromInit || isFixedAction) position else null,
            localId = localId,
            resourcePosition = resourcePosition
        )

        val additionInfo = if (enableAGS || enableAGM) {
            val id = ImpactUtils.generateGeneId(individual, gene)
            //root gene impact
            val impact = eval.getImpact(individual, gene)
            AdditionalGeneMutationInfo(
                config.adaptiveGeneSelectionMethod,
                impact, id,
                archiveGeneSelector, archiveGeneMutator,
                eval, targets,
                fromInitialization = isFromInit, position = position, localId = localId, rootGene = gene
            )
        } else null

        if (enableAGM) {

            val effective = eval.getLast<EvaluatedIndividual<T>>(
                config.maxlengthOfHistoryForAGM,
                EvaluatedMutation.range(min = EvaluatedMutation.BETTER_THAN.value)
            ).filter {
                (if (isFromInit) it.individual.seeActions(ActionFilter.INIT) else if (isFixedAction) it.individual.seeFixedMainActions() else it.individual.seeDynamicMainActions()).isEmpty() ||
                        it.individual.findAction(isFromInit, if (position >= 0) position else null, localId)
                            ?.getName() == individual.findAction(
                    isFromInit,
                    if (position >= 0) position else null,
                    localId
                )?.getName()
            }
            val history =
                eval.getLast<EvaluatedIndividual<T>>(config.maxlengthOfHistoryForAGM, EvaluatedMutation.range())
                    .filter {
                        (if (isFromInit) it.individual.seeActions(ActionFilter.INIT) else if (isFixedAction) it.individual.seeFixedMainActions() else it.individual.seeDynamicMainActions()).isEmpty() ||
                                it.individual.findAction(isFromInit, if (position >= 0) position else null, localId)
                                    ?.getName() == individual.findAction(
                            isFromInit,
                            if (position >= 0) position else null,
                            localId
                        )?.getName()
                    }


            additionInfo!!.effectiveHistory.addAll(effective.mapNotNull {
                val action = it.individual.findAction(isFromInit, if (position >= 0) position else null, localId)
                if (action != null)
                    ImpactUtils.findMutatedGene(action, gene, includeSameValue)
                else if (!individual.hasAnyAction())
                    ImpactUtils.findMutatedGene(it.individual.seeGenes(), gene, includeSameValue)
                else
                    null
            })

            additionInfo.history.addAll(history.mapNotNull { e ->
                val action = e.individual.findAction(isFromInit, if (position >= 0) position else null, localId)
                (if (action != null)
                    ImpactUtils.findMutatedGene(action, gene, includeSameValue)
                else if (!e.individual.hasAnyAction())
                    ImpactUtils.findMutatedGene(e.individual.seeGenes(), gene, includeSameValue)
                else null)?.run {
                    this to EvaluatedInfo(
                        index = e.index,
                        result = e.evaluatedResult,
                        targets = e.fitness.getViewOfData().keys,
                        specificTargets = if (!isFromInit) e.fitness.getTargetsByAction(position) else setOf()
                    )
                }
            })
        }

        return additionInfo
    }
}