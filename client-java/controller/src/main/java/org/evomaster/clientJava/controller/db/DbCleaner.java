package org.evomaster.clientJava.controller.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Class used to clean/reset the state of the current database
 */
public class DbCleaner {

    public static void clearDatabase_H2(Connection connection) {
        /*
            Code based on
            https://stackoverflow.com/questions/8523423/reset-embedded-h2-database-periodically
         */

        try {
            Statement s = connection.createStatement();

            // Disable FK
            s.execute("SET REFERENTIAL_INTEGRITY FALSE");

            // Find all tables and truncate them
            Set<String> tables = new HashSet<>();
            ResultSet rs = s.executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES  where TABLE_SCHEMA='PUBLIC'");
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
            rs.close();
            for (String table : tables) {
                s.executeUpdate("TRUNCATE TABLE " + table);
            }

            // Idem for sequences
            Set<String> sequences = new HashSet<>();
            rs = s.executeQuery("SELECT SEQUENCE_NAME FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_SCHEMA='PUBLIC'");
            while (rs.next()) {
                sequences.add(rs.getString(1));
            }
            rs.close();
            for (String seq : sequences) {
                s.executeUpdate("ALTER SEQUENCE " + seq + " RESTART WITH 1");
            }

            // Enable FK
            s.execute("SET REFERENTIAL_INTEGRITY TRUE");
            s.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
