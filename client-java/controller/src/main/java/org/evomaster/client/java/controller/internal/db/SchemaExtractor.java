package org.evomaster.client.java.controller.internal.db;

import org.evomaster.client.java.controller.api.dto.database.schema.*;
import org.evomaster.client.java.controller.internal.db.constraint.*;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class SchemaExtractor {


    public static boolean validate(DbSchemaDto schema) throws IllegalArgumentException {

        /*
            some checks if the derived schema is consistent
         */

        Objects.requireNonNull(schema);

        for (TableDto table : schema.tables) {

            for (ColumnDto column : table.columns) {
                checkForeignKeyToAutoIncrementPresent(schema, table, column);
                checkForeignKeyToAutoIncrementMissing(schema, table, column);
            }

        }

        return true;
    }

    private static void checkForeignKeyToAutoIncrementMissing(DbSchemaDto schema, TableDto table, ColumnDto column) {
        if (column.foreignKeyToAutoIncrement) {
            return;
        }

        Optional<ForeignKeyDto> fk = table.foreignKeys.stream()
                .filter(it -> it.sourceColumns.contains(column.name))
                .findFirst();

        if (!fk.isPresent()) {
            //not a foreign key
            return;
        }

        //TODO proper handling of multi-column PKs/FKs

        Optional<TableDto> targetTable = schema.tables.stream()
                .filter(t -> t.name.equals(fk.get().targetTable))
                .findFirst();

        if (!targetTable.isPresent()) {
            throw new IllegalArgumentException("Foreign key in table " + table.name +
                    " pointing to non-existent table " + fk.get().targetTable);
        }

        List<ColumnDto> pks = targetTable.get().columns.stream()
                .filter(c -> c.primaryKey)
                .collect(Collectors.toList());

        if (pks.isEmpty()) {
            throw new IllegalArgumentException("No PK in table " + targetTable.get().name + " that has FKs pointing to it");
        }

        for (ColumnDto pk : pks) {
            if (pk.autoIncrement || pk.foreignKeyToAutoIncrement) {
                throw new IllegalArgumentException("Column " + pk.name + " in table " +
                        pk.table + " is auto-increment, although FK pointing to it does not mark it " +
                        "as autoincrement in " + column.name + " in " + table.name
                );
            }
        }
    }

    private static void checkForeignKeyToAutoIncrementPresent(DbSchemaDto schema, TableDto table, ColumnDto column) {
        if (!column.foreignKeyToAutoIncrement) {
            return;
        }

        Optional<ForeignKeyDto> fk = table.foreignKeys.stream()
                .filter(it -> it.sourceColumns.contains(column.name))
                .findFirst();

        if (!fk.isPresent()) {
            throw new IllegalArgumentException("No foreign key constraint for marked column " +
                    column.name + " in table " + table.name);
        }

        //TODO proper handling of multi-column PKs/FKs

        Optional<TableDto> targetTable = schema.tables.stream()
                .filter(t -> t.name.equals(fk.get().targetTable))
                .findFirst();

        if (!targetTable.isPresent()) {
            throw new IllegalArgumentException("Foreign key in table " + table.name +
                    " pointing to non-existent table " + fk.get().targetTable);
        }

        //there should be only 1 PK, and that must be auto-increment

        List<ColumnDto> pks = targetTable.get().columns.stream()
                .filter(c -> c.primaryKey)
                .collect(Collectors.toList());

        if (pks.size() != 1) {
            throw new IllegalArgumentException("There must be only 1 PK in table " +
                    targetTable.get().name + " pointed by the FK-to-autoincrement " +
                    column.name + " in " + table.name + ". However, there were: " + pks.size());
        }

        ColumnDto pk = pks.get(0);
        if (!pk.autoIncrement && !pk.foreignKeyToAutoIncrement) {
            throw new IllegalArgumentException("Column " + pk.name + " in table " +
                    pk.table + " is not auto-increment, although FK pointing to it does mark it" +
                    "as autoincrement in " + column.name + " in " + table.name
            );
        }
    }

    public static DbSchemaDto extract(Connection connection) throws Exception {

        Objects.requireNonNull(connection);

        DbSchemaDto schemaDto = new DbSchemaDto();

        try {
            schemaDto.name = connection.getSchema();
        } catch (Exception | AbstractMethodError e) {
            /*
                In remote sessions, getSchema might fail.
                We do not do much with it anyway (at least for
                now), so not a big deal...
                Furthermore, some drivers might be compiled to Java 6,
                whereas getSchema was introduced in Java 7
             */
            schemaDto.name = "public";
        }
        DatabaseMetaData md = connection.getMetaData();

        String protocol = md.getURL(); //TODO better handling
        DatabaseType dt = DatabaseType.OTHER;
        if (protocol.contains(":h2")) {
            dt = DatabaseType.H2;
        } else if (protocol.contains(":derby")) {
            dt = DatabaseType.DERBY;
        } else if (protocol.contains(":postgresql")) {
            dt = DatabaseType.POSTGRES;
        }
        schemaDto.databaseType = dt;

        //see https://www.progress.com/blogs/jdbc-tutorial-extracting-database-metadata-via-jdbc-driver

        schemaDto.name = schemaDto.name.toUpperCase();

        ResultSet tables = md.getTables(null, schemaDto.name, null, new String[]{"TABLE"});

        Set<String> tableNames = new HashSet<>();

        /*
            Interfaces to deal with DBs are simply awful...
            Here, we first check with schema name in upper case, and, if that gives no results,
            we try with lower case... this is because different databases deal with upper/lower
            cases differently.
            But API does not give you any info on whether result set
            is empty or not, and only way is to call next()
         */
        if (!tables.next()) {
            tables.close();
            schemaDto.name = schemaDto.name.toLowerCase();
            tables = md.getTables(null, schemaDto.name, null, new String[]{"TABLE"});
            if (tables.next()) {
                do {
                    handleTableEntry(schemaDto, md, tables, tableNames);
                } while (tables.next());
            }
        } else {
            do {
                handleTableEntry(schemaDto, md, tables, tableNames);
            } while (tables.next());
        }
        tables.close();

        /*
            Mark those columns that are using auto generated values
         */
        addForeignKeyToAutoIncrement(schemaDto);

        /*
            JDBC MetaData is quite limited.
            To check constraints, we need to do SQL queries on the system tables.
            Unfortunately, this is database-dependent
         */
        addConstraints(connection, dt, schemaDto);

        assert validate(schemaDto);

        return schemaDto;
    }


    /**
     * Adds a unique constraint to the corresponding ColumnDTO for the selected table.column pair.
     * Requires the ColumnDTO to be contained in the TableDTO.
     * If the column DTO is not contained, a IllegalArgumentException is thrown.
     *
     * @param tableDto the DTO of the table
     * @param columnName the name of the column to add the unique constraint on
     **/
    public static void addUniqueConstraintToColumn(TableDto tableDto, String columnName) {

        ColumnDto columnDto = tableDto.columns.stream()
                .filter(c -> c.name.equals(columnName)).findAny().orElse(null);

        if (columnDto == null) {
            throw new IllegalArgumentException("Missing column DTO for column:" + tableDto.name + "." + columnName);
        }

        columnDto.unique = true;
    }

    /**
     * Appends constraints that are database specific.
     */
    private static void addConstraints(Connection connection, DatabaseType dt, DbSchemaDto schemaDto) throws SQLException {
        TableConstraintExtractor constraintExtractor = TableConstraintExtractorFactory.buildConstraintExtractor(dt);
        if (constraintExtractor != null) {
            final List<DbTableConstraint> dbTableConstraints = constraintExtractor.extract(connection, schemaDto);
            addConstraints(schemaDto, dbTableConstraints);
        } else {
            SimpleLogger.uniqueWarn("WARNING: EvoMaster cannot extract constraints from database " + dt);
        }

    }

    private static void addConstraints(DbSchemaDto schemaDto, List<DbTableConstraint> constraintList) {
        for (DbTableConstraint constraint : constraintList) {
            String tableName = constraint.getTableName();
            TableDto tableDto = schemaDto.tables.stream().filter(t -> t.name.equalsIgnoreCase(tableName)).findFirst().orElse(null);

            if (constraint instanceof DbTableCheckExpression) {
                TableCheckExpressionDto constraintDto = new TableCheckExpressionDto();
                final DbTableCheckExpression tableCheckExpression = (DbTableCheckExpression) constraint;
                constraintDto.sqlCheckExpression = tableCheckExpression.getSqlCheckExpression();
                tableDto.tableCheckExpressions.add(constraintDto);
            } else if (constraint instanceof DbTableUniqueConstraint) {
                DbTableUniqueConstraint tableUniqueConstraint = (DbTableUniqueConstraint) constraint;
                for (String columnName : tableUniqueConstraint.getUniqueColumnNames()) {
                    addUniqueConstraintToColumn(tableDto, columnName);
                }
            } else {
                throw new RuntimeException("Unknown constraint type " + constraint.getClass().getName());
            }

        }
    }

    private static void handleTableEntry(DbSchemaDto schemaDto, DatabaseMetaData md, ResultSet tables, Set<String> tableNames) throws SQLException {
        TableDto tableDto = new TableDto();
        schemaDto.tables.add(tableDto);
        tableDto.name = tables.getString("TABLE_NAME");

        if (tableNames.contains(tableDto.name)) {
            /*
             * Perhaps we should throw a more specific exception than IllegalArgumentException
             */
            throw new IllegalArgumentException("Cannot handle repeated table " + tableDto.name + " in schema");
        } else {
            tableNames.add(tableDto.name);
        }

        Set<String> pks = new HashSet<>();
        SortedMap<Integer, String> primaryKeySequence = new TreeMap<>();
        ResultSet rsPK = md.getPrimaryKeys(null, null, tableDto.name);


        while (rsPK.next()) {
            String pkColumnName = rsPK.getString("COLUMN_NAME");
            int positionInPrimaryKey = (int) rsPK.getShort("KEY_SEQ");
            pks.add(pkColumnName);
            int pkIndex = positionInPrimaryKey - 1;
            primaryKeySequence.put(pkIndex, pkColumnName);
        }
        rsPK.close();

        tableDto.primaryKeySequence.addAll(primaryKeySequence.values());

        ResultSet columns = md.getColumns(null, schemaDto.name, tableDto.name, null);

        Set<String> columnNames = new HashSet<>();
        while (columns.next()) {

            ColumnDto columnDto = new ColumnDto();
            tableDto.columns.add(columnDto);

            columnDto.table = tableDto.name;
            columnDto.name = columns.getString("COLUMN_NAME");

            if (columnNames.contains(columnDto.name)) {
                /**
                 * Perhaps we should throw a more specific exception than IllegalArgumentException
                 */
                throw new IllegalArgumentException("Cannot handle repeated column " + columnDto.name + " in table " + tableDto.name);
            } else {
                columnNames.add(columnDto.name);
            }

            columnDto.type = columns.getString("TYPE_NAME");
            columnDto.size = columns.getInt("COLUMN_SIZE");
            columnDto.nullable = columns.getBoolean("IS_NULLABLE");
            columnDto.autoIncrement = columns.getBoolean("IS_AUTOINCREMENT");
            //columns.getString("DECIMAL_DIGITS");

            columnDto.primaryKey = pks.contains(columnDto.name);
        }
        columns.close();


        ResultSet fks = md.getImportedKeys(null, null, tableDto.name);
        while (fks.next()) {
            //TODO need to see how to handle case of multi-columns

            ForeignKeyDto fkDto = new ForeignKeyDto();
            fkDto.sourceColumns.add(fks.getString("FKCOLUMN_NAME"));
            fkDto.targetTable = fks.getString("PKTABLE_NAME");

            tableDto.foreignKeys.add(fkDto);
        }
        fks.close();
    }

    /**
     * Sets the foreignKeyToAutoIncrement field in the Column DTO
     * when a column is a foreign key to an auto increment value.
     * This information will be needed to properly handle the
     * automatically generated values in primary keys that are
     * referenced by columns in other tables
     *
     * @param schema a DTO with the database information
     */
    private static void addForeignKeyToAutoIncrement(DbSchemaDto schema) {
        for (TableDto tableDto : schema.tables) {
            for (ColumnDto columnDto : tableDto.columns) {
                if (isFKToAutoIncrementColumn(schema, tableDto, columnDto.name)) {
                    columnDto.foreignKeyToAutoIncrement = true;
                }
            }
        }
    }

    /**
     * @return  a table DTO for a particular table name
     */
    private static TableDto getTable(DbSchemaDto schema, String tableName) {
        TableDto tableDto = schema.tables.stream()
                .filter(t -> t.name.equalsIgnoreCase(tableName))
                .findFirst().orElse(null);
        return tableDto;
    }

    private static ColumnDto getColumn(TableDto table, String columnName) {
        ColumnDto columnDto = table.columns.stream()
                .filter(c -> c.name.equalsIgnoreCase(columnName))
                .findFirst().orElse(null);
        return columnDto;
    }


    /**
     * Checks if the given table/column is a foreign key to an autoincrement column.
     * This is done to be able to compute foreignKeyToAutoIncrement boolean.
     * Otherwise, we could just read that boolean.
     *
     * @return true if the given table/column is a foreign key to an autoincrement column.
     */
    private static boolean isFKToAutoIncrementColumn(DbSchemaDto schema, TableDto tableDto, String columnName) {

        Objects.requireNonNull(schema);
        Objects.requireNonNull(tableDto);
        Objects.requireNonNull(columnName);

        // is this column among the declared FKs?
        if (!tableDto.foreignKeys.stream()
                .anyMatch(fk -> fk.sourceColumns.stream()
                        .anyMatch(s -> s.equalsIgnoreCase(columnName)))) {
            return false;
        }

        ColumnDto columnDto = getColumn(tableDto, columnName);

        if (columnDto.autoIncrement == true) {
            // Assuming here that a FK cannot be auto-increment
            return false;
        }

        // check if the column belongs to a foreign key that is non printable
        for (ForeignKeyDto fk : tableDto.foreignKeys) {
            if (fk.sourceColumns.stream()
                    .anyMatch(s -> s.equalsIgnoreCase(columnName))) {

                /*
                    TODO: instead of using those positions, should have proper
                    support for multi-column PKs/FKs
                 */
                int positionInFKSequence = fk.sourceColumns.indexOf(columnName);
                TableDto targetTableDto = getTable(schema, fk.targetTable);
                String targetColumnName = targetTableDto.primaryKeySequence.get(positionInFKSequence);
                ColumnDto targetColumnDto = getColumn(targetTableDto, targetColumnName);

                /*
                    Either that target PK is auto-increment, or itself is a FK to a non-printable PK
                 */
                if (targetColumnDto.autoIncrement ||
                        isFKToAutoIncrementColumn(schema, targetTableDto, targetColumnName)) {
                    return true;
                }
            }
        }

        return false;
    }

}
