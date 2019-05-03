package org.evomaster.client.java.controller.internal.db;

import org.evomaster.client.java.controller.api.dto.database.schema.*;
import org.evomaster.client.java.controller.internal.db.constraint.ConstraintUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SchemaExtractor {


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
        } else if (protocol.contains(":postgresql")){
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
        if(! tables.next()){
            tables.close();
            schemaDto.name = schemaDto.name.toLowerCase();
            tables = md.getTables(null, schemaDto.name, null, new String[]{"TABLE"});
            if(tables.next()){
                do{
                    handleTableEntry(schemaDto, md, tables, tableNames);
                } while (tables.next());
            }
        } else {
            do{
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
        ConstraintUtils.addConstraints(connection, dt, schemaDto);

        return schemaDto;
    }

    private static void handleTableEntry(DbSchemaDto schemaDto, DatabaseMetaData md, ResultSet tables, Set<String> tableNames) throws SQLException {
        TableDto tableDto = new TableDto();
        schemaDto.tables.add(tableDto);
        tableDto.name = tables.getString("TABLE_NAME");

        if (tableNames.contains(tableDto.name)) {
            /**
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
     * referenced by other columns in tables (that are not directly
     * linked)
     *
     * @param schema
     */
    private static void addForeignKeyToAutoIncrement(DbSchemaDto schema) {
        for (TableDto tableDto : schema.tables) {
            String tableName = tableDto.name;
            for (ColumnDto columnDto : tableDto.columns) {
                if (columnDto.autoIncrement == true) {
                    continue;
                }
                if (columnDto.primaryKey == false) {
                    continue;
                }
                if (!tableDto.foreignKeys.stream().anyMatch(fk -> fk.sourceColumns.contains(columnDto.name))) {
                    continue;
                }
                String columnName = columnDto.name;
                if (isFKToAutoIncrementColumn(schema, tableName, columnName)) {
                    columnDto.foreignKeyToAutoIncrement = true;
                }
            }
        }
    }

    /**
     * Returns a table DTO for a particular table name
     *
     * @param schema
     * @param tableName
     * @return
     */
    private static TableDto getTable(DbSchemaDto schema, String tableName) {
        TableDto tableDto = schema.tables.stream().filter(t -> t.name.equalsIgnoreCase(tableName)).findFirst().orElse(null);
        return tableDto;
    }


    /**
     * Checks if the given tableName.columnName column is a foreign key to an autoincrement column
     *
     * @param schema
     * @param tableName
     * @param columnName
     * @return
     */
    private static boolean isFKToAutoIncrementColumn(DbSchemaDto schema, String tableName, String columnName) {
        TableDto tableDto = getTable(schema, tableName);
        // check if column is primary key
        if (tableDto.columns.stream().anyMatch(c -> c.name.equalsIgnoreCase(columnName) && c.primaryKey)) {
            // check if the column is autoincrement (non printable)
            if (tableDto.columns.stream().anyMatch(c -> c.name.equalsIgnoreCase(columnName) && c.autoIncrement)) {
                return true;
            } else {
                // check if the column belongs to a foreign key that is non printable
                for (ForeignKeyDto fk : tableDto.foreignKeys) {
                    if (fk.sourceColumns.contains(columnName)) {
                        int positionInFKSequence = fk.sourceColumns.indexOf(columnName);
                        String targetTableName = fk.targetTable;
                        TableDto targetTableDto = getTable(schema, targetTableName);
                        String targetColumnName = targetTableDto.primaryKeySequence.get(positionInFKSequence);
                        if (isFKToAutoIncrementColumn(schema, targetTableName, targetColumnName)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

}
