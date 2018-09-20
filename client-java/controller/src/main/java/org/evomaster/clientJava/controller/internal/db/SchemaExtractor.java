package org.evomaster.clientJava.controller.internal.db;

import org.evomaster.clientJava.controllerApi.dto.database.schema.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SchemaExtractor {


    public static DbSchemaDto extract(Connection connection) throws Exception {

        Objects.requireNonNull(connection);

        DbSchemaDto schemaDto = new DbSchemaDto();

        try {
            schemaDto.name = connection.getSchema();
        } catch (Exception e) {
            /*
                In remote sessions, getSchema might fail.
                We do not do much with it anyway (at least for
                now), so not a big deal...
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

        ResultSet tables = md.getTables(null, null, null, new String[]{"TABLE"});

        while (tables.next()) {

            TableDto tableDto = new TableDto();
            schemaDto.tables.add(tableDto);
            tableDto.name = tables.getString("TABLE_NAME");

            Set<String> pks = new HashSet<>();
            ResultSet rsPK = md.getPrimaryKeys(null, null, tableDto.name);
            while (rsPK.next()) {
                pks.add(rsPK.getString("COLUMN_NAME"));
            }
            rsPK.close();


            ResultSet columns = md.getColumns(null, null, tableDto.name, null);

            while (columns.next()) {

                ColumnDto columnDto = new ColumnDto();
                tableDto.columns.add(columnDto);

                columnDto.table = tableDto.name;
                columnDto.name = columns.getString("COLUMN_NAME");
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
        addConstraints(connection, dt, schemaDto);

        return schemaDto;
    }

    private static void addConstraints(Connection connection, DatabaseType dt, DbSchemaDto schemaDto) throws Exception {
        if (dt == DatabaseType.H2) {
            addConstraints(connection, schemaDto);
        }
        //TODO Derby
    }

    private static void addConstraints(Connection connection, DbSchemaDto schemaDto) throws Exception {
        //TODO  Unique and Check
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