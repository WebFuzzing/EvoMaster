package org.evomaster.core.problem.enterprise.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.Sampler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class EnterpriseSampler<T> : Sampler<T>() where T : Individual {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(EnterpriseSampler::class.java)
    }

    @Inject(optional = true)
    protected lateinit var rc: RemoteController


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

        val extraConstraints = randomness.nextBoolean(apc.getExtraSqlDbConstraintsProbability())
        val enableSingleInsertionForTable = randomness.nextBoolean(config.probOfEnablingSingleInsertionForTable)

        val chosenColumns = if(config.forceSqlAllColumnInsertion){
            setOf("*")
        } else {
            columns
        }

        val actions = sqlInsertBuilder?.createSqlInsertionAction(tableName, chosenColumns, mutableListOf(),false, extraConstraints, enableSingleInsertionForTable=enableSingleInsertionForTable)
            ?: throw IllegalStateException("No DB schema is available")
        actions.flatMap{it.seeTopGenes()}.forEach{it.doInitialize(randomness)}

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

    open fun initSqlInfo(infoDto: SutInfoDto) {
        if (infoDto.sqlSchemaDto != null && config.shouldGenerateSqlData()) {
            sqlInsertBuilder = SqlInsertBuilder(infoDto.sqlSchemaDto, rc)
            existingSqlData = sqlInsertBuilder!!.extractExistingPKs()
        }
    }

    override fun extractFkTables(tables: Set<String>): Set<String> {
        if(sqlInsertBuilder == null || tables.isEmpty()) return tables

        return sqlInsertBuilder!!.extractFkTable(tables)
    }
}