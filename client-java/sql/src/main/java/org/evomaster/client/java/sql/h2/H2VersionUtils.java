package org.evomaster.client.java.sql.h2;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.VersionUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class H2VersionUtils {

    private static final int COLUMN_INDEX_H2_VERSION = 1;

    /**
     * A string representing version "2.0.0" of the H2 database
     */
    public static final String H2_VERSION_2_0_0 = "2.0.0";

    /**
     * Returns true if [versionString] &gt;= [otherVersionString]
     *
     * @param versionString      a string with a version (e.g. "1.2.100")
     * @param otherVersionString another string with a version (e.g. "1.4.100")
     * @return true if [versionString] &gt;= [otherVersionString]
     */
    public static synchronized boolean isVersionGreaterOrEqual(String versionString, String otherVersionString) {
        Version version = VersionUtil.parseVersion(versionString, null, null);
        Version otherVersion = VersionUtil.parseVersion(otherVersionString, null, null);
        return version.compareTo(otherVersion) >= 0;
    }

    /**
     * Returns the version of the H2 database.
     * Some possible values are "2.1.212", "1.4.200", "1.3.199", etc.
     *
     * @param connectionToH2 the connection to the H2 database
     * @return the version of the H2 database
     * @throws SQLException if the H2Version() function is not implemented
     */
    public static synchronized String getH2Version(Connection connectionToH2) throws SQLException {
        try (Statement statement = connectionToH2.createStatement()) {
            ResultSet columns;
            try {
                final String selectH2versionQuery = "SELECT H2VERSION();";
                columns = statement.executeQuery(selectH2versionQuery);
            } catch (SQLException ex) {
                /*
                 * In some versions of H2, the SELECT H2VERSION() query throws a "Function "H2VERSION" not found"
                 * SQLException. In that scenario, we could still try with a low level query as shown in
                 * https://stackoverflow.com/questions/40729216/h2-database-unsupported-database-file-version-or-invalid-file-header-in-file
                 */
                if (ex.getMessage()!=null && ex.getMessage().contains("Function \"H2VERSION\" not found")) {
                    final String query = "SELECT value FROM information_schema.settings WHERE name = 'info.VERSION';";
                    columns = statement.executeQuery(query);
                } else {
                    throw ex;
                }
            }
            boolean hasNext = columns.next();
            if (!hasNext) {
                throw new IllegalArgumentException("No results for query: SELECT H2VERSION();");
            }
            return columns.getString(COLUMN_INDEX_H2_VERSION);

        }
    }

}
