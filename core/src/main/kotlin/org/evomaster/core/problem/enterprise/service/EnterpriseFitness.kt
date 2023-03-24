package org.evomaster.core.problem.enterprise.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.client.java.controller.api.dto.HeuristicEntryDto
import org.evomaster.client.java.controller.api.dto.TestResultsDto
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.core.StaticCounter
import org.evomaster.core.database.DatabaseExecution
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionResult
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.api.service.ApiWsFitness
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.sql.SqlAutoIncrementGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.service.ExtraHeuristicsLogger
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.core.taint.TaintAnalysis
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class EnterpriseFitness<T> : FitnessFunction<T>() where T : Individual {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(EnterpriseFitness::class.java)
    }

    @Inject(optional = true)
    protected lateinit var rc: RemoteController

    @Inject
    protected lateinit var extraHeuristicsLogger: ExtraHeuristicsLogger

    @Inject
    protected lateinit var searchTimeController: SearchTimeController


    /**
     * @param allDbActions specified the db actions to be executed
     * @param sqlIdMap indicates the map id of pk to generated id
     * @param allSuccessBefore indicates whether all SQL before this [allDbActions] are executed successfully
     * @param previous specified the previous db actions which have been executed
     * @return whether [allDbActions] execute successfully
     */
    fun doDbCalls(allDbActions : List<DbAction>,
                  sqlIdMap : MutableMap<Long, Long> = mutableMapOf(),
                  allSuccessBefore : Boolean = true,
                  previous: MutableList<DbAction> = mutableListOf(),
                  actionResults: MutableList<ActionResult>
    ) : Boolean {

        if (allDbActions.isEmpty()) {
            return true
        }

        val dbresults = (allDbActions.indices).map { DbActionResult() }
        actionResults.addAll(dbresults)

        if (allDbActions.none { !it.representExistingData }) {
            /*
                We are going to do an initialization of database only if there
                is data to add.
                Note that current data structure also keeps info on already
                existing data (which of course should not be re-inserted...)
             */
            // other dbactions might bind with the representExistingData, so we still need to record sqlId here.
            allDbActions.filter { it.representExistingData }.flatMap { it.seeTopGenes() }.filterIsInstance<SqlPrimaryKeyGene>().forEach {
                sqlIdMap.putIfAbsent(it.uniqueId, it.uniqueId)
            }
            previous.addAll(allDbActions)
            return true
        }

        val startingIndex = allDbActions.indexOfLast { it.representExistingData } + 1
        val dto = try {
            DbActionTransformer.transform(allDbActions, sqlIdMap, previous)
        }catch (e : IllegalArgumentException){
            // the failure might be due to previous failure
            if (!allSuccessBefore){
                previous.addAll(allDbActions)
                return false
            } else
                throw e
        }
        dto.idCounter = StaticCounter.getAndIncrease()

        val sqlResults = rc.executeDatabaseInsertionsAndGetIdMapping(dto)
        val map = sqlResults?.idMapping
        val executedResults = sqlResults?.executionResults

        if ((executedResults?.size?:0) > allDbActions.size)
            throw IllegalStateException("incorrect insertion execution results (${executedResults!!.size}) which is more than the size of insertions (${allDbActions.size}).")
        executedResults?.forEachIndexed { index, b ->
            dbresults[startingIndex+index].setInsertExecutionResult(b)
        }
        previous.addAll(allDbActions)


        if (map == null) {
            LoggingUtil.uniqueWarn(log, "Failed in executing database command")
            return false
        }else{
            val expected = allDbActions.filter { !it.representExistingData }
                .flatMap { it.seeTopGenes() }.flatMap { it.flatView() }
                .filterIsInstance<SqlPrimaryKeyGene>()
                .filter { it.gene is SqlAutoIncrementGene }
                .filterNot { it.gene is SqlForeignKeyGene }
            val missing = expected.filterNot {
                map.containsKey(it.uniqueId)
            }
            sqlIdMap.putAll(map)
            if (missing.isNotEmpty()){
                log.warn("can not get sql ids for {} from sut", missing.map { "${it.uniqueId} of ${it.tableName}" }.toSet().joinToString(","))
                return false
            }
        }
        return true
    }

    protected fun registerNewAction(action: Action, index: Int){
        rc.registerNewAction(getActionDto(action, index))
    }

    /**
     * @return dto of an action based on specified [action] and [index]
     */
    protected open fun getActionDto(action: Action, index: Int): ActionDto {
        return ActionDto().apply {
            this.index = index
            //for now, we only include specialized regex
            this.inputVariables = TaintAnalysis.getRegexTaintedValues(action)
            this.name = action.getName()
        }
    }

    protected fun updateFitnessAfterEvaluation(targets: Set<Int>, individual: T, fv: FitnessValue) : TestResultsDto?{
        val ids = targetsToEvaluate(targets, individual)

        val dto = rc.getTestResults(ids)
        if (dto == null) {
            log.warn("Cannot retrieve coverage")
            return null
        }

        dto.targets.forEach { t ->

            if (t.descriptiveId != null) {

                val noMethodReplacement = !config.useMethodReplacement && t.descriptiveId.startsWith(ObjectiveNaming.METHOD_REPLACEMENT)
                val noNonIntegerReplacement = !config.useNonIntegerReplacement && t.descriptiveId.startsWith(
                    ObjectiveNaming.NUMERIC_COMPARISON)

                if (noMethodReplacement || noNonIntegerReplacement) {
                    return@forEach
                }

                idMapper.addMapping(t.id, t.descriptiveId)
            }

            fv.updateTarget(t.id, t.value, t.actionIndex)
        }

        return dto
    }

    protected fun handleExtra(dto: TestResultsDto, fv: FitnessValue) {

        if(!config.isMIO()){
            return
        }

        if (configuration.heuristicsForSQL) {

            for (i in 0 until dto.extraHeuristics.size) {

                val extra = dto.extraHeuristics[i]

                //TODO handling of toMaximize as well
                //TODO refactoring when will have other heuristics besides for SQL

                extraHeuristicsLogger.writeHeuristics(extra.heuristics, i)

                val toMinimize = extra.heuristics
                    .filter {
                        it != null
                                && it.objective == HeuristicEntryDto.Objective.MINIMIZE_TO_ZERO
                    }.map { it.value }
                    .toList()

                if (!toMinimize.isEmpty()) {
                    fv.setExtraToMinimize(i, toMinimize)
                }

                fv.setDatabaseExecution(i, DatabaseExecution.fromDto(extra.databaseExecutionDto))
            }

            fv.aggregateDatabaseData()

            if (!fv.getViewOfAggregatedFailedWhere().isEmpty()) {
                searchTimeController.newIndividualsWithSqlFailedWhere()
            }
        } else if (configuration.extractSqlExecutionInfo) {
            /*
                this code here is done in previous block as well
             */
            for (i in 0 until dto.extraHeuristics.size) {
                val extra = dto.extraHeuristics[i]
                fv.setDatabaseExecution(i, DatabaseExecution.fromDto(extra.databaseExecutionDto))
            }
        }
    }
}