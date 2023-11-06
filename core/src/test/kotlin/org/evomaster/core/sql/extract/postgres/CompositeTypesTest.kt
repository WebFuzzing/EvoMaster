package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
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
        val complexCompositeType = schema.compositeTypes.find { it.name.equals("complex".lowercase()) }!!
        val rColumn = complexCompositeType.columns[0]
        val iColumn = complexCompositeType.columns[1]

        assertTrue(rColumn.name.equals("r".lowercase()))
        assertTrue(iColumn.name.equals("i".lowercase()))

        assertTrue(rColumn.nullable)
        assertTrue(iColumn.nullable)

        assertTrue(schema.compositeTypes.any { it.name.equals("inventory_item".lowercase()) })
        val inventoryItemType = schema.compositeTypes.find { it.name.equals("inventory_item".lowercase()) }!!

        val addressColumn = inventoryItemType.columns[0]!!
        val supplierIdColumn = inventoryItemType.columns[1]!!
        val priceColumn = inventoryItemType.columns[2]!!

        assertTrue(addressColumn.name.equals("address".lowercase()))
        assertTrue(supplierIdColumn.name.equals("supplier_id".lowercase()))
        assertTrue(priceColumn.name.equals("price".lowercase()))

        assertTrue(addressColumn.nullable)
        assertTrue(supplierIdColumn.nullable)
        assertTrue(priceColumn.nullable)

        assertEquals(Int.MAX_VALUE, addressColumn.size)
        assertEquals(4, supplierIdColumn.size)
        assertEquals(8, priceColumn.size)


        assertTrue(schema.compositeTypes.any { it.name.equals("nested_composite_type".lowercase()) })
        val nestedCompositeType = schema.compositeTypes.find { it.name.equals("nested_composite_type".lowercase()) }!!

        val itemColumn = nestedCompositeType.columns[0]
        val countColumn = nestedCompositeType.columns[1]

        assertTrue(itemColumn.name.equals("item".lowercase()))
        assertTrue(countColumn.name.equals("count".lowercase()))

        assertTrue(itemColumn.columnTypeIsComposite)

        assertTrue(schema.tables.any { it.name.equals("on_hand".lowercase()) })
        val onHandTable = schema.tables.find { it.name.equals("on_hand".lowercase()) }!!

        assertTrue(onHandTable.columns[0].name.equals("item".lowercase()))
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

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)
    }


}