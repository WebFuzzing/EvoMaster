package org.evomaster.client.java.controller.internal.db;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.evomaster.client.java.controller.api.dto.database.schema.*;
import org.evomaster.client.java.controller.internal.db.constraint.H2Constraints;

import java.sql.*;
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
        }
        schemaDto.databaseType = dt;

        //see https://www.progress.com/blogs/jdbc-tutorial-extracting-database-metadata-via-jdbc-driver

        ResultSet tables = md.getTables(null, schemaDto.name.toUpperCase(), null, new String[]{"TABLE"});

        Set<String> tableNames = new HashSet<>();

        while (tables.next()) {

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

            ResultSet columns = md.getColumns(null, schemaDto.name.toUpperCase(), tableDto.name, null);

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

        return schemaDto;
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
     * Indicates the first column name that is a foreign key to an autoincrement column
     *
     * @param schema
     * @param tableName
     * @return
     */
    private static String getColumnNameForeignKeyToAutoIncrementColumn(DbSchemaDto schema, String tableName) {
        TableDto tableDto = getTable(schema, tableName);
        for (String pkColumnName : tableDto.primaryKeySequence) {
            if (isFKToAutoIncrementColumn(schema, tableName, pkColumnName)) {
                return pkColumnName;
            }
        }
        return null;
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


    /**
     * Appends constraints that are database specific.
     *
     * @param connection
     * @param dt
     * @param schemaDto
     * @throws Exception
     */
    private static void addConstraints(Connection connection, DatabaseType dt, DbSchemaDto schemaDto) throws SQLException, JSQLParserException {
        switch (dt) {
            case H2: {
                H2Constraints.addH2Constraints(connection, schemaDto);
                break;
            }
            case DERBY: {
                // TODO Derby
                break;
            }
            case OTHER: {
                // TODO Other
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown database type " + dt);
            }
        }
    }




}


//TODO remove once code finalized
//    QueryResult columns = SqlScriptRunner.execCommand(connection,
//                "select * from INFORMATION_SCHEMA.Columns");
//
//        /*
//            No clear check if a column is an "identity".
//            So, here we see if:
//            - it is a primary key
//            - no autoincrement
//            - has a system sequence
//         */
//        for (TableDto table : schemaDto.tables) {
//            for (ColumnDto column : table.columns) {
//                if (!column.primaryKey || column.autoIncrement) {
//                    continue;
//                }
//
//                DataRow descriptor = columns.seeRows().stream()
//                        .filter(r -> table.name.equalsIgnoreCase(r.getValueByName("table_name").toString())
//                                && column.name.equalsIgnoreCase(r.getValueByName("column_name").toString()))
//                        .findFirst().get();
//
//                String sequenceName = descriptor.getValueByName("sequence_name").toString();
//                if(sequenceName != null && sequenceName.startsWith("SYSTEM_SEQUENCE_")){
//                    column.identity = true;
//                }
//            }
//        }