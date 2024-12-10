package org.evomaster.core.search.structuralelement.action

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.Column
import org.evomaster.core.sql.schema.ColumnDataType
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.structuralelement.StructuralElementBaseTest

class DbActionStructureTest: StructuralElementBaseTest(){
    override fun getStructuralElement(): SqlAction {
        val key =  Column("key", ColumnDataType.INTEGER, 10,
            primaryKey = true,
            databaseType = DatabaseType.H2)
        val date =  Column("date", ColumnDataType.DATE, databaseType = DatabaseType.H2)
        val info = Column("info", ColumnDataType.JSON, databaseType = DatabaseType.H2)
        val table = Table("foo", setOf(key, date, info), setOf())

        val gk0 = SqlPrimaryKeyGene(key.name, table.name, IntegerGene(key.name, 1), 1)
        val gd0 = DateGene(date.name)
        val gj0 = ObjectGene(info.name, listOf(DateGene("field1"), IntegerGene("field2", 2)))
        return SqlAction(table, setOf(key, date, info), 0L, listOf(gk0, gd0, gj0))

    }

    override fun getExpectedChildrenSize() = 3
}