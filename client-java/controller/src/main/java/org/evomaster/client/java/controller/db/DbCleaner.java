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

    public static void clearDatabase_Derby(Connection connection, String schemaName) {

        /*
            Code from
            https://stackoverflow.com/questions/171727/delete-all-tables-in-derby-db
         */

        String sql = "SELECT " +
                "'ALTER TABLE '||S.SCHEMANAME||'.'||T.TABLENAME||' DROP CONSTRAINT '||C.CONSTRAINTNAME||';' " +
                "FROM " +
                "    SYS.SYSCONSTRAINTS C, " +
                "    SYS.SYSSCHEMAS S, " +
                "    SYS.SYSTABLES T " +
                "WHERE " +
                "    C.SCHEMAID = S.SCHEMAID " +
                "AND " +
                "    C.TABLEID = T.TABLEID " +
                "AND " +
                "S.SCHEMANAME = '" + schemaName + "' " +
                "UNION " +
                "SELECT 'DROP TABLE ' || schemaname ||'.' || tablename || ';' " +
                "FROM SYS.SYSTABLES " +
                "INNER JOIN SYS.SYSSCHEMAS ON SYS.SYSTABLES.SCHEMAID = SYS.SYSSCHEMAS.SCHEMAID " +
                "where schemaname='" + schemaName + "'";

        try {
            Statement s = connection.createStatement();
            s.execute(sql);
            s.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void clearDatabase_H2(Connection connection) {
        clearDatabase_H2(connection, null);
    }

    public static void clearDatabase_H2(Connection connection, List<String> tablesToSkip) {
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

            truncateTables(tablesToSkip, s, "PUBLIC", false);

            resetSequences(s, "PUBLIC");

            s.execute("SET REFERENTIAL_INTEGRITY TRUE");
            s.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void clearDatabase_Postgres(Connection connection) {
        clearDatabase_Postgres(connection, null);
    }

    public static void clearDatabase_Postgres(Connection connection, List<String> tablesToSkip) {

        try {
            Statement s = connection.createStatement();

            truncateTables(tablesToSkip, s, "public", true);

            resetSequences(s, "public");

            s.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void truncateTables(List<String> tablesToSkip, Statement s, String schema, boolean singleCommand) throws SQLException {

        // Find all tables and truncate them
        Set<String> tables = new HashSet<>();
        ResultSet rs = s.executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES  where TABLE_SCHEMA='" + schema + "'");
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
                    throw new IllegalStateException("Asked to skip table '" + skip + "', but it does not exist");
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
    }
}
