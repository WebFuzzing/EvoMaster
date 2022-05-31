package org.evomaster.core.problem.api.service

import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.Sampler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * abstract sampler for handling API based SUT, such as REST, GraphQL, RPC
 */
abstract class ApiWsSampler<T> : Sampler<T>() where T : Individual {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ApiWsSampler::class.java)
    }

    var sqlInsertBuilder: SqlInsertBuilder? = null
        protected set

    var existingSqlData : List<DbAction> = listOf()
        protected set



    protected fun updateConfigBasedOnSutInfoDto(infoDto: SutInfoDto) {
        if (config.outputFormat == OutputFormat.DEFAULT) {
            try {
                val format = OutputFormat.valueOf(infoDto.defaultOutputFormat?.toString()!!)
                config.outputFormat = format
            } catch (e: Exception) {
                throw SutProblemException("Failed to use test output format: " + infoDto.defaultOutputFormat)
            }
        }

        // only check this configuration if DB exists
        if (infoDto.sqlSchemaDto != null){
            val employSDB = infoDto.sqlSchemaDto?.employSmartDbClean == true
            if (config.employSmartDbClean == null){
                config.employSmartDbClean = employSDB
            } else if (config.employSmartDbClean != employSDB){
                throw SutProblemException("Mismatched EmploySmartDbClean configuration between EvoMaster(${config.employSmartDbClean}) and EM Driver ($employSDB)")
            }
        }
    }

    fun sampleSqlInsertion(tableName: String, columns: Set<String>): List<DbAction> {

        val actions = sqlInsertBuilder?.createSqlInsertionAction(tableName, columns)
            ?: throw IllegalStateException("No DB schema is available")

        DbActionUtils.randomizeDbActionGenes(actions, randomness)
        //FIXME need proper handling of intra-gene constraints
        actions.forEach { it.seeGenes().forEach { g -> g.markAllAsInitialized() } }

        if (log.isTraceEnabled){
            log.trace("at sampleSqlInsertion, {} insertions are added, and they are {}", actions.size,
                actions.joinToString(",") {
                    if (it is DbAction) it.getResolvedName() else it.getName()
                })
        }

        return actions
    }

    fun canInsertInto(tableName: String) : Boolean {
        //TODO might need to refactor/remove once we deal with VIEWs
        return sqlInsertBuilder?.isTable(tableName) ?: false
    }

    abstract fun initSqlInfo(infoDto: SutInfoDto)

    override fun extractFkTables(tables: Set<String>): Set<String> {
        if(sqlInsertBuilder == null || tables.isEmpty()) return tables

        return sqlInsertBuilder!!.extractFkTable(tables)
    }

}