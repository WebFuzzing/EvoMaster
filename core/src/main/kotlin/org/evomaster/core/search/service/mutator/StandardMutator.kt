package org.evomaster.core.search.service.mutator

import org.evomaster.core.EMConfig.GeneMutationStrategy.ONE_OVER_N
import org.evomaster.core.EMConfig.GeneMutationStrategy.ONE_OVER_N_BIASED_SQL
import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.graphql.GraphQLUtils
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.UpdateForBodyParam
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.Individual.GeneFilter.ALL
import org.evomaster.core.search.Individual.GeneFilter.NO_SQL
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.EvaluatedInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.max

/**
 * make the standard mutator open for extending the mutator,
 *
 * e.g., in order to handle resource rest individual
 */
open class StandardMutator<T> : Mutator<T>() where T : Individual {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(StandardMutator::class.java)
    }

    override fun doesStructureMutation(individual : T): Boolean {
        /**
         * disable structure mutation (add/remove) during focus search
         */
        if (config.disableStructureMutationDuringFocusSearch && apc.doesFocusSearch()){return false}

        return structureMutator.canApplyStructureMutator(individual) &&
                config.maxTestSize > 1 && // if the maxTestSize is 1, there is no point to do structure mutation
                randomness.nextBoolean(config.structureMutationProbability)
    }

    override fun genesToMutation(individual: T, evi: EvaluatedIndividual<T>, targets: Set<Int>) : List<Gene> {
        val filterMutate = if (config.generateSqlDataWithSearch) ALL else NO_SQL
        return individual.seeGenes(filterMutate).filter { it.isMutable() }
    }

    override fun selectGenesToMutate(individual: T, evi: EvaluatedIndividual<T>, targets: Set<Int>, mutatedGenes: MutatedGeneSpecification?) : List<Gene>{
        val genesToMutate = genesToMutation(individual, evi, targets)
        if(genesToMutate.isEmpty()) return mutableListOf()

        val filterN = when (config.geneMutationStrategy) {
            ONE_OVER_N -> ALL
            ONE_OVER_N_BIASED_SQL -> NO_SQL
        }
        val mutated = mutableListOf<Gene>()

        if(!config.weightBasedMutationRate){
            val p = 1.0/ max(1, individual.seeGenes(filterN).filter { genesToMutate.contains(it) }.size)
            while (mutated.isEmpty()){
                genesToMutate.forEach { g->
                    if (randomness.nextBoolean(p))
                        mutated.add(g)
                }
            }
        }else{
            val enableAPC = config.weightBasedMutationRate && archiveGeneSelector.applyArchiveSelection()
            val noSQLGenes = individual.seeGenes(NO_SQL).filter { genesToMutate.contains(it) }
            val sqlGenes = genesToMutate.filterNot { noSQLGenes.contains(it) }
            while (mutated.isEmpty()){
                if (config.specializeSQLGeneSelection && noSQLGenes.isNotEmpty() && sqlGenes.isNotEmpty()){
                    mutated.addAll(mwc.selectSubGene(noSQLGenes, enableAPC, targets, null, individual, evi, forceNotEmpty = false, numOfGroup = 2))
                    mutated.addAll(mwc.selectSubGene(sqlGenes, enableAPC, targets, null, individual, evi, forceNotEmpty = false, numOfGroup = 2))
                }else{
                    mutated.addAll(mwc.selectSubGene(genesToMutate, enableAPC, targets, null, individual, evi, forceNotEmpty = false))
                }
            }
        }
        return mutated
    }

    private fun mutationPreProcessing(individual: T){

        for(a in individual.seeActions()){
            if(a !is RestCallAction){
                continue
            }
            val update = a.parameters.find { it is UpdateForBodyParam } as? UpdateForBodyParam
            if(update != null){
                a.parameters.removeIf{ it is BodyParam}
                a.parameters.removeIf{ it is UpdateForBodyParam}
                a.parameters.add(update.body)
            }

            a.seeGenes().flatMap { it.flatView()}
                    .filterIsInstance<OptionalGene>()
                    .filter { it.selectable && it.requestSelection }
                    .forEach{ it.isActive = true; it.requestSelection = false}
        }
    }

    private fun innerMutate(individual: EvaluatedIndividual<T>, targets: Set<Int>, mutatedGene: MutatedGeneSpecification?) : T{

        val copy = individual.individual.copy() as T


        if(doesStructureMutation(individual.individual)){
            if (log.isTraceEnabled){
                log.trace("structure mutator will be applied")
            }
            structureMutator.mutateStructure(copy, mutatedGene)
            return copy
        }

        mutationPreProcessing(copy)

        val allGenes = copy.seeGenes().flatMap { it.flatView() }

        val selectGeneToMutate = selectGenesToMutate(copy, individual, targets, mutatedGene)

        if(selectGeneToMutate.isEmpty())
            return copy

        for (gene in selectGeneToMutate){

            val adaptive = randomness.nextBoolean(config.probOfArchiveMutation)

            // enable weight based mutation when mutating gene
            val enableWGS = config.weightBasedMutationRate && config.enableWeightBasedMutationRateSelectionForGene
            // enable gene selection when mutating gene, eg, ObjectGene
            val enableAGS = enableWGS && adaptive && config.isEnabledArchiveGeneSelection()
            // enable gene mutation based on history
            val enableAGM = adaptive && config.isEnabledArchiveGeneMutation()

            val selectionStrategy = when {
                enableAGS -> SubsetGeneSelectionStrategy.ADAPTIVE_WEIGHT
                enableWGS && !enableAGS -> SubsetGeneSelectionStrategy.DETERMINISTIC_WEIGHT
                else -> SubsetGeneSelectionStrategy.DEFAULT
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

            gene.standardMutation(randomness, apc, mwc, allGenes, selectionStrategy, enableAGM, additionalGeneMutationInfo = additionInfo)
        }
        return copy
    }

    override fun mutate(individual: EvaluatedIndividual<T>, targets: Set<Int>, mutatedGenes: MutatedGeneSpecification?): T {

        //  mutate the individual
        val mutatedIndividual = innerMutate(individual, targets, mutatedGenes)

        postActionAfterMutation(mutatedIndividual, mutatedGenes)

        if (config.trackingEnabled()) tag(mutatedIndividual, time.evaluatedIndividuals)

        return mutatedIndividual
    }

    override fun postActionAfterMutation(mutatedIndividual: T, mutated: MutatedGeneSpecification?) {

        Lazy.assert {
            DbActionUtils.verifyForeignKeys(
                    mutatedIndividual.seeInitializingActions().filterIsInstance<DbAction>())
        }

        Lazy.assert {
            mutatedIndividual.seeActions()
                    .flatMap { it.seeGenes() }
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
        if(mutatedIndividual is GraphQLIndividual) {
            GraphQLUtils.repairIndividual(mutatedIndividual)
        }

        if (!mutatedIndividual.verifyBindingGenes()){
            mutatedIndividual.cleanBrokenBindingReference()
            Lazy.assert { mutatedIndividual.verifyBindingGenes() }
        }

        if (mutatedIndividual is RestIndividual)
            mutatedIndividual.repairDbActionsInCalls()
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
            eval : EvaluatedIndividual<T>,
            enableAGS : Boolean,
            enableAGM: Boolean,
            targets: Set<Int>, mutatedGene: MutatedGeneSpecification?, includeSameValue : Boolean = false) : AdditionalGeneMutationInfo?{

        val isFromInit = individual.seeInitializingActions().any { it.seeGenes().contains(gene) }
        val isDbInResourceCall = (individual as? RestIndividual)?.getResourceCalls()?.any {
            it.seeActions(ActionFilter.ONLY_SQL).any { d-> d.seeGenes().contains(gene) }
        }?:false

        val filter = if (isFromInit) ActionFilter.INIT else ActionFilter.NO_INIT

        val value = try {
            if(gene.isPrintable()) gene.getValueAsPrintableString() else "null"
        } catch (e: Exception){
            "exception"
        }
        val position = when {
            isFromInit -> individual.seeInitializingActions().indexOfFirst { it.seeGenes().contains(gene) }
            individual.seeActions(filter).isEmpty() -> individual.seeGenes().indexOf(gene)
            else -> individual.seeActions(ActionFilter.NO_INIT).indexOfFirst { it.seeGenes().contains(gene) }
        }

        val resourcePosition = (individual as? RestIndividual)?.getResourceCalls()?.indexOfFirst {
            it.seeActions(ActionFilter.ALL).any { d-> d.seeGenes().contains(gene) }
        }

        mutatedGene?.addMutatedGene(isFromInit || isDbInResourceCall, valueBeforeMutation = value, gene = gene, position = position, resourcePosition = resourcePosition)

        val additionInfo = if(enableAGS || enableAGM){
            val id = ImpactUtils.generateGeneId(individual, gene)
            //root gene impact
            val impact = eval.getImpact(individual, gene)
            AdditionalGeneMutationInfo(
                    config.adaptiveGeneSelectionMethod, impact, id, archiveGeneSelector, archiveGeneMutator, eval,targets, fromInitialization = isFromInit, position = position, rootGene = gene)
        }else null

        if (enableAGM){
            /*
                TODO might conduct further experiment on the 'maxlengthOfHistoryForAGM'?
             */
            val effective = eval.getLast<EvaluatedIndividual<T>>(config.maxlengthOfHistoryForAGM, EvaluatedMutation.range(min = EvaluatedMutation.BETTER_THAN.value)).filter {
                it.individual.seeActions(filter).isEmpty() ||
                        (it.individual.seeActions(filter).size > position && it.individual.seeActions(filter)[position].getName() == individual.seeActions(filter)[position].getName())
            }
            val history = eval.getLast<EvaluatedIndividual<T>>(config.maxlengthOfHistoryForAGM, EvaluatedMutation.range()).filter {
                it.individual.seeActions(filter).isEmpty() ||
                        (it.individual.seeActions(filter).size > position && it.individual.seeActions(filter)[position].getName() == individual.seeActions(filter)[position].getName())
            }


            additionInfo!!.effectiveHistory.addAll(effective.mapNotNull {
                if (it.individual.seeActions(filter).isEmpty())
                    /*
                        if there exist actions structure and the group (e.g., dbInitialization) of actions is empty,
                        we do not find further possible impacts for it
                     */
                    if(it.individual.hasAnyAction()) null
                    else ImpactUtils.findMutatedGene(it.individual.seeGenes(), gene, includeSameValue)
                else
                    ImpactUtils.findMutatedGene(
                        it.individual.seeActions(filter)[position], gene, includeSameValue)
            })

            additionInfo.history.addAll(history.mapNotNull {e->
                if (e.individual.seeActions(filter).isEmpty())
                    /*
                        if there exist actions structure and the group (e.g., dbInitialization) of actions is empty,
                        we do not find further possible impacts for it
                     */
                    if(e.individual.hasAnyAction()) null
                    else  ImpactUtils.findMutatedGene(
                           e.individual.seeGenes(), gene, includeSameValue)?.run {
                        this to EvaluatedInfo(
                                index =  e.index,
                                result = e.evaluatedResult,
                                targets = e.fitness.getViewOfData().keys,
                                specificTargets = if (!isFromInit) e.fitness.getTargetsByAction(position) else setOf()
                        )
                    }
                else
                    ImpactUtils.findMutatedGene(
                            e.individual.seeActions(filter)[position], gene, includeSameValue)?.run {
                        this to EvaluatedInfo(
                                index =  e.index,
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