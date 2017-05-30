package org.evomaster.clientJava.controller.db;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class used to execute SQL commands from a script file
 */
public class SqlScriptRunner {

    /*
        Class adapted from ScriptRunner
        https://github.com/BenoitDuffez/ScriptRunner/blob/master/ScriptRunner.java

        released under Apache 2.0 license
     */

    private static final String DEFAULT_DELIMITER = ";";

    /**
     * regex to detect delimiter.
     * ignores spaces, allows delimiter in comment, allows an equals-sign
     */
    public static final Pattern delimP = Pattern.compile("^\\s*(--)?\\s*delimiter\\s*=?\\s*([^\\s]+)+\\s*.*$", Pattern.CASE_INSENSITIVE);

    private final Connection connection;

    private String delimiter = DEFAULT_DELIMITER;
    private boolean fullLineDelimiter = false;

    /**
     * Default constructor
     */
    public SqlScriptRunner(Connection connection) {
        this.connection = connection;
    }

    public void setDelimiter(String delimiter, boolean fullLineDelimiter) {
        this.delimiter = delimiter;
        this.fullLineDelimiter = fullLineDelimiter;
    }


    /**
     * Runs an SQL script (read in using the Reader parameter)
     *
     * @param reader - the source of the script
     */
    public void runScript(Reader reader) {
        Objects.requireNonNull(reader);

        runCommands(connection, readCommands(reader));
    }

    public static void runCommands(Connection connection, List<String> commands) {
        try {
            boolean originalAutoCommit = connection.getAutoCommit();
            try {
                if (!originalAutoCommit) {
                    connection.setAutoCommit(true);
                }

                for (String command : commands) {
                    execCommand(connection, command);
                }
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error running script.  Cause: " + e, e);
        }
    }

    public List<String> readCommands(Reader reader) {

        List<String> list = new ArrayList<>();

        StringBuffer command = null;
        try {
            LineNumberReader lineReader = new LineNumberReader(reader);
            String line;

            while ((line = lineReader.readLine()) != null) {
                if (command == null) {
                    command = new StringBuffer();
                }

                String trimmedLine = line.trim();
                Matcher delimMatch = delimP.matcher(trimmedLine);

                if (trimmedLine.isEmpty()
                        || trimmedLine.startsWith("//")
                        || trimmedLine.startsWith("--")) {
                    // Do nothing
                } else if (delimMatch.matches()) {
                    setDelimiter(delimMatch.group(2), false);
                } else if (!fullLineDelimiter
                        && trimmedLine.endsWith(delimiter)
                        || fullLineDelimiter
                        && trimmedLine.equals(delimiter)) {

                    command.append(line.substring(0, line.lastIndexOf(delimiter)));
                    command.append(" ");

                    list.add(command.toString());
                    command = null;

                } else {
                    command.append(line);
                    command.append("\n");
                }
            }

            if (command != null) {
                list.add(command.toString());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return list;
    }


    public static QueryResult execCommand(Connection conn, String command) throws SQLException {
        Statement statement = conn.createStatement();

        try {
            statement.execute(command);
        } catch (SQLException e) {
            String errText = String.format("Error executing '%s': %s", command, e.getMessage());
            throw new SQLException(errText, e);
        }

        conn.commit();

        ResultSet result = statement.getResultSet();
        QueryResult queryResult = new QueryResult(result);

        try {
            statement.close();
        } catch (Exception e) {
            // Ignore to workaround a bug in Jakarta DBCP
        }

        return queryResult;
    }

}
