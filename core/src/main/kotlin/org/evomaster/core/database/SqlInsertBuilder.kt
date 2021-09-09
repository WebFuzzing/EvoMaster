package org.evomaster.core.database

import org.evomaster.client.java.controller.api.dto.database.operations.DataRowDto
import org.evomaster.client.java.controller.api.dto.database.operations.DatabaseCommandDto
import org.evomaster.client.java.controller.api.dto.database.operations.QueryResultDto
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto
import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnFactory.createColumnFromDto
import org.evomaster.core.database.schema.ForeignKey
import org.evomaster.core.database.schema.Table
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ImmutableDataHolderGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.dbconstraint.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class SqlInsertBuilder(
        schemaDto: DbSchemaDto,
        private val dbExecutor: DatabaseExecutor? = null
) {

    /**
     * The counter used to give unique IDs to the DbActions.
     * Should not be negative
     */
    private var counter: Long = 0

    /*
        All the objects here are immutable
     */
    private val tables = mutableMapOf<String, Table>()

    private val databaseType: DatabaseType

    private val name: String

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SqlInsertBuilder::class.java)
    }


    init {
        /*
            Here, we need to transform (and validate) the input DTO
            into immutable domain objects
         */
        if (counter < 0) {
            throw IllegalArgumentException("Invalid negative counter: $counter")
        }

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
        val tableToConstraints = mutableMapOf<String, Set<TableConstraint>>()

        for (t in schemaDto.tables) {

            val tableConstraints = parseTableConstraints(t).toMutableList()

            val columns = mutableSetOf<Column>()

            for (c in t.columns) {

                if (!c.table.equals(t.name, ignoreCase = true)) {
                    throw IllegalArgumentException("Column in different table: ${c.table}!=${t.name}")
                }

                var lowerBoundForColumn: Int? = findLowerBound(tableConstraints, c)

                var upperBoundForColumn: Int? = findUpperBound(tableConstraints, c)

                val enumValuesForColumn: List<String>? = findEnumValuesForColumn(tableConstraints, c)

                val similarToPatternsForColumn: List<String>? = findSimilarToPatternsForColumn(tableConstraints, c)


                // rangeConstraints can be combined with lower/upper bound constraints
                val pair = findUpperLoweBoundOfRangeConstraints(tableConstraints, c)
                val minRangeValue = pair.first
                val maxRangeValue = pair.second
                if (minRangeValue != null) {
                    lowerBoundForColumn = maxOf(minRangeValue, lowerBoundForColumn!!)
                }

                if (maxRangeValue != null) {
                    upperBoundForColumn = minOf(maxRangeValue, upperBoundForColumn!!)
                }

                val likePatternsForColumn = findLikePatternsForColumn(tableConstraints, c)

                val column = createColumnFromDto(c, lowerBoundForColumn, upperBoundForColumn, enumValuesForColumn,
                        similarToPatternsForColumn, likePatternsForColumn, databaseType)

                columns.add(column)
            }


            tableToConstraints[t.name] = tableConstraints.toSet()
            tableToColumns[t.name] = columns
        }

        for (t in schemaDto.tables) {

            val fks = mutableSetOf<ForeignKey>()

            for (f in t.foreignKeys) {

                tableToColumns[f.targetTable]
                        ?: throw IllegalArgumentException("Foreign key for non-existent table ${f.targetTable}")

                val sourceColumns = mutableSetOf<Column>()


                for (cname in f.sourceColumns) {
                    //TODO wrong check, as should be based on targetColumns, when we ll introduce them
                    //val c = targetTable.find { it.name.equals(cname, ignoreCase = true) }
                    //        ?: throw IllegalArgumentException("Issue in foreign key: table ${f.targetTable} does not have a column called $cname")

                    val c = tableToColumns[t.name]!!.find { it.name.equals(cname, ignoreCase = true) }
                            ?: throw IllegalArgumentException("Issue in foreign key: table ${t.name} does not have a column called $cname")
                    sourceColumns.add(c)
                }

                fks.add(ForeignKey(sourceColumns, f.targetTable))
            }

            tableToForeignKeys[t.name] = fks
        }

        for (t in schemaDto.tables) {
            val table = Table(t.name,
                    tableToColumns[t.name]!!,
                    tableToForeignKeys[t.name]!!,
                    tableToConstraints[t.name]!!)
            tables[t.name] = table
        }
    }

    private fun findUpperLoweBoundOfRangeConstraints(tableConstraints: MutableList<TableConstraint>, c: ColumnDto): Pair<Int?, Int?> {
        val rangeConstraints = filterRangeConstraints(tableConstraints, c.name)
        val minRangeValue: Int?
        val maxRangeValue: Int?
        if (rangeConstraints.isNotEmpty()) {
            minRangeValue = rangeConstraints.map { c -> c.minValue }.maxOrNull()!!.toInt()
            maxRangeValue = rangeConstraints.map { c -> c.maxValue }.minOrNull()!!.toInt()
        } else {
            minRangeValue = null
            maxRangeValue = null
        }

        tableConstraints.removeAll(rangeConstraints)
        return Pair(minRangeValue, maxRangeValue)
    }

    private fun findSimilarToPatternsForColumn(tableConstraints: MutableList<TableConstraint>, c: ColumnDto): List<String>? {
        val similarToConstraints = filterSimilarToConstraints(tableConstraints, c.name)
        val similarToPatterns = if (similarToConstraints.isNotEmpty())
            similarToConstraints.map { c -> c.pattern }.toList()
        else
            null

        tableConstraints.removeAll(similarToConstraints)
        return similarToPatterns
    }

    private fun findEnumValuesForColumn(tableConstraints: MutableList<TableConstraint>, c: ColumnDto): List<String>? {
        val enumConstraints = filterEnumConstraints(tableConstraints, c.name)
        val enumValuesAsStrings = if (enumConstraints.isNotEmpty())
            enumConstraints
                    .map { c -> c.valuesAsStrings.toMutableList() }
                    .reduce { acc, it -> acc.apply { retainAll(it) } }
        else
            null

        tableConstraints.removeAll(enumConstraints)
        return enumValuesAsStrings
    }

    private fun findUpperBound(tableConstraints: MutableList<TableConstraint>, c: ColumnDto): Int? {
        val upperBounds = filterUpperBoundConstraints(tableConstraints, c.name)

        val upperBound = if (upperBounds.isNotEmpty())
            upperBounds.map { c -> c.upperBound.toInt() }.minOrNull()
        else
            null

        tableConstraints.removeAll(upperBounds)
        return upperBound
    }

    private fun findLowerBound(tableConstraints: MutableList<TableConstraint>, c: ColumnDto): Int? {
        val lowerBounds = findLowerBounds(tableConstraints, c.name)

        val lowerBound = if (lowerBounds.isNotEmpty())
            lowerBounds.map { c -> c.lowerBound.toInt() }.maxOrNull()
        else
            null

        tableConstraints.removeAll(lowerBounds)
        return lowerBound
    }

    private fun findLikePatternsForColumn(tableConstraints: MutableList<TableConstraint>, c: ColumnDto): List<String>? {
        val likePatterns: List<String>?
        val likeConstraints = filterLikeConstraints(tableConstraints, c.name)
        if (likeConstraints.size > 1)
            throw IllegalArgumentException("cannot handle table with ${likeConstraints.size} LIKE constraints")

        if (likeConstraints.isNotEmpty()) {
            val likeConstraint = likeConstraints.first()
            likePatterns = likeConstraint.patterns
        } else
            likePatterns = null


        tableConstraints.removeAll(likeConstraints)
        return likePatterns
    }

    private fun findLowerBounds(tableConstraints: List<TableConstraint>, columnName: String): List<LowerBoundConstraint> {
        return tableConstraints
                .asSequence()
                .filterIsInstance<LowerBoundConstraint>()
                .filter { c -> c.columnName.equals(columnName, true) }
                .toList()
    }

    private fun filterSimilarToConstraints(tableConstraints: List<TableConstraint>, columnName: String): List<SimilarToConstraint> {
        return tableConstraints
                .asSequence()
                .filterIsInstance<SimilarToConstraint>()
                .filter { c -> c.columnName.equals(columnName, true) }
                .toList()
    }

    private fun filterLikeConstraints(tableConstraints: List<TableConstraint>, columnName: String): List<LikeConstraint> {
        return tableConstraints
                .asSequence()
                .filterIsInstance<LikeConstraint>()
                .filter { c -> c.columnName.equals(columnName, true) }
                .toList()
    }


    private fun filterUpperBoundConstraints(tableConstraints: List<TableConstraint>, columnName: String): List<UpperBoundConstraint> {
        return tableConstraints
                .asSequence()
                .filterIsInstance<UpperBoundConstraint>()
                .filter { c -> c.columnName.equals(columnName, true) }
                .toList()

    }

    private fun filterRangeConstraints(tableConstraints: List<TableConstraint>, columnName: String): List<RangeConstraint> {
        return tableConstraints
                .asSequence()
                .filterIsInstance<RangeConstraint>()
                .filter { c -> c.columnName.equals(columnName, true) }
                .toList()

    }


    private fun filterEnumConstraints(tableConstraints: List<TableConstraint>, columnName: String): List<EnumConstraint> {
        return tableConstraints
                .filter { c -> c is EnumConstraint }
                .map { c -> c as EnumConstraint }
                .filter { c -> c.columnName.equals(columnName, true) }
                .toList()
    }

    private fun getConstraintDatabaseType(currentDatabaseType: DatabaseType): ConstraintDatabaseType {
        return when (currentDatabaseType) {
            DatabaseType.H2 -> ConstraintDatabaseType.H2
            DatabaseType.POSTGRES -> ConstraintDatabaseType.POSTGRES
            DatabaseType.DERBY -> ConstraintDatabaseType.DERBY
            DatabaseType.MYSQL -> ConstraintDatabaseType.MYSQL
            DatabaseType.MARIADB -> ConstraintDatabaseType.MARIADB
            DatabaseType.MS_SQL_SERVER -> ConstraintDatabaseType.MS_SQL_SERVER
            DatabaseType.OTHER -> ConstraintDatabaseType.OTHER
        }
    }

    private fun parseTableConstraints(t: TableDto): List<TableConstraint> {
        val tableConstraints = mutableListOf<TableConstraint>()
        val tableName = t.name
        for (sqlCheckExpression in t.tableCheckExpressions) {
            val builder = TableConstraintBuilder()
            val constraintDatabaseType = getConstraintDatabaseType(this.databaseType)
            val tableConstraint = builder.translateToConstraint(tableName, sqlCheckExpression.sqlCheckExpression, constraintDatabaseType)
            tableConstraints.add(tableConstraint)
        }
        return tableConstraints
    }

    fun isTable(tableName: String) = tables[tableName.toUpperCase()] != null || tables[tableName.toLowerCase()] != null

    private fun getTable(tableName: String): Table {
        return tables[tableName]
                ?: tables[tableName.toUpperCase()]
                ?: tables[tableName.toLowerCase()]
                ?: throw IllegalArgumentException("No table called $tableName")
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
     * Note: the created actions have default values. You might want to randomize its data before
     * using it.
     *
     * Note: names are case-sensitive, although some DBs are case-insensitive. To make
     * things even harder, there could be a mismatch in casing when inserting and then
     * reading back table/column names from schema :(
     * This is (should not) be a problem when running EM, but can be trickier when writing
     * test cases manually for EM
     */
    fun createSqlInsertionAction(
            tableName: String,
            /**
             * Which columns to create data for. Default is all, ie *.
             * Notice that more columns might be added, eg, to satisfy non-null
             * and PK constraints
             */
            columnNames: Set<String> = setOf("*"),
            /**
             * used to avoid infinite recursion
             */
            history : MutableList<String> = mutableListOf(),
            /**
             *   When adding new insertions due to FK constraints, specify if
             *   should get all columns for those new insertions, or just the minimal
             *   needed to satisfy all the constraints
             */
            forceAll : Boolean = false
    ): List<DbAction> {

        history.add(tableName)

        val table = getTable(tableName)

        val takeAll = columnNames.contains("*")

        if (takeAll && columnNames.size > 1) {
            throw IllegalArgumentException("Invalid column description: more than one entry when using '*'")
        }

        for (cn in columnNames) {
            if (cn != "*" && !table.columns.any { it.name.equals(cn, true) }) {
                throw IllegalArgumentException("No column called $cn in table $tableName")
            }
        }

        val selectedColumns = mutableSetOf<Column>()

        for (c in table.columns) {
            /*
                we need to take primaryKey even if autoIncrement.
                Point is, even if value will be set by DB, and so could skip it,
                we would still need a non-modifiable, non-printable Gene to
                store it, as we can have other Foreign Key genes pointing to it
             */

            if (takeAll || columnNames.any { it.equals(c.name, true) } || !c.nullable || c.primaryKey) {
                //TODO are there also other constraints to consider?
                selectedColumns.add(c)
            }
        }

        val insertion = DbAction(table, selectedColumns, counter++)
        if (log.isTraceEnabled){
            log.trace("create an insertion which is {} and the counter is ", insertion.getResolvedName(), counter)
        }

        val actions = mutableListOf(insertion)

        for (fk in table.foreignKeys) {

            val target = fk.targetTable
            val n = history.filter { it.equals(target, true) }.count()
            if(n >= 3 && fk.sourceColumns.all{it.nullable}){
                //TODO as a configurable parameter in EMConfig?
                continue
            }

            val pre = if(forceAll) {
                createSqlInsertionAction(target, setOf("*"), history, true)
            } else {
                createSqlInsertionAction(target, setOf(), history, false)
            }
            actions.addAll(0, pre)
        }
        if (log.isTraceEnabled){
            log.trace("create insertions and current size is", actions.size)
        }
        return actions
    }


    /**
     * Check current state of database.
     * For each row, create a DbAction containing only Primary Keys
     * and immutable data
     */
    fun extractExistingPKs(): List<DbAction> {

        if (dbExecutor == null) {
            throw IllegalStateException("No Database Executor registered for this object")
        }

        val list = mutableListOf<DbAction>()

        for (table in tables.values) {

            val pks = table.columns.filter { it.primaryKey }

            if (pks.isEmpty()) {
                /*
                    In some very special cases, it might happen that a table has no defined
                    primary key. It's rare, but it is technically legal.
                    We can just skip those tables.
                 */
                continue
            }

            val sql = formatSelect(pks.map { it.name }, table.name)

            val dto = DatabaseCommandDto()
            dto.command = sql

            val result: QueryResultDto = dbExecutor.executeDatabaseCommandAndGetQueryResults(dto)
                    ?: continue

            result.rows.forEach { r ->

                val id = counter++

                val genes = mutableListOf<Gene>()

                for (i in 0 until pks.size) {
                    val pkName = pks[i].name
                    val inQuotes = pks[i].type.shouldBePrintedInQuotes()
                    val data = ImmutableDataHolderGene(pkName, r.columnData[i], inQuotes)
                    val pk = SqlPrimaryKeyGene(pkName, table.name, data, id)
                    genes.add(pk)
                }

                val action = DbAction(table, pks.toSet(), id, genes, true)
                list.add(action)
            }
        }

        return list
    }


    /**
     * the method is used to create a select sql based on the specified [pkValues] on [tableName]
     *
     * @param tableName specified the data of table to extract
     * @param pkValues specified the values
     * @param columnIds specified the columns for the [pkValues].
     *          for the table, there might exist more than one pks, here we allow to define the specific columns for values.
     *          Note that the specified [columnIds] can be empty, then we take all pks by default
     *
     * @return DbAction has all values of columns in the row regarding [pkValues]
     *
     */
    fun extractExistingByCols(tableName: String, pkValues: DataRowDto, columnIds: List<String> = mutableListOf()): DbAction {

        if (dbExecutor == null) {
            throw IllegalStateException("No Database Executor registered for this object")
        }

        val table = tables.values.find { it.name.equals(tableName, ignoreCase = true) }
                ?: throw  IllegalArgumentException("cannot find the table by name $tableName")


        val pks = if (columnIds.isNotEmpty()) table.columns.filter { columnIds.contains(it.name) } else table.columns.filter { it.primaryKey }
        val cols = table.columns.toList()

        var row: DataRowDto? = null
        if (pks.isNotEmpty()) {


            val condition = SQLGenerator.composeAndConditions(
                    SQLGenerator.genConditions(
                            pks.map { it.name }.toTypedArray(),
                            pkValues.columnData,
                            table)
            )

            val sql = SQLGenerator.genSelect(cols.map { it.name }.toTypedArray(), table, condition)

            val dto = DatabaseCommandDto()
            dto.command = sql

            val result: QueryResultDto = dbExecutor.executeDatabaseCommandAndGetQueryResults(dto)
                    ?: throw IllegalArgumentException("rows regarding pks can not be found")
            if (result.rows.size != 1) {
                log.warn("there exist more than one rows (${result.rows.size}) with pkValues $condition")
            }
            row = result.rows.first()
        } else
            row = pkValues


        val id = counter++

        val genes = mutableListOf<Gene>()

        for (i in 0 until cols.size) {

            if (row!!.columnData[i] != "NULL") {

                val colName = cols[i].name
                val inQuotes = cols[i].type.shouldBePrintedInQuotes()

                val gene = if (cols[i].primaryKey) {
                    SqlPrimaryKeyGene(colName, table.name, ImmutableDataHolderGene(colName, row.columnData[i], inQuotes), id)
                } else {
                    ImmutableDataHolderGene(colName, row.columnData[i], inQuotes)
                }
                genes.add(gene)
            }
        }

        return DbAction(table, pks.toSet(), id, genes, true)

    }



    /**
     * get existing pks in db
     */
    fun extractExistingPKs(dataInDB: MutableMap<String, MutableList<DataRowDto>>) {

        if (dbExecutor == null) {
            throw IllegalStateException("No Database Executor registered for this object")
        }

        dataInDB.clear()

        for (table in tables.values) {
            val pks = table.columns.filter { it.primaryKey }
            val sql = formatSelect(if (pks.isEmpty()) listOf(SQLKey.ALL.key) else pks.map { it.name }, table.name)

            val dto = DatabaseCommandDto()
            dto.command = sql

            val result: QueryResultDto = dbExecutor.executeDatabaseCommandAndGetQueryResults(dto)
                    ?: continue
            dataInDB.getOrPut(table.name) { result.rows.map { it }.toMutableList() }
        }
    }


    /**
     * get table info
     */
    fun extractExistingTables(tablesMap: MutableMap<String, Table>? = null) {
        if (tablesMap != null) {
            tablesMap.clear()
            tablesMap.putAll(tables)
        }
    }


    private fun formatSelect(columnNames: List<String>, tableName: String): String{
        return "SELECT ${columnNames.joinToString(",") { formatNameInSql(it) }} FROM ${formatNameInSql(tableName)}"
    }

    private fun formatNameInSql(name: String) : String{
        return when{
            databaseType == DatabaseType.MYSQL || name == SQLKey.ALL.key  -> name
            else -> "\"$name\""
        }
    }
}
