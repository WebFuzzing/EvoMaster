package org.evomaster.client.java.sql;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.insert.Insert;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionEntryDto;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionResultsDto;
import org.evomaster.client.java.sql.internal.ParserUtils;
import org.evomaster.client.java.utils.SimpleLogger;

import java.io.*;
import java.sql.*;
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

    private static final String QUOTATION_MARK  = "\"";

    private static final String SINGLE_APOSTROPHE_PLACEHOLDER = "SINGLE_APOSTROPHE_PLACEHOLDER";

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

    /**
     *
     * @param reader for SQL script
     * @return extract SQL commands
     */
    public String readSQLCommandsAsString(Reader reader){
        return String.join(";"+System.lineSeparator(), readCommands(reader));
    }

    public List<String> readCommands(Reader reader) {

        List<String> list = new ArrayList<>();

        StringBuilder command = null;
        try {
            LineNumberReader lineReader = new LineNumberReader(reader);
            String line;

            while ((line = lineReader.readLine()) != null) {
                if (command == null) {
                    command = new StringBuilder();
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

                    command.append(line, 0, line.lastIndexOf(delimiter));
                    command.append(" ");

                    list.add(command.toString());
                    command = null;

                } else {
                    command.append(line);
                    command.append("\n");
                }
            }

            if (command != null && command.length() > 0) {
                list.add(command.toString());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return list;
    }


    /**
     * Execute the different SQL insertions.
     * Those can refer to each other via foreign keys, even in the case
     * of auto-generated ids
     *
     * @param conn a JDBC connection to the database
     * @param insertions the SQL insertions to execute
     * @param previous the results of previously executed SQL insertions
     *
     * @return a map from InsertionDto id to id of auto-generated primary
     * keys in the database (if any was generated).
     * If an InsertionDto has no id, we will not keep track of any auto-generated
     * value for it.
     */
    public static InsertionResultsDto execInsert(Connection conn, List<InsertionDto> insertions, InsertionResultsDto... previous) throws SQLException {

        if (insertions == null || insertions.isEmpty()) {
            throw new IllegalArgumentException("No data to insert");
        }

        String insertSql = "INSERT INTO ";

        //From DTO Insertion Id to generated Id in database
        Map<Long, Long> map = new HashMap<>();
        if (previous!=null){
            Arrays.stream(previous).forEach(p->{
                map.putAll(p.idMapping);
            });
        }

        List<Boolean> sqlResults = new ArrayList<>(Collections.nCopies(insertions.size(), false));

        for (int i = 0; i < insertions.size(); i++) {

            InsertionDto insDto = insertions.get(i);

            String sql = prepareSqlInsertionCommand(insertSql, map, i, insDto);

            Long autoGeneratedId;

            try {
                autoGeneratedId = execInsert(conn, sql);
                sqlResults.set(i, true);
            } catch (SQLException e) {
                String msg = "Failed to execute insertion with index " + i + " with SQL: " + sql + ". Error: " + e.getMessage();
                throw new SQLException(msg, e);
            }

            if(insDto.id == null){
                //throw new IllegalArgumentException("Insertion for an autoincrement value in table " + insDto.targetTable + " does not have an id");
                continue;
            }

            if (autoGeneratedId != null) {
                map.put(insDto.id, autoGeneratedId);

            } else {

                /*
                    check if in this insertion there is no auto-generated PK,
                    but there is a foreign key to an auto-increment.

                    There can at most one entry that can be a FK to an auto-increment value.
                    FIXME: this is not really true, eg consider tables to handle relationships,
                    where PK is composed of 2 FK columns, ie the PKs of the 2 related tables.
                    NEED to support multi-column PKs/FKs
                 */

                InsertionEntryDto entry = insDto.data.stream()
                        .filter(e -> e.foreignKeyToPreviouslyGeneratedRow != null)
                        .findFirst().orElse(null);

                if (entry != null) {
                    long previouslyGeneratedValue = map.get(entry.foreignKeyToPreviouslyGeneratedRow);
                    map.put(insDto.id, previouslyGeneratedValue);
                }
            }
        }

        InsertionResultsDto insertionResultsDto = new InsertionResultsDto();
        insertionResultsDto.idMapping = map;
        insertionResultsDto.executionResults = sqlResults;
        return insertionResultsDto;
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
                                    + map.keySet().stream().map(Object::toString).collect(Collectors.joining(", "))
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

    /*
     * In SQL, strings need '' instead of ""
     * Set<ColumnDto> primaryKeys = getPrimaryKeys(schema, tableName);
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
        // replace Special Quotation mark with single apostrophe
        if (value.contains(SINGLE_APOSTROPHE_PLACEHOLDER)) {
            value = value.replaceAll(SINGLE_APOSTROPHE_PLACEHOLDER, SINGLE_APOSTROPHE);
        }
        // replace starting and ending quotation marks (if any)
        if (value.startsWith(QUOTATION_MARK) && value.endsWith(QUOTATION_MARK)) {
            return SINGLE_APOSTROPHE + value.substring(1, value.length() - 1) + SINGLE_APOSTROPHE;
        }

        return value;
    }

    /**
     * @param conn a JDBC connection to the database
     * @param command the SQL insertion to execute
     *
     * @return a single id for the new row, if any was automatically generated, {@code null} otherwise.
     *         In other words, return the value of auto-generated primary key, if any was created.
     */
    public static Long execInsert(Connection conn, String command) throws SQLException {

        SimpleLogger.debug("Executing DB insertion:");
        SimpleLogger.debug(command);

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
        ResultSetMetaData generatedKeysMetaData = generatedKeys.getMetaData();
        Long autoGeneratedId;
        if (generatedKeys.next()) {
            int columnType = generatedKeysMetaData.getColumnType(1);
            switch (columnType) {
                case Types.INTEGER:
                case Types.TINYINT:
                case Types.SMALLINT:
                case Types.BIGINT:
                    autoGeneratedId = generatedKeys.getLong(1);
                    break;
                default:
                    // TODO Support non-Long generated keys
                    autoGeneratedId = null;
            }
        } else {
            autoGeneratedId = null;
        }

        statement.close();

        return autoGeneratedId;
    }

    /**
     * this is used for mysql which cannot execute multiple statements at one time.
     *
     * @param conn a connection to db
     * @param script represents a sql script
     * @throws SQLException if the execution of the command fails
     */
    public static void execScript(Connection conn, String script) throws SQLException {
        List<String> commands = extractSql(script);
        for (String command : commands){
            execCommand(conn, command+";");
        }
    }

    /**
     *
     * @param conn a connection to db
     * @param script represents a sql script
     * @param tablesToInsert represents insertions are executed only on the tables in this list.
     *                       insertions for other tables will be skipped.
     *                       if it is null or empty, nothing will be inserted.
     * @throws SQLException if the execution of the command fails
     */
    public static void execScript(Connection conn, String script, List<String> tablesToInsert) throws SQLException {
        if (tablesToInsert == null || tablesToInsert.isEmpty()) return;
        List<String> commands = extractSql(script);
        for (String command : commands){
            if(shouldExecuteInsert(command, tablesToInsert)){
                execCommand(conn, command+";");
            }
        }
    }

    /**
     *
     * @param script is a SQL script
     * @return a list of SQL commands based on the script
     */
    public static List<String> extractSql(String script){
        String[] commands = script.split(";");
        return Arrays.stream(commands).filter(
                s-> !s.replaceAll("\r\n","") // on Windows
                        .replaceAll("\n","") // on Unix
                        .isEmpty()).map(s-> s+";").collect(Collectors.toList());
    }

    /**
     * extract a map from table name to a list of SQL INSERT commands for initializing data into the table
     * @param commands a list of SQL commands to be extracted
     * @return the map from table name (key) to a list of SQL INSERT commands (values) on the table
     */
    public static  Map<String, List<String>> extractSqlTableMap(List<String> commands){
        Map<String, List<String>> tableSqlMap = new HashMap<>();
        for (String command: commands){
            if (ParserUtils.isInsert(command)){
                Insert stmt = (Insert) ParserUtils.asStatement(command);
                Table table = stmt.getTable();
                tableSqlMap.putIfAbsent(table.getName(), new ArrayList<>());
                String end = "";
                if (!command.replaceAll(" ","").replaceAll("\r","").replaceAll("\n","").endsWith(";"))
                    end = ";";
                tableSqlMap.get(table.getName()).add(command+end);
            }
        }
        return tableSqlMap;
    }


    public static QueryResult execCommand(Connection conn, String command) throws SQLException {
        Statement statement = conn.createStatement();

        SimpleLogger.debug("Executing DB command:");
        SimpleLogger.debug(command);

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

    private static boolean shouldExecuteInsert(String command, List<String> tablesToInsert){
        if (!ParserUtils.isInsert(command)) return true;
        if (tablesToInsert == null || tablesToInsert.isEmpty()) return false;
        Insert stmt = (Insert) ParserUtils.asStatement(command);
        Table table = stmt.getTable();
        return table!= null && tablesToInsert.stream().anyMatch(t-> t.equalsIgnoreCase(table.getName()));
    }

}
