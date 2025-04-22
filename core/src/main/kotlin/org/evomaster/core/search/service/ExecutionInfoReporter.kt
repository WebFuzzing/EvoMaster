package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.sql.DatabaseExecution
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.utils.ReportWriter.wrapWithQuotation
import org.evomaster.core.utils.ReportWriter.writeByChannel
import java.nio.file.Paths

/**
 * report executed info
 */
class ExecutionInfoReporter {

    @Inject
    private lateinit var config: EMConfig

    private val executedAction: MutableList<String> = mutableListOf()

    /**
     * record the latest computation overhead between tests
     * first is the number of evaluated individual referring the index of individual generated during the search
     * second is the time cost
     */
    private var latestComputationOverhead : Pair<Int, Long>? = null

    private val executedMainAction : MutableList<String> = mutableListOf()

    private val executedSqlAction: MutableList<DatabaseExecution> = mutableListOf()

    private var hasHeader: Boolean = false

    /**
     * @param actions are endpoints to be executed
     * @param sqlExecutionInfo are sql commands produced by the [actions]
     *      key - the index of the actions
     *      value - its corresponding database execution info
     */
    fun sqlExecutionInfo(actions: List<Action>, sqlExecutionInfo: Map<Int, DatabaseExecution>){

        if (config.outputExecutedSQL == EMConfig.OutputExecutedSQL.NONE) return

        if (!hasHeader && sqlExecutionInfo.values.any { it.executionInfo.isNotEmpty() }){
            writeByChannel(
                    Paths.get(config.saveExecutedSQLToFile),
                    getRowString(arrayOf("endpoint","sqlCommand","executionTime"))+System.lineSeparator())
            hasHeader = true
        }

        sqlExecutionInfo.forEach { t, u ->
            getOneRow(actions.get(t).getName(), u, config.outputExecutedSQL == EMConfig.OutputExecutedSQL.ONCE_EXECUTED)
        }
    }

    /**
     * record main action execution info
     * @param individual to execute and evaluate during the search
     * @param executedTimes time cost
     * @param index refers to an index of individuals generated during the search
     */
    fun actionExecutionInfo(individual: Individual, executedTimes : Long?, index : Int){
        if (!config.recordExecutedMainActionInfo) return
        if (latestComputationOverhead != null)
            executedMainAction.add("${latestComputationOverhead!!.first}, ComputationOverhead , ComputationOverhead, ${latestComputationOverhead!!.second}")

        executedMainAction.addAll(individual.seeMainExecutableActions().mapIndexed {
            /*
                executed time for all actions in this individual show at the first index
             */
             aindex, action -> "$index , ${wrapWithQuotation(action.getName())} ,${wrapWithQuotation(extractActionInfo(action))} , ${wrapWithQuotation("${if (aindex == 0) executedTimes?:"" else ""}")}"
        }
        )
    }

    /**
     * record the latest computation overhead between tests
     * @param executionTime time cost ms
     * @param index refers to an index of individuals generated and evaluated during the search
     */
    fun addLatestComputationOverhead(executionTime : Long, index : Int){
        if (config.recordExecutedMainActionInfo)
            latestComputationOverhead = index to executionTime
    }

    private fun extractActionInfo(action : Action) : String{
        return try {
            action.toString()
        }catch (e : Exception){
            action.getName()
        }
    }

    /**
     * save all execution info at end of the search
     */
    fun saveAll(){
        if (config.outputExecutedSQL == EMConfig.OutputExecutedSQL.ALL_AT_END){
            executedAction.forEachIndexed { index, s ->
                getOneRow(s, executedSqlAction[index], true)
            }
        }
        if (config.recordExecutedMainActionInfo){
            outputExecutedMainActions()
        }
    }

    private fun getRowString(info: Array<String>) = info.joinToString(",")

    private fun getOneRow(action: String, sqlInfo: DatabaseExecution, output: Boolean){
        if (!output){
            executedAction.add(action)
            executedSqlAction.add(sqlInfo)
        }else{
            outputSqlExecution(action, sqlInfo)
        }
    }

    private fun outputSqlExecution(action: String, sqlInfo: DatabaseExecution){
        sqlInfo.executionInfo.forEach {
            save(getRowString(arrayOf(wrapWithQuotation(action), wrapWithQuotation(it.sqlCommand), "${it.executionTime}", "${it.threwSqlException}"))+System.lineSeparator(), true)
        }
    }

    private fun save(content:String, append: Boolean){
        writeByChannel(
            Paths.get(config.saveExecutedSQLToFile),
            content,
            append)
    }

    private fun outputExecutedMainActions(){
        writeByChannel(
            Paths.get(config.saveExecutedMainActionInfo),
            executedMainAction.joinToString(System.lineSeparator()) {it},
            false)
    }
}
