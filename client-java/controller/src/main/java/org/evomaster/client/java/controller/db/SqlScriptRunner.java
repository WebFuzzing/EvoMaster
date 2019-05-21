package org.evomaster.client.java.controller.db;

import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionEntryDto;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


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

    private static final String SINGLE_APOSTROPHE = "'";

    private static final String DOUBLE_APOSTROPHE = "''";

    private String delimiter = DEFAULT_DELIMITER;
    private boolean fullLineDelimiter = false;

    /**
     * Default constructor
     */
    public SqlScriptRunner() {
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
    public static void runScript(Connection connection, Reader reader) {
        Objects.requireNonNull(reader);

        runCommands(connection, new SqlScriptRunner().readCommands(reader));
    }

    public static void runScriptFromResourceFile(Connection connection, String resourcePath) {
        try {
            InputStream in = SqlScriptRunner.class.getResourceAsStream(resourcePath);
            runScript(connection, new InputStreamReader(in));
            in.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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


    //From DTO Insertion Id to generated Id in database
    public static Map<Long, Long> map = new HashMap<>();

    public static Map<Long, Long> execInsert(Connection conn, List<InsertionDto> insertions) throws SQLException {
        return execInsert(conn, insertions, true);
    }

    /**
     * Execute the different SQL insertions.
     * Those can refer to each other via foreign keys, even in the case
     * of auto-generated ids
     *
     * @return a map from InsertionDto id to id of auto-generated primary
     * keys in the database (if any was generated)
     */
    public static Map<Long, Long> execInsert(Connection conn, List<InsertionDto> insertions, Boolean isFirst) throws SQLException {

        if (insertions == null || insertions.isEmpty()) {
            throw new IllegalArgumentException("No data to insert");
        }

        if (isFirst) map.clear();

        String insertSql = "INSERT INTO ";

        for (int i = 0; i < insertions.size(); i++) {

            InsertionDto insDto = insertions.get(i);

            String sql = prepareSqlInsertionCommand(insertSql, map, i, insDto);

            Long id;

            try {
                id = execInsert(conn, sql);
            } catch (SQLException e){
                String msg = "Failed to execute insertion with index " + i + " with SQL: " + sql + ". Error: " + e.getMessage();
                throw new SQLException(msg, e);
            }

            if (id == null) {
                /*
                    check if we need to keep the auto generated value.
                    there can at most one entry that can be a FK to an auto-increment value
                 */
                InsertionEntryDto entry = insDto.data.stream()
                        .filter(e -> e.keepAutoGeneratedValue != null && e.keepAutoGeneratedValue == true)
                        .findFirst().orElse(null);

                if (entry != null) {
                    long previouslyGeneratedValue = map.get(entry.foreignKeyToPreviouslyGeneratedRow);
                    map.put(insDto.id, previouslyGeneratedValue);
                }
            }
            if (id != null) {
                map.put(insDto.id, id);
            }
        }

        return map;
    }

    private static String prepareSqlInsertionCommand(String insertSql, Map<Long, Long> map, int i, InsertionDto insDto) {
        StringBuilder sql = new StringBuilder(insertSql);
        sql.append(insDto.targetTable).append(" (");

        sql.append(insDto.data.stream()
                .map(e -> e.variableName)
                .collect(Collectors.joining(",")));

        sql.append(" )  VALUES (");

        for (InsertionEntryDto e : insDto.data) {
            if (e.printableValue == null && e.foreignKeyToPreviouslyGeneratedRow != null) {
                if (!map.containsKey(e.foreignKeyToPreviouslyGeneratedRow)) {
                    throw new IllegalArgumentException(
                            "Insertion operation at position " + i
                                    + " has a foreign key reference to key "
                                    + e.foreignKeyToPreviouslyGeneratedRow
                                    + " but that was not processed."
                                    + " Processed primary keys: "
                                    + map.keySet().stream().map(v -> v.toString()).collect(Collectors.joining(", "))
                    );
                }
            }
        }

        sql.append(insDto.data.stream()
                .map(e -> e.printableValue != null
                        ? replaceQuotes(e.printableValue)
                        : map.get(e.foreignKeyToPreviouslyGeneratedRow).toString()
                ).collect(Collectors.joining(",")));

        sql.append(");");

        return sql.toString();
    }

    /**
     * In SQL, strings need '' instead of ""         Set<ColumnDto> primaryKeys = getPrimaryKeys(schema, tableName);
     * for (ColumnDto primaryKey : primaryKeys) {
     * primaryKey.
     * }(at least for H2).
     * Also, in H2 single apostrophes have to be duplicated
     * (http://h2database.com/html/grammar.html#string)
     */
    private static String replaceQuotes(String value) {
        if (value.contains(SINGLE_APOSTROPHE)) {
            String oldValue = value;
            value = value.replaceAll(SINGLE_APOSTROPHE, DOUBLE_APOSTROPHE);
            assert (!oldValue.equals(value));
        }
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return "'" + value.substring(1, value.length() - 1) + "'";
        }

        return value;
    }

    /**
     * @return a single id for the new row, if any was automatically generated, {@code null} otherwise
     * @throws SQLException
     */
    public static Long execInsert(Connection conn, String command) throws SQLException {

        String insert = "INSERT ";

        command = command.trim();
        if (!command.toUpperCase().startsWith(insert)) {
            throw new IllegalArgumentException("SQL command is not an INSERT\n" + command);
        }

        Statement statement = conn.createStatement();

        try {
            statement.executeUpdate(command, Statement.RETURN_GENERATED_KEYS);
        } catch (SQLException e) {
            statement.close();
            String errText = String.format("Error executing '%s': %s", command, e.getMessage());
            throw new SQLException(errText, e);
        }

        ResultSet generatedKeys = statement.getGeneratedKeys();
        Long id = null;

        if (generatedKeys.next()) {
            id = generatedKeys.getLong(1);
        }

        statement.close();

        return id;
    }

    public static QueryResult execCommand(Connection conn, String command) throws SQLException {
        Statement statement = conn.createStatement();

        try {
            statement.execute(command);
        } catch (SQLException e) {
            statement.close();
            String errText = String.format("Error executing '%s': %s", command, e.getMessage());
            throw new SQLException(errText, e);
        }

        ResultSet result = statement.getResultSet();
        QueryResult queryResult = new QueryResult(result);

        statement.close();

        return queryResult;
    }

}
