package org.evomaster.client.java.sql;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by arcuri82 on 25-Oct-19.
 */
public class SqlScriptRunnerCached {

    /*
        WARNING: mutable static state. But is just a cache.

        Key -> resource path
        Value -> SQL commands, as list of lines
     */
    private static Map<String, List<String>> cache = new HashMap<>();

    private static final SqlScriptRunner runner = new SqlScriptRunner();


    public static void runScriptFromResourceFile(Connection connection, String... paths) {

        for(String p : paths){
            runScriptFromResourceFile(connection, p);
        }
    }

    /**
     * Executes the SQL commands in the given resource file.
     * The data is cached, so subsequent requests do not need to re-read the same files.
     *
     * @param connection the database connection to use for executing the script
     * @param resourcePath the path to the resource file containing the SQL commands
     */
    public static void runScriptFromResourceFile(Connection connection, String resourcePath) {

        List<String> sql = extractSqlScriptFromResourceFile(resourcePath);

        SqlScriptRunner.runCommands(connection, sql);
    }

    /**
     * Extracts SQL script based on a given resource path.
     *
     * @param resourcePath the path to the resource file containing the SQL commands
     * @return a list of SQL commands
     */
    public static List<String> extractSqlScriptFromResourceFile(String resourcePath){
        List<String> sql = cache.get(resourcePath);

        if(sql == null){
            try(InputStream in = SqlScriptRunner.class.getResourceAsStream(resourcePath)) {
                sql = runner.readCommands(new InputStreamReader(in));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            cache.put(resourcePath, sql);
        }
        return sql;
    }
}
