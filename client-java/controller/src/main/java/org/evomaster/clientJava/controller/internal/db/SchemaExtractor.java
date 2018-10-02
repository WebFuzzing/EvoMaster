package org.evomaster.clientJava.controller.internal.db;

import org.evomaster.clientJava.controllerApi.dto.database.schema.*;

import java.sql.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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

        Set<String> tableNames = new HashSet<String>();

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
            ResultSet rsPK = md.getPrimaryKeys(null, null, tableDto.name);
            while (rsPK.next()) {
                pks.add(rsPK.getString("COLUMN_NAME"));
            }
            rsPK.close();


            ResultSet columns = md.getColumns(null, schemaDto.name.toUpperCase(), tableDto.name, null);

            Set<String> columnNames = new HashSet<String>();
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
            JDBC MetaData is quite limited.
            To check constraints, we need to do SQL queries on the system tables.
            Unfortunately, this is database-dependent
         */
        addH2Constraints(connection, dt, schemaDto);

        return schemaDto;
    }

    private static void addH2Constraints(Connection connection, DatabaseType dt, DbSchemaDto schemaDto) throws Exception {
        switch (dt) {
            case H2: {
                addH2Constraints(connection, schemaDto);
                break;
            }
            case DERBY: {
                //TODO Derby
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

    /**
     * Expects the schema explained in
     * http://www.h2database.com/html/systemtables.html#information_schema
     *
     * @param connection a connection to a H2 database
     * @param schemaDto  a DTO schema with retrieved information from the JBDC metada
     * @throws Exception
     */
    private static void addH2Constraints(Connection connection, DbSchemaDto schemaDto) throws Exception {

        addH2ColumnConstraints(connection, schemaDto);

        addH2TableConstraints(connection, schemaDto);

    }

    /**
     * For each table in the schema DTO, this method appends
     * the constraints that are originated in the ALTER TABLE commands
     * for those particular tables.
     * <p>
     * Foreign keys are handled separately in the JDBC metadata
     *
     * @param connection
     * @param schemaDto
     * @throws SQLException
     */
    private static void addH2TableConstraints(Connection connection, DbSchemaDto schemaDto) throws SQLException {

        String tableSchema = schemaDto.name;
        for (TableDto tableDto : schemaDto.tables) {
            String tableName = tableDto.name;
            Statement statement = connection.createStatement();
            ResultSet constraints = statement.executeQuery("Select * From INFORMATION_SCHEMA.CONSTRAINTS "
                    + " where CONSTRAINTS.TABLE_SCHEMA='" + tableSchema + "' and CONSTRAINTS.TABLE_NAME='" + tableName + "'");

            while (constraints.next()) {
                String tableCatalog = constraints.getString("TABLE_CATALOG");
                String constraintCatalog = constraints.getString("CONSTRAINT_CATALOG");
                String constraintSchema = constraints.getString("CONSTRAINT_SCHEMA");
                String constraintName = constraints.getString("CONSTRAINT_NAME");
                String constraintType = constraints.getString("CONSTRAINT_TYPE");
                String uniqueIndexName = constraints.getString("UNIQUE_INDEX_NAME");
                String checkExpression = constraints.getString("CHECK_EXPRESSION");
                String columnList = constraints.getString("COLUMN_LIST");

                if (constraintType.equals("UNIQUE")) {
                    assert (checkExpression == null);
                    String[] uniqueColumnNames = columnList.split(",");
                    for (int i = 0; i < uniqueColumnNames.length; i++) {
                        String columnName = uniqueColumnNames[i].trim();
                        addUniqueConstraintToColumn(tableName, tableDto, columnName);
                    }
                } else if (constraintType.equals("REFERENTIAL")) {
                    /**
                     * This type of constraint is already handled by
                     * JDBC Metadata
                     **/
                    continue;
                } else if (constraintType.equals("PRIMARY KEY")) {
                    /**
                     * This type of constraint is already handled by
                     * JDBC Metadata
                     **/
                    continue;
                } else if (constraintType.equals("CHECK")) {
                    assert (columnList == null);
                    addH2CheckConstraint(tableDto, checkExpression);
                } else {
                    throw new RuntimeException("Unknown constraint type : " + constraintType);
                }

            }

            statement.close();

        }

    }

    /**
     * Parsers a check expression and appends the constraint to the TableDto
     *
     * @param tableDto
     * @param checkExpression
     */
    private static void addH2CheckConstraint(TableDto tableDto, String checkExpression) {

    }

    /**
     * Appends all Column constraints (i.e. CHECK contraints) to the DTO
     *
     * @param connection
     * @param schemaDto
     * @throws SQLException
     */
    private static void addH2ColumnConstraints(Connection connection, DbSchemaDto schemaDto) throws SQLException {
        String tableSchema = schemaDto.name;
        for (TableDto tableDto : schemaDto.tables) {
            String tableName = tableDto.name;
            Statement statement = connection.createStatement();
            ResultSet columns = statement.executeQuery("Select * From INFORMATION_SCHEMA.COLUMNS "
                    + " where COLUMNS.TABLE_SCHEMA='" + tableSchema + "' and COLUMNS.TABLE_NAME='" + tableName + "'");
            while (columns.next()) {
                String tableCatalog = columns.getString("TABLE_CATALOG");
                String columnName = columns.getString("COLUMN_NAME");

                String checkConstraint = columns.getString("CHECK_CONSTRAINT");
                if (checkConstraint != null && !checkConstraint.equals("")) {
                    addH2CheckConstraint(tableDto, checkConstraint);
                }
            }

            statement.close();
        }
    }

    /**
     * Adds a unique constriant to the correspondinding ColumnDTO for the selected table.column pair.
     * Requires the ColumnDTO to be contained in the TableDTO.
     * If the column DTO is not contained, a IllegalArgumentException is thrown.
     **/
    private static void addUniqueConstraintToColumn(String tableName, TableDto tableDto, String columnName) {
        ColumnDto columnDto = tableDto.columns.stream().filter(c -> c.name.equals(columnName)).findAny().orElse(null);
        if (columnDto == null) {
            throw new IllegalArgumentException("Missing column DTO for column:" + tableName + "." + columnName);
        }
        columnDto.unique = true;
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