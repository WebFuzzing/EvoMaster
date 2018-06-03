package org.evomaster.core.database

import org.evomaster.clientJava.controllerApi.dto.database.schema.DatabaseType
import org.evomaster.clientJava.controllerApi.dto.database.schema.DbSchemaDto
import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ForeignKey
import org.evomaster.core.database.schema.Table


class SqlInsertBuilder(schemaDto: DbSchemaDto) {

    /*
        All the objects here are immutable
     */

    private val tables = mutableMapOf<String, Table>()

    private val databaseType: DatabaseType

    private val name: String


    init {
        /*
            Here, we need to transform (and validate) the input DTO
            into immutable domain objects
         */


        if (schemaDto.databaseType == null) {
            throw IllegalArgumentException("Undefined database type")
        }
        if (schemaDto.name == null) {
            throw IllegalArgumentException("Undefined schema name")
        }

        databaseType = schemaDto.databaseType
        name = schemaDto.name

        val tableToColumns = mutableMapOf<String, MutableSet<Column>>()
        val tableToForeignKeys = mutableMapOf<String, MutableSet<ForeignKey>>()

        for (t in schemaDto.tables) {

            val columns = mutableSetOf<Column>()

            for (c in t.columns) {

                if(! c.table.equals(t.name, ignoreCase = true)){
                    throw IllegalArgumentException("Column in different table: ${c.table}!=${t.name}")
                }

                val column = Column(
                        name = c.name,
                        size = c.size,
                        type = c.type, //TODO enum mapping
                        primaryKey = c.primaryKey,
                        nullable = c.nullable,
                        unique = c.unique,
                        autoIncrement = c.autoIncrement
                )

                columns.add(column)
            }

            tableToColumns[t.name] = columns
        }

        for (t in schemaDto.tables) {

            val fks = mutableSetOf<ForeignKey>()

            for(f in t.foreignKeys){

                val targetTable = tableToColumns[f.targetTable]
                        ?: throw IllegalArgumentException("Foreign key for non-existent table ${f.targetTable}")

                val columns = mutableSetOf<Column>()


                for(cname in f.columns){
                    val c = targetTable.find { it.name.equals(cname, ignoreCase = true) }
                            ?: throw IllegalArgumentException("Issue in foreign key: table ${f.targetTable} does not have a column called $cname")

                    columns.add(c)
                }

                fks.add(ForeignKey(columns, f.targetTable))
            }

            tableToForeignKeys[t.name] = fks
        }

        for (t in schemaDto.tables) {
            val table = Table(t.name, tableToColumns[t.name]!!, tableToForeignKeys[t.name]!!)
            tables[t.name] = table
        }
    }


    /**
     * Create a SQL insertion operation into the table called [tableName].
     * Use columns only from [columnNames], to avoid wasting resources in setting
     * non-used data.
     * Note: due to constraints (eg non-null), we might create data also for non-specified columns.
     *
     * If the table has non-null foreign keys to other tables, then create an insertion for those
     * as well. This means that more than one action can be returned here.
     *
     * Note: the create action has default values. You might want to randomize its data before
     * using it.
     *
     * Note: names are case-sensitive, although some DBs are case-insensitive. To make
     * things even harder, there could be a mismatch in casing when inserting and then
     * reading back table/column names from schema :(
     * This is (should not) be a problem when running EM, but can be trickier when writing
     * test cases manually for EM
     */
    fun createSqlInsertionAction(tableName: String, columnNames: Set<String>) : List<DbAction>{

        val table = tables[tableName] ?:
                throw IllegalArgumentException("No table called $tableName")

        for(cn in columnNames){
            if(! table.columns.any{it.name == cn}){
                throw IllegalArgumentException("No column called $cn in table $tableName")
            }
        }

        val selectedColumns = mutableSetOf<Column>()

        for (c in table.columns){
            if(c.primaryKey && c.autoIncrement){
                //value will be set by DB, so skip it
                continue
            }

            if(columnNames.contains(c.name) || !c.nullable){
                //TODO are there also other constraints to consider?
                selectedColumns.add(c)
            }
        }

        val insertion = DbAction(table, selectedColumns)
        val actions = mutableListOf(insertion)

        for(fk in table.foreignKeys){
            /*
                Assumption: in a valid Schema, this should never end up
                in a infinite loop.
             */
            val pre = createSqlInsertionAction(fk.targetTable, setOf())
            actions.addAll(0, pre)
        }

        return actions
    }
}