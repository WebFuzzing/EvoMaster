package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.SqlInsertBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 08-Apr-22.
 */
class CompositeTypesTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_composite_types.sql"

    @Test
    fun testExtractCompositeTypes() {
        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        // domains are not composite types
        assertFalse(schema.compositeTypes.any { it.name.equals("contact_name".lowercase()) })

        // enums are not composite types
        assertTrue(schema.enumeraredTypes.any { it.name.equals("communication_channels".lowercase()) })
        assertFalse(schema.compositeTypes.any { it.name.equals("communication_channels".lowercase()) })

        assertTrue(schema.compositeTypes.any { it.name.equals("complex".lowercase()) })
        val complexCompositeType = schema.compositeTypes.find { it.name.equals("complex".lowercase()) }
        assertTrue(complexCompositeType!!.columns[0].name.equals("r".lowercase()))
        assertTrue(complexCompositeType.columns[1].name.equals("i".lowercase()))

        assertTrue(schema.compositeTypes.any { it.name.equals("inventory_item".lowercase()) })
        val inventoryItemType = schema.compositeTypes.find { it.name.equals("inventory_item".lowercase()) }
        assertTrue(inventoryItemType!!.columns[0].name.equals("name".lowercase()))
        assertTrue(inventoryItemType.columns[1].name.equals("supplier_id".lowercase()))
        assertTrue(inventoryItemType.columns[2].name.equals("price".lowercase()))

        assertTrue(schema.compositeTypes.any { it.name.equals("nested_composite_type".lowercase()) })
        val nestedCompositeType = schema.compositeTypes.find { it.name.equals("nested_composite_type".lowercase()) }
        assertTrue(nestedCompositeType!!.columns[0].name.equals("item".lowercase()))
        assertTrue(nestedCompositeType.columns[1].name.equals("count".lowercase()))

        assertTrue(nestedCompositeType.columns.find { it.name.equals("item".lowercase()) }!!.columnTypeIsComposite)

        assertTrue(schema.tables.any { it.name.equals("on_hand".lowercase()) })
        val onHandTable = schema.tables.find { it.name.equals("on_hand".lowercase()) }
        assertTrue(onHandTable!!.columns[0].name.equals("item".lowercase()))
        assertTrue(onHandTable.columns[1].name.equals("count".lowercase()))
        assertTrue(onHandTable.columns[2].name.equals("nestedColumn".lowercase()))

        assertTrue(onHandTable.columns.find { it.name.equals("item".lowercase()) }!!.isCompositeType)

        assertFalse(onHandTable.columns.find { it.name.equals("count".lowercase()) }!!.isCompositeType)
    }

    @Test
    fun testCreateCompositeType() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
                "on_hand",
                setOf(
                        "item",
                        "count",
                        "nestedColumn"
                )
        )

        val dbCommandDto = DbActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)
    }


}