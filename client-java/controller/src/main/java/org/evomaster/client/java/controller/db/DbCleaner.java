package org.evomaster.client.java.controller.db;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
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

    public static void clearDatabase_H2(Connection connection, List<String> tablesToSkip) {
        clearDatabase_H2(connection, "PUBLIC", tablesToSkip);
    }

    public static void clearDatabase_H2(Connection connection, String schemaName, List<String> tablesToSkip) {
        clearDatabase(getDefaultReties(DatabaseType.H2), connection, schemaName, tablesToSkip, DatabaseType.H2, false);
    }

    private static void clearDatabase(int retries, Connection connection, String schemaName, List<String> tablesToSkip, DatabaseType type, boolean doDropTable) {
        /*
            Code based on
            https://stackoverflow.com/questions/8523423/reset-embedded-h2-database-periodically
         */

        try {
            Statement s = connection.createStatement();

            /*
                For H2, we have to delete tables one at a time... but, to avoid issues
                with FKs, we must temporarily disable the integrity checks
             */
            disableReferentialIntegrity(s, type);

            truncateTables(tablesToSkip, s, schemaName, isSingleCleanCommand(type), doDropTable);

            resetSequences(s, type, schemaName);

            enableReferentialIntegrity(s, type);
            s.close();
        } catch (Exception e) {
            /*
                this could happen if there is a current transaction with a lock on any table.
                We could check the content of INFORMATION_SCHEMA.LOCKS, or simply look at error message
             */
            String msg = e.getMessage();
            if(msg != null && msg.toLowerCase().contains("timeout")){
                if(retries > 0) {
                    SimpleLogger.warn("Timeout issue with cleaning DB. Trying again.");
                    //let's just wait a bit, and retry
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException interruptedException) {
                    }
                    retries--;
                    clearDatabase(retries, connection, schemaName, tablesToSkip, type, doDropTable);
                } else {
                    SimpleLogger.error("Giving up cleaning the DB. There are still timeouts.");
                }
            }

            throw new RuntimeException(e);
        }
    }


    public static void clearDatabase(Connection connection, String schemaName, List<String> tablesToSkip, DatabaseType type){
        clearDatabase(getDefaultReties(type), connection, schemaName, tablesToSkip, type, false);
    }

    public static void dropDatabaseTables(Connection connection, String schemaName, List<String> tablesToSkip, DatabaseType type){
        if (type != DatabaseType.MYSQL && type != DatabaseType.MARIADB)
            throw new IllegalArgumentException("Dropping tables are not supported by "+type);
        clearDatabase(getDefaultReties(type), connection, schemaName, tablesToSkip, type, true);
    }


    public static void clearDatabase_Postgres(Connection connection, String schemaName, List<String> tablesToSkip ) {
        clearDatabase(getDefaultReties(DatabaseType.POSTGRES), connection, schemaName, tablesToSkip, DatabaseType.POSTGRES, false);
    }

    /**
     *
     * @param tablesToSkip are tables to be skipped
     * @param statement is to execute the SQL command
     * @param schema is the table schema
     * @param singleCommand specify whether to execute the SQL commands (e.g., truncate table/tables) by single command
     * @param doDropTable specify whether to drop tables which is only for MySQL and MariaDB now.
     * @throws SQLException are exceptions during sql execution
     */
    private static void truncateTables(List<String> tablesToSkip, Statement statement, String schema, boolean singleCommand, boolean doDropTable) throws SQLException {

        // Find all tables and truncate them
        Set<String> tables = new HashSet<>();
        ResultSet rs = statement.executeQuery(getAllTableCommand(schema));
        while (rs.next()) {
            tables.add(rs.getString(1));
        }
        rs.close();

        if (tables.isEmpty()) {
            throw new IllegalStateException("Could not find any table");
        }

        if (tablesToSkip != null) {
            for (String skip : tablesToSkip) {
                if (!tables.stream().anyMatch(t -> t.equalsIgnoreCase(skip))) {
                    String msg = "Asked to skip table '" + skip + "', but it does not exist.";
                    msg += " Existing tables in schema '"+schema+"': [" +
                            tables.stream().collect(Collectors.joining(", ")) + "]";
                    throw new IllegalStateException(msg);
                }
            }
        }

        List<String> tablesToClear = tables.stream()
                .filter(n -> tablesToSkip == null || tablesToSkip.isEmpty() ||
                        !tablesToSkip.stream().anyMatch(skip -> skip.equalsIgnoreCase(n)))
                .collect(Collectors.toList());

        if (singleCommand) {
            String ts = tablesToClear.stream()
                    .sorted()
                    .collect(Collectors.joining(","));

            if (doDropTable)
                statement.execute("DROP TABLE IF EXISTS " +ts);
            else
                statement.executeUpdate("TRUNCATE TABLE " + ts);
        } else {
            //note: if one at a time, need to make sure to first disable FK checks
            for(String t : tablesToClear){
                if (doDropTable)
                    statement.execute("DROP TABLE IF EXISTS " +t);
                else
                    statement.executeUpdate("TRUNCATE TABLE " + t);
            }
        }
    }

    private static void resetSequences(Statement s, DatabaseType type, String schemaName) throws SQLException {
        ResultSet rs;// Idem for sequences
        Set<String> sequences = new HashSet<>();
        rs = s.executeQuery(getAllSequenceCommand(type, schemaName));
        while (rs.next()) {
            sequences.add(rs.getString(1));
        }
        rs.close();
        for (String seq : sequences) {
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
        switch (type)
        {
            case POSTGRES: break;
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
        switch (type)
        {
            case POSTGRES: break;
            case H2:
                /*
                    For H2, we have to delete tables one at a time... but, to avoid issues
                    with FKs, we must temporarily disable the integrity checks
                */
                s.execute( "SET REFERENTIAL_INTEGRITY TRUE");
                break;
            case MARIADB:
            case MYSQL:
                s.execute("SET @@foreign_key_checks = 1;");
                break;
            case OTHER:
                throw new DbUnsupportedException(type);
        }
    }

    private static int getDefaultReties(DatabaseType type){
        switch (type){
            case POSTGRES: return 0;
            case H2:
            case MARIADB:
            case MYSQL: return 3;
        }
        throw new DbUnsupportedException(type);
    }

    private static String getDefaultSchema(DatabaseType type){
        switch (type){
            case H2: return "PUBLIC";
            case MARIADB:
            case MYSQL: throw new IllegalArgumentException("there is no default schema for MySQL");
            case POSTGRES: return "public";
        }
        throw new DbUnsupportedException(type);
    }

    private static boolean isSingleCleanCommand(DatabaseType type){
        return type == DatabaseType.POSTGRES;
    }


    private static String getAllTableCommand(String schema) {
        return "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA='" + schema + "' AND (TABLE_TYPE='TABLE' OR TABLE_TYPE='BASE TABLE')";
    }

    private static String getAllSequenceCommand(DatabaseType type, String schemaName)
    {
        switch (type){
            case MYSQL:
            case MARIADB: return getAllTableCommand(schemaName);
            case H2:
            case POSTGRES: return getAllSequenceCommand(getDefaultSchema(type));
        }
        throw new DbUnsupportedException(type);
    }


    private static String getAllSequenceCommand(String schema)
    {
        return "SELECT SEQUENCE_NAME FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_SCHEMA='" + schema + "'";
    }

    private static String resetSequenceCommand(String sequence, DatabaseType type) {
        switch (type){
            case MARIADB:
            case MYSQL: return "ALTER TABLE " + sequence + " AUTO_INCREMENT=1;";
            case H2:
            case POSTGRES: return "ALTER SEQUENCE " + sequence + " RESTART WITH 1";
        }
        throw new DbUnsupportedException(type);
    }

}
