package org.evomaster.client.java.controller.db;

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
            s.execute("SET REFERENTIAL_INTEGRITY FALSE");

            truncateTables(tablesToSkip, s, schemaName, false);

            resetSequences(s, schemaName);

            s.execute("SET REFERENTIAL_INTEGRITY TRUE");
            s.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void clearDatabase_Postgres(Connection connection) {
        clearDatabase_Postgres(connection, "public", null);
    }

    public static void clearDatabase_Postgres(Connection connection, String schemaName, List<String> tablesToSkip) {

        try {
            Statement s = connection.createStatement();

            truncateTables(tablesToSkip, s, schemaName, true);

            resetSequences(s, schemaName);

            s.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void truncateTables(List<String> tablesToSkip, Statement s, String schema, boolean singleCommand) throws SQLException {

        // Find all tables and truncate them
        Set<String> tables = new HashSet<>();
        ResultSet rs = s.executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES  where TABLE_SCHEMA='" + schema + "' AND (TABLE_TYPE='TABLE' OR TABLE_TYPE='BASE TABLE')");
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

            s.executeUpdate("TRUNCATE TABLE " + ts);
        } else {
            //note: if one at a time, need to make sure to first disable FK checks
            for(String t : tablesToClear){
                s.executeUpdate("TRUNCATE TABLE " + t);
            }
        }
    }

    private static void resetSequences(Statement s, String schema) throws SQLException {
        ResultSet rs;// Idem for sequences
        Set<String> sequences = new HashSet<>();
        rs = s.executeQuery("SELECT SEQUENCE_NAME FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_SCHEMA='" + schema + "'");
        while (rs.next()) {
            sequences.add(rs.getString(1));
        }
        rs.close();
        for (String seq : sequences) {
            s.executeUpdate("ALTER SEQUENCE " + seq + " RESTART WITH 1");
        }

        /*
            Note: we reset all sequences from 1. But the original database might
            have used a different value.
            In most cases (99.99%), this should not be a problem.
            We could allow using different values in this API... but, maybe just easier
            for the user to reset it manually if really needed?
         */
    }
}
