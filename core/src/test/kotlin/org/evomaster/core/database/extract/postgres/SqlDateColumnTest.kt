package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.DateGene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.SqlPrimaryKeyGene
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
        val genes = actions[0].seeGenes()

        assertEquals(2, genes.size)
        assertTrue(genes[0] is SqlPrimaryKeyGene)
        assertTrue(genes[1] is DateGene)

    }

    @Test
    fun testInsertion() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("logins", setOf("userId", "lastLogin"))
        val genes = actions[0].seeGenes()
        val userIdValue = ((genes[0] as SqlPrimaryKeyGene).gene as IntegerGene).value
        val lastLoginDayValue = (genes[1] as DateGene).day.value
        val lastLoginMonthValue = (genes[1] as DateGene).month.value
        val lastLoginYearValue = (genes[1] as DateGene).year.value


        val query = "Select * from logins where userId=%s".format(userIdValue)

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = DbActionTransformer.transform(actions)

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