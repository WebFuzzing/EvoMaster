package org.evomaster.core.problem.enterprise.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.client.java.controller.api.dto.ExtraHeuristicEntryDto
import org.evomaster.client.java.controller.api.dto.TestResultsDto
import org.evomaster.core.StaticCounter
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.mongo.MongoDbAction
import org.evomaster.core.mongo.MongoDbActionResult
import org.evomaster.core.mongo.MongoDbActionTransformer
import org.evomaster.core.mongo.MongoExecution
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.sql.SqlAutoIncrementGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.service.ExtraHeuristicsLogger
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.core.sql.*
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
     * @param allSqlActions specified the db actions to be executed.
     *        This requires that, if any is representExistingData, they should all be at beginning of list
     * @param sqlIdMap indicates the map id of pk to generated id
     * @param allSuccessBefore indicates whether all SQL before this [allSqlActions] are executed successfully
     * @param previous specified the previous db actions which have been executed
     * @return whether [allSqlActions] execute successfully
     */
    fun doDbCalls(allSqlActions : List<SqlAction>,
                  sqlIdMap : MutableMap<Long, Long> = mutableMapOf(),
                  allSuccessBefore : Boolean = true,
                  previous: MutableList<SqlAction> = mutableListOf(),
                  actionResults: MutableList<ActionResult>
    ) : Boolean {

        if (allSqlActions.isEmpty()) {
            return true
        }

        val startingIndex = allSqlActions.indexOfLast { it.representExistingData } + 1
        if(!SqlActionUtils.verifyExistingDataFirst(allSqlActions)){
            throw IllegalArgumentException("SQLAction representing existing data are not in order")
        }

        val dbresults = (allSqlActions).map { SqlActionResult(it.getLocalId()) }
        actionResults.addAll(dbresults)

        if (allSqlActions.none { !it.representExistingData }) {
            /*
                We are going to do an initialization of database only if there
                is data to add.
                Note that current data structure also keeps info on already
                existing data (which of course should not be re-inserted...)
             */
            // other dbactions might bind with the representExistingData, so we still need to record sqlId here.
            allSqlActions.filter { it.representExistingData }
                .flatMap { it.seeTopGenes() }
                .filterIsInstance<SqlPrimaryKeyGene>()
                .forEach {
                    sqlIdMap.putIfAbsent(it.uniqueId, it.uniqueId)
                }
            previous.addAll(allSqlActions)
            return true
        }


        val dto = try {
            SqlActionTransformer.transform(allSqlActions, sqlIdMap, previous)
        }catch (e : Exception){
            // the failure might be due to previous failure
            if (!allSuccessBefore){
                previous.addAll(allSqlActions)
            } else {
                log.warn("Failed to create SQL command from internal representation: ${e.message}",e)
                assert(false)
            /*
                shouldn't happen in tests... but don't crash EM either... as
                support for SQL is still not fully
                TODO check again once we fully support FKs
             */
            }
            return false
        }
        dto.idCounter = StaticCounter.getAndIncrease()

        val sqlResults = rc.executeDatabaseInsertionsAndGetIdMapping(dto)
        val map = sqlResults?.idMapping
        val executedResults = sqlResults?.executionResults

        if ((executedResults?.size?:0) > allSqlActions.size)
            throw IllegalStateException("incorrect insertion execution results (${executedResults!!.size}) which is more than the size of insertions (${allSqlActions.size}).")
        if (executedResults != null){
            if (dbresults.size < startingIndex + executedResults.size)
                throw IllegalStateException("incorrect insertion execution results (${executedResults.size}) which is more than initialized db results (${dbresults.size}).")

        }
        executedResults?.forEachIndexed { index, b ->
            dbresults[startingIndex+index].setInsertExecutionResult(b)
        }
        previous.addAll(allSqlActions)


        if (map == null) {
            LoggingUtil.uniqueWarn(log, "Failed in executing database command")
            return false
        }else{
            val expected = allSqlActions.filter { !it.representExistingData }
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

    fun doMongoDbCalls(allDbActions: List<MongoDbAction>, actionResults: MutableList<ActionResult>) : Boolean {

        if (allDbActions.isEmpty()) {
            return true
        }

        val mongoDbResults = (allDbActions).map { MongoDbActionResult(it.getLocalId()) }
        actionResults.addAll(mongoDbResults)

        val dto = try {
            MongoDbActionTransformer.transform(allDbActions)
        }catch (e : IllegalArgumentException){
            throw e
        }

        val mongoResults = rc.executeMongoDatabaseInsertions(dto)
        val executedResults = mongoResults?.executionResults

        executedResults?.forEachIndexed { index, b ->
            mongoDbResults[index].setInsertExecutionResult(b)
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

    protected fun updateFitnessAfterEvaluation(
        targets: Set<Int>,
        allTargets: Boolean,
        fullyCovered: Boolean,
        descriptiveIds: Boolean,
        individual: T,
        fv: FitnessValue
    ) : TestResultsDto?{

        if(allTargets && targets.isNotEmpty()){
            throw IllegalArgumentException("Cannot specify all targets and a specific subset at same time")
        }
        val dto = if(allTargets){
                rc.getTestResults(ids = setOf(), fullyCovered = fullyCovered, descriptiveIds = descriptiveIds)
        } else {
            val ids = targetsToEvaluate(targets, individual)
            rc.getTestResults(ids, fullyCovered = fullyCovered, descriptiveIds = descriptiveIds)
        }

        if (dto == null) {
            log.warn("Cannot retrieve coverage")
            return null
        }

        val problems = dto.targets.filter { it.descriptiveId == null && !idMapper.hasMappingFor(it.id) }
        if(problems.isNotEmpty()){
            val update = rc.getTestResults(ids = dto.targets.map { it.id }.toSet(), descriptiveIds = true)
            if (update == null) {
                log.warn("Cannot retrieve coverage with full descriptive ids")
                return null
            }
            val np = problems.size
            val nu = update.targets.size
            if(nu != np){
                log.warn("Asked for $np updates but got $nu")
            }
            val list = update.targets.joinToString("\n") { it.descriptiveId }
            log.warn("There were ${problems.size} targets with no descriptive ids:\n$list")

            //assert(false)// FIXME should understand why happens in the tests. likely a bug somewhere... :(
            update.targets.forEach { idMapper.addMapping(it.id, it.descriptiveId) }
        }

        dto.targets.forEach { t ->

            if (t.descriptiveId != null) {

                //outdated code, now wrong
//                val noMethodReplacement = !config.useMethodReplacement && t.descriptiveId.startsWith(ObjectiveNaming.METHOD_REPLACEMENT)
//                val noNonIntegerReplacement = !config.useNonIntegerReplacement && t.descriptiveId.startsWith(
//                    ObjectiveNaming.NUMERIC_COMPARISON)
//
//                if (noMethodReplacement || noNonIntegerReplacement) {
//                    return@forEach
//                }

                idMapper.addMapping(t.id, t.descriptiveId)
            } else if(!idMapper.hasMappingFor(t.id)){
                // shouldn't really no longer happen after check above
                log.warn("No descriptive id for unknown code: ${t.id}")
                assert(false)
            }

            fv.updateTarget(t.id, t.value, t.actionIndex)
        }

        return dto
    }

    protected fun handleExtra(dto: TestResultsDto, fv: FitnessValue) {

        if (!config.isUsingAdvancedTechniques()) {
            return
        }

        if (configuration.heuristicsForSQL) {
            handleSqlHeuristics(dto, fv)
        }

        if (configuration.extractSqlExecutionInfo) {
            for (i in 0 until dto.extraHeuristics.size) {
                val extra = dto.extraHeuristics[i]
                val databaseExecution = DatabaseExecution.fromDto(extra.sqlSqlExecutionsDto)
                fv.setDatabaseExecution(i, databaseExecution)
                if (databaseExecution.sqlParseFailureCount>0) {
                    statistics.reportSqlParsingFailures(databaseExecution.sqlParseFailureCount)
                }
            }
            fv.aggregateDatabaseData()
            if (fv.getViewOfAggregatedFailedWhere().isNotEmpty()) {
                searchTimeController.newIndividualsWithSqlFailedWhere()
            }

        }

        if (configuration.heuristicsForMongo) {
            handleMongoHeuristics(dto, fv)
        }

        if (configuration.extractMongoExecutionInfo) {
            for (i in 0 until dto.extraHeuristics.size) {
                val extra = dto.extraHeuristics[i]
                fv.setMongoExecution(i, MongoExecution.fromDto(extra.mongoExecutionsDto))
            }
            fv.aggregateMongoDatabaseData()
        }
    }

    private fun handleSqlHeuristics(
        dto: TestResultsDto,
        fv: FitnessValue,
    ) {
        for (i in 0 until dto.extraHeuristics.size) {

            val extra = dto.extraHeuristics[i]

            //TODO handling of toMaximize as well
            //TODO refactoring when will have other heuristics besides for SQL

            extraHeuristicsLogger.writeHeuristics(extra.heuristics, i)

            val toMinimize = extra.heuristics
                .filter {
                    it != null
                            && it.objective == ExtraHeuristicEntryDto.Objective.MINIMIZE_TO_ZERO
                            && it.type == ExtraHeuristicEntryDto.Type.SQL
                }.map { it.value }
                .toList()

            if (!toMinimize.isEmpty()) {
                fv.setExtraToMinimize(i, toMinimize)
            }

            extra.heuristics
                .filterNotNull().forEach {
                    if (it.type == ExtraHeuristicEntryDto.Type.SQL) {

                        statistics.reportNumberOfEvaluatedRowsForSqlHeuristic(it.numberOfEvaluatedRecords)
                        if (it.extraHeuristicEvaluationFailure) {
                            statistics.reportSqlHeuristicEvaluationFailure()
                        } else {
                            statistics.reportSqlHeuristicEvaluationSuccess()
                        }
                    }
                }
        }

    }

    private fun handleMongoHeuristics(dto: TestResultsDto, fv: FitnessValue) {
        for (i in 0 until dto.extraHeuristics.size) {

            val extra = dto.extraHeuristics[i]

            extraHeuristicsLogger.writeHeuristics(extra.heuristics, i)

            val toMinimize = extra.heuristics
                .filter {
                    it != null
                            && it.objective == ExtraHeuristicEntryDto.Objective.MINIMIZE_TO_ZERO
                            && it.type == ExtraHeuristicEntryDto.Type.MONGO
                }.map { it.value }
                .toList()

            if (toMinimize.isNotEmpty()) {
                fv.setExtraToMinimize(i, toMinimize)
            }

            extra.heuristics
                .filterNotNull().forEach {
                    if (it.type == ExtraHeuristicEntryDto.Type.MONGO) {
                        statistics.reportNumberOfEvaluatedDocumentsForMongoHeuristic(it.numberOfEvaluatedRecords)
                        if (it.extraHeuristicEvaluationFailure) {
                            statistics.reportMongoHeuristicEvaluationFailure()
                        } else {
                            statistics.reportMongoHeuristicEvaluationSuccess()
                        }
                    }
                }
        }
    }
}
