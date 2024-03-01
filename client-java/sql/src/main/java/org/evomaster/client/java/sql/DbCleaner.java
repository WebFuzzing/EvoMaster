package org.evomaster.client.java.sql;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.h2.H2VersionUtils;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Class used to clean/reset the state of the current database
 */
public class DbCleaner {

    public static void clearDatabase_H2(Connection connection) {
        clearDatabase_H2(connection, null);
    }

    public static void clearDatabase_H2(Connection connection, List<String> tableToSkip) {
        clearDatabase_H2(connection, getDefaultSchema(DatabaseType.H2), tableToSkip);
    }

    public static void clearDatabase_H2(Connection connection, String schemaName, List<String> tableToSkip) {
        clearDatabase_H2(connection, schemaName, tableToSkip, null);
    }

    public static void clearDatabase_H2(Connection connection, String schemaName, List<String> tableToSkip, List<String> tableToClean) {
        final String h2Version;
        try {
            h2Version = H2VersionUtils.getH2Version(connection);
        } catch (SQLException e) {
            throw new RuntimeException("Unexpected SQLException while fetching H2 version", e);
        }
        /*
         * The SQL command "TRUNCATE TABLE my_table RESTART IDENTITY"
         * is not supported by H2 version 1.4.199 or lower
         */
        final boolean restartIdentitiyWhenTruncating = H2VersionUtils.isVersionGreaterOrEqual(h2Version, H2VersionUtils.H2_VERSION_2_0_0);
        clearDatabase(getDefaultRetries(DatabaseType.H2), connection, schemaName, tableToSkip, tableToClean, DatabaseType.H2,
                false, true, restartIdentitiyWhenTruncating);
    }

    /*
        [non-determinism-source] Man: retries might lead to non-determinate logs
     */
    private static void clearDatabase(int retries,
                                      Connection connection,
                                      String schemaName,
                                      List<String> tableToSkip,
                                      List<String> tableToClean,
                                      DatabaseType type,
                                      boolean doDropTable,
                                      boolean doResetSequence,
                                      boolean restartIdentityWhenTruncating) {

        /*
            Code based on
            https://stackoverflow.com/questions/8523423/reset-embedded-h2-database-periodically
         */

        try {
            Statement statement = connection.createStatement();

            /*
                For H2, we have to delete tables one at a time... but, to avoid issues
                with FKs, we must temporarily disable the integrity checks
             */
            disableReferentialIntegrity(statement, type);


            List<String> cleanedTable = cleanDataInTables(tableToSkip,
                    tableToClean,
                    statement,
                    type,
                    schemaName,
                    isSingleCleanCommand(type),
                    doDropTable,
                    restartIdentityWhenTruncating);

            if (doResetSequence) {
                List<String> sequenceToClean = null;
                if (type == DatabaseType.MYSQL || type == DatabaseType.MARIADB)
                    sequenceToClean = cleanedTable;
                resetSequences(statement, type, schemaName, sequenceToClean);
            }


            enableReferentialIntegrity(statement, type);
            statement.close();
        } catch (Exception e) {
            /*
                this could happen if there is a current transaction with a lock on any table.
                We could check the content of INFORMATION_SCHEMA.LOCKS, or simply look at error message
             */
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("timeout")) {
                if (retries > 0) {
                    SimpleLogger.warn("Timeout issue with cleaning DB. Trying again.");
                    //let's just wait a bit, and retry
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException interruptedException) {
                        // empty block
                    }
                    retries--;
                    clearDatabase(retries, connection, schemaName, tableToSkip, tableToClean, type, doDropTable, doResetSequence, restartIdentityWhenTruncating);
                } else {
                    SimpleLogger.error("Giving up cleaning the DB. There are still timeouts.");
                }
            }

            throw new RuntimeException(e);
        }
    }

    public static void clearDatabase(Connection connection, List<String> tablesToSkip, DatabaseType type, boolean doResetSequence) {
        clearDatabase(connection, getDefaultSchema(type), tablesToSkip, type, doResetSequence);
    }

    public static void clearDatabase(Connection connection, List<String> tablesToSkip, DatabaseType type) {
        clearDatabase(connection, tablesToSkip, type, true);
    }

    public static void clearDatabase(Connection connection, List<String> tableToSkip, List<String> tableToClean, DatabaseType type) {
        clearDatabase(connection, tableToSkip, tableToClean, type, true);
    }

    public static void clearDatabase(Connection connection, List<String> tableToSkip, List<String> tableToClean, DatabaseType type, boolean doResetSequence) {
        clearDatabase(connection, getDefaultSchema(type), tableToSkip, tableToClean, type, doResetSequence);
    }

    public static void clearDatabase(Connection connection, String schemaName, List<String> tablesToSkip, DatabaseType type) {
        clearDatabase(connection, getSchemaName(schemaName, type), tablesToSkip, type, true);
    }

    public static void clearDatabase(Connection connection, String schemaName, List<String> tablesToSkip, DatabaseType type, boolean doResetSequence) {
        clearDatabase(connection, getSchemaName(schemaName, type), tablesToSkip, null, type, doResetSequence);
    }

    public static void clearDatabase(Connection connection, String schemaName, List<String> tableToSkip, List<String> tableToClean, DatabaseType type) {
        clearDatabase(connection, getSchemaName(schemaName, type), tableToSkip, tableToClean, type, true);
    }

    public static void clearDatabase(Connection connection, String schemaName, List<String> tableToSkip, List<String> tableToClean, DatabaseType type, boolean doResetSequence) {
        /*
         * Enable the restarting of Identity fields only if sequences are to be restarted
         * and the database type is H2
         */
        boolean restartIdentityWhenTruncating;
        if (doResetSequence && type.equals(DatabaseType.H2)) {
            try {
                String h2Version = H2VersionUtils.getH2Version(connection);
                restartIdentityWhenTruncating = H2VersionUtils.isVersionGreaterOrEqual(h2Version, H2VersionUtils.H2_VERSION_2_0_0);
            } catch (SQLException ex) {
                throw new RuntimeException("Unexpected SQL exception while getting H2 version", ex);
            }
        } else {
            restartIdentityWhenTruncating = false;
        }
        clearDatabase(getDefaultRetries(type), connection, getSchemaName(schemaName, type), tableToSkip, tableToClean, type, false, doResetSequence, restartIdentityWhenTruncating);
    }

    public static void dropDatabaseTables(Connection connection, String schemaName, List<String> tablesToSkip, DatabaseType type) {
        if (type != DatabaseType.MYSQL && type != DatabaseType.MARIADB)
            throw new IllegalArgumentException("Dropping tables are not supported by " + type);
        clearDatabase(getDefaultRetries(type), connection, getSchemaName(schemaName, type), tablesToSkip, null, type, true, true, false);
    }


    public static void clearDatabase_Postgres(Connection connection, String schemaName, List<String> tablesToSkip) {
        clearDatabase_Postgres(connection, getSchemaName(schemaName, DatabaseType.POSTGRES), tablesToSkip, null);
    }

    public static void clearDatabase_Postgres(Connection connection, String schemaName, List<String> tableToSkip, List<String> tableToClean) {
        clearDatabase(getDefaultRetries(DatabaseType.POSTGRES), connection, getSchemaName(schemaName, DatabaseType.POSTGRES), tableToSkip, tableToClean, DatabaseType.POSTGRES, false, true, false);
    }

    private static String getSchemaName(String schemaName, DatabaseType type) {
        if (schemaName != null) return schemaName;
        return getDefaultSchema(type);
    }

    /**
     * @param tableToSkip   are tables to skip
     * @param tableToClean  are tables to clean
     * @param statement     is to execute the SQL command
     * @param schema        specify the schema of data to clean. if [schema] is empty, all data will be cleaned.
     * @param singleCommand specify whether to execute the SQL commands (e.g., truncate table/tables) by single command
     * @param doDropTable   specify whether to drop tables which is only for MySQL and MariaDB now.
     * @return a list of tables which have been cleaned
     * @throws SQLException are exceptions during sql execution
     */
    private static List<String> cleanDataInTables(List<String> tableToSkip,
                                                  List<String> tableToClean,
                                                  Statement statement,
                                                  DatabaseType type,
                                                  String schema,
                                                  boolean singleCommand,
                                                  boolean doDropTable,
                                                  boolean restartIdentityWhenTruncating) throws SQLException {
        if (tableToSkip != null && (!tableToSkip.isEmpty()) && tableToClean != null && (!tableToClean.isEmpty()))
            throw new IllegalArgumentException("tableToSkip and tableToClean cannot be configured at the same time.");

        // Find all tables and truncate them
        Set<String> tables = new HashSet<>();
        ResultSet rs = statement.executeQuery(getAllTableCommand(type, schema));
        while (rs.next()) {
            tables.add(rs.getString(1));
        }
        rs.close();

        if (tables.isEmpty()) {
            throw new IllegalStateException("Could not find any table");
        }

        final List<String> tableToHandle;
        boolean toskip = tableToSkip != null;
        if (tableToClean != null) {
            tableToHandle = tableToClean;
        } else {
            tableToHandle = tableToSkip;
        }


        if (tableToHandle != null) {
            for (String skip : tableToHandle) {
                if (tables.stream().noneMatch(t -> t.equalsIgnoreCase(skip))) {
                    String msg = "Asked to skip/clean table '" + skip + "', but it does not exist.";
                    msg += " Existing tables in schema '" + schema + "': [" +
                            String.join(", ", tables) + "]";
                    throw new IllegalStateException(msg);
                }
            }
        }

        Set<String> tablesHaveIdentifies = new HashSet<>();
        if (type == DatabaseType.MS_SQL_SERVER) {
            ResultSet rst = statement.executeQuery(getAllTableHasIdentify(type, schema));
            while (rst.next()) {
                tablesHaveIdentifies.add(rst.getString(1));
            }
            rst.close();
        }

        List<String> tablesToClear = tables.stream()
                .filter(n -> tableToHandle == null ||
                        (toskip && (tableToHandle.isEmpty() || tableToHandle.stream().noneMatch(skip -> skip.equalsIgnoreCase(n)))) ||
                        (!toskip && tableToHandle.stream().anyMatch(clean -> clean.equalsIgnoreCase(n)))
                )
                .collect(Collectors.toList());

        if (singleCommand) {
            String ts = tablesToClear.stream()
                    .sorted()
                    .collect(Collectors.joining(","));

            if (type != DatabaseType.POSTGRES)
                throw new IllegalArgumentException("do not support for cleaning all data by one single command for " + type);

            if (doDropTable) {
                dropTableIfExists(statement, ts);
            } else {
                truncateTable(statement, ts, restartIdentityWhenTruncating);
            }
        } else {
            //note: if one at a time, need to make sure to first disable FK checks
            for (String t : tablesToClear) {
                if (doDropTable)
                    dropTableIfExists(statement, t);
                else {
                    /*
                        for MS_SQL_SERVER, we cannot use truncate tables if there exist fk
                        see
                            https://docs.microsoft.com/en-us/sql/t-sql/statements/truncate-table-transact-sql?view=sql-server-ver15#restrictions
                            https://stackoverflow.com/questions/155246/how-do-you-truncate-all-tables-in-a-database-using-tsql#156813
                        then it will cause a problem to reset identify
                    */
                    if (type == DatabaseType.MS_SQL_SERVER)
                        deleteTables(statement, t, schema, tablesHaveIdentifies);
                    else
                        truncateTable(statement, t, restartIdentityWhenTruncating);
                }
            }
        }
        return tablesToClear;
    }


    private static void dropTableIfExists(Statement statement, String table) throws SQLException {
        statement.executeUpdate("DROP TABLE IF EXISTS " + table);
    }

    private static void deleteTables(Statement statement, String table, String schema, Set<String> tableHasIdentify) throws SQLException {
        String tableWithSchema = table;
        /*
         for MS SQL, the delete command should consider its schema,
         but such schema info is not returned when retrieving table name with select command, see [getAllTableCommand]
         then here, we need to reformat the table name with schema
         */
        if (!schema.isEmpty() && !schema.equals(getDefaultSchema(DatabaseType.MS_SQL_SERVER)))
            tableWithSchema = schema + "." + schema;
        statement.executeUpdate("DELETE FROM " + tableWithSchema);
//        NOTE TAHT ideally we should reseed identify here, but there would case an issue, i.e., does not contain an identity column
        if (tableHasIdentify.contains(table))
            statement.executeUpdate("DBCC CHECKIDENT ('" + tableWithSchema + "', RESEED, 0)");
    }

    private static void truncateTable(Statement statement, String table, boolean restartIdentityWhenTruncating) throws SQLException {
        if (restartIdentityWhenTruncating) {
            statement.executeUpdate("TRUNCATE TABLE " + table + " RESTART IDENTITY");
        } else {
            statement.executeUpdate("TRUNCATE TABLE " + table);
        }
    }

    private static void resetSequences(Statement s, DatabaseType type, String schemaName, List<String> sequenceToClean) throws SQLException {
        ResultSet rs;// Idem for sequences
        Set<String> sequences = new HashSet<>();

        rs = s.executeQuery(getAllSequenceCommand(type, schemaName));
        while (rs.next()) {
            sequences.add(rs.getString(1));
        }
        rs.close();
        for (String seq : sequences) {
            if (sequenceToClean == null || sequenceToClean.isEmpty() || sequenceToClean.stream().anyMatch(x -> x.equalsIgnoreCase(seq)))
                s.executeUpdate(resetSequenceCommand(seq, type));
        }

        /*
            Note: we reset all sequences from 1. But the original database might
            have used a different value.
            In most cases (99.99%), this should not be a problem.
            We could allow using different values in this API... but, maybe just easier
            for the user to reset it manually if really needed?
         */
    }


    private static void disableReferentialIntegrity(Statement s, DatabaseType type) throws SQLException {
        switch (type) {
            case POSTGRES:
                break;
            case MS_SQL_SERVER:
                //https://stackoverflow.com/questions/159038/how-can-foreign-key-constraints-be-temporarily-disabled-using-t-sql
                //https://stackoverflow.com/questions/155246/how-do-you-truncate-all-tables-in-a-database-using-tsql#156813
                //https://docs.microsoft.com/en-us/sql/relational-databases/tables/disable-foreign-key-constraints-with-insert-and-update-statements?view=sql-server-ver15
                s.execute("EXEC sp_MSForEachTable \"ALTER TABLE ? NOCHECK CONSTRAINT all\"");
                break;
            case H2:
                s.execute("SET REFERENTIAL_INTEGRITY FALSE");
                break;
            case MARIADB:
            case MYSQL:
                s.execute("SET @@foreign_key_checks = 0;");
                break;
            case OTHER:
                throw new DbUnsupportedException(type);
        }
    }

    private static void enableReferentialIntegrity(Statement s, DatabaseType type) throws SQLException {
        switch (type) {
            case POSTGRES:
                break;
            case MS_SQL_SERVER:
                s.execute("exec sp_MSForEachTable \"ALTER TABLE ? WITH CHECK CHECK CONSTRAINT all\"");
                break;
            case H2:
                /*
                    For H2, we have to delete tables one at a time... but, to avoid issues
                    with FKs, we must temporarily disable the integrity checks
                */
                s.execute("SET REFERENTIAL_INTEGRITY TRUE");
                break;
            case MARIADB:
            case MYSQL:
                s.execute("SET @@foreign_key_checks = 1;");
                break;
            case OTHER:
                throw new DbUnsupportedException(type);
        }
    }

    private static int getDefaultRetries(DatabaseType type) {
        switch (type) {
            case MS_SQL_SERVER:
            case POSTGRES:
                return 0;
            case H2:
            case MARIADB:
            case MYSQL:
                return 3;
        }
        throw new DbUnsupportedException(type);
    }

    private static String getDefaultSchema(DatabaseType type) {
        switch (type) {
            case H2:
                return "PUBLIC";
            //https://docs.microsoft.com/en-us/dotnet/framework/data/adonet/sql/ownership-and-user-schema-separation-in-sql-server
            case MS_SQL_SERVER:
                return "dbo";
            case MARIADB:
            case MYSQL:
                throw new IllegalArgumentException("there is no default schema for " + type + ", and you must specify a db name here");
            case POSTGRES:
                return "public";
        }
        throw new DbUnsupportedException(type);
    }

    private static boolean isSingleCleanCommand(DatabaseType type) {
        return type == DatabaseType.POSTGRES;
    }

    private static String getAllTableHasIdentify(DatabaseType type, String schema) {
        if (type != DatabaseType.MS_SQL_SERVER)
            throw new IllegalArgumentException("getAllTableHasIdentify only supports for MS_SQL_SERVER, not for " + type);
        return getAllTableCommand(type, schema) + " AND OBJECTPROPERTY(OBJECT_ID(TABLE_NAME), 'TableHasIdentity') = 1";
    }

    private static String getAllTableCommand(DatabaseType type, String schema) {
        String command = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES where (TABLE_TYPE='TABLE' OR TABLE_TYPE='BASE TABLE')";

        switch (type) {
            // https://stackoverflow.com/questions/175415/how-do-i-get-list-of-all-tables-in-a-database-using-tsql, TABLE_CATALOG='"+dbname+"'"
            case MS_SQL_SERVER:
                // for MySQL, schema is dbname
            case MYSQL:
            case MARIADB:
            case H2:
            case POSTGRES:
                if (schema.isEmpty())
                    return command;
                return command + " AND TABLE_SCHEMA='" + schema + "'";
        }
        throw new DbUnsupportedException(type);
//            case MS_SQL_SERVER_2000:
//                return "SELECT sobjects.name FROM sysobjects sobjects WHERE sobjects.xtype = '"+schema+"'"; // shcema can be 'U'
    }

    private static String getAllSequenceCommand(DatabaseType type, String schemaName) {
        String command = "SELECT SEQUENCE_NAME FROM INFORMATION_SCHEMA.SEQUENCES";
        switch (type) {
            case MYSQL:
            case MARIADB:
                return getAllTableCommand(type, schemaName);
            case H2:
            case POSTGRES:
                //https://docs.microsoft.com/en-us/sql/relational-databases/system-information-schema-views/sequences-transact-sql?view=sql-server-ver15
            case MS_SQL_SERVER:
                if (schemaName.isEmpty())
                    return command;
                else
                    return command + " WHERE SEQUENCE_SCHEMA='" + schemaName + "'";
        }
        throw new DbUnsupportedException(type);
    }


    private static String resetSequenceCommand(String sequence, DatabaseType type) {
        switch (type) {
            case MARIADB:
            case MYSQL:
                return "ALTER TABLE " + sequence + " AUTO_INCREMENT=1;";
            case MS_SQL_SERVER:
            case H2:
            case POSTGRES:
                return "ALTER SEQUENCE " + sequence + " RESTART WITH 1";
        }
        throw new DbUnsupportedException(type);
    }

}
