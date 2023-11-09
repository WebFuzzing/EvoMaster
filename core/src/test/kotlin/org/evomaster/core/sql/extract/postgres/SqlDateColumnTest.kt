package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.sql.Date


/**
 * Created by jgaleotti on 29-May-19.
 */
class SqlDateColumnTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/date_column_db.sql"

    @Test
    fun testExtraction() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("logins", setOf("userId", "lastLogin"))
        val genes = actions[0].seeTopGenes()

        assertEquals(2, genes.size)
        assertTrue(genes[0] is SqlPrimaryKeyGene)
        assertTrue(genes[1] is DateGene)

    }

    @Test
    fun testInsertion() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("logins", setOf("userId", "lastLogin"))
        val genes = actions[0].seeTopGenes()
        val userIdValue = ((genes[0] as SqlPrimaryKeyGene).gene as IntegerGene).value
        val lastLoginDayValue = (genes[1] as DateGene).day.value
        val lastLoginMonthValue = (genes[1] as DateGene).month.value
        val lastLoginYearValue = (genes[1] as DateGene).year.value


        val query = "Select * from logins where userId=%s".format(userIdValue)

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = SqlActionTransformer.transform(actions)

        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)
        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        assertFalse(queryResultAfterInsertion.isEmpty)

        val row = queryResultAfterInsertion.seeRows()[0]
        val lastLoginActualValue = row.getValueByName("lastLogin")

        assertTrue(lastLoginActualValue is Date)
        lastLoginActualValue as Date
        assertEquals(lastLoginYearValue, lastLoginActualValue.toLocalDate().year)
        assertEquals(lastLoginMonthValue, lastLoginActualValue.toLocalDate().monthValue)
        assertEquals(lastLoginDayValue, lastLoginActualValue.toLocalDate().dayOfMonth)


    }
}