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

    /**
     * When genes are created, those are not necessarily initialized.
     * The reason is that some genes might depend on other genes (eg., foreign keys in SQL).
     * So, once all genes are created, we force their initialization, which will also randomize their values.
     */
    fun randomizeActionGenes(action: Action, probabilistic: Boolean = false) {
        action.randomize(randomness, false)
    }

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

}