package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.StatementClassReplacement.executeSql;

public class PreparedStatementClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return PreparedStatement.class;
    }

    public static String extractSqlFromH2PreparedStatement(PreparedStatement stmt) {
        Class<?> klass = stmt.getClass();
        String className = klass.getName();
        if (!className.equals("org.h2.jdbc.JdbcPreparedStatement")) {
            throw new IllegalArgumentException("Invalid type: " + className);
        }

        try {
            /*
                Quite brittle... but easy to test on new H2 releases
             */
            Field cf = klass.getDeclaredField("command");
            cf.setAccessible(true);
            Object command = cf.get(stmt);
            Class<?> ck = command.getClass();
            if(! ck.getName().endsWith("CommandRemote")){
                /*
                    very brittle.. :(
                    based on the concrete class for the Command (there are 3...)
                    the location of the 'sql' private field is different :(
                 */
                ck = ck.getSuperclass();
            }
            Field sf = ck.getDeclaredField("sql");
            sf.setAccessible(true);
            String sql = (String) sf.get(command);

            Method pm = command.getClass().getDeclaredMethod("getParameters");
            pm.setAccessible(true);
            List<?> pv = (List<?>) pm.invoke(command);
            List<String> params = pv.stream()
                    .map(it -> {
                        try {
                            Method gpvm = it.getClass().getDeclaredMethod("getParamValue");
                            gpvm.setAccessible(true);
                            Object value =  gpvm.invoke(it);
                            if(value.getClass().getName().equals("org.h2.value.ValueLobDb")){
                                //FIXME this gives issues... not sure we can really handle it
                                return "LOB";
                            }
                            return value.toString();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            return interpolateSqlStringWithJSqlParser(sql, params);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // replaced by interpolateSqlStringWithJSqlParser
    @Deprecated
    public static String interpolateSqlString(String sql, List<String> params) {

        long replacements = sql.chars().filter(it -> it=='?').count();
        if(replacements != params.size()){
            SimpleLogger.error("EvoMaster ERROR. Mismatch of parameter count " + replacements+"!="+ params.size()
                    + " in SQL command: " + sql);
            return null;
        }

        for(String p : params){
            sql = sql.replaceFirst("\\?", p);
        }

        return sql;
    }


    /**
     * inspired by this example from https://stackoverflow.com/questions/46890089/how-can-i-purify-a-sql-query-and-replace-all-parameters-with-using-regex
     * @param sqlCommand is an original sql command which might contain comments or be dynamic sql with parameters
     * @param params are parameters which exists in the [sql]
     * @return a interpolated sql.
     * note that if the sql could not be handled, we return the original one since such info is still useful for e.g., industrial partner
     *          then we could record the execution info.
     * note that comments could also be removed with this function.
     *
     */
    public static String interpolateSqlStringWithJSqlParser(String sqlCommand, List<String> params) {
        if (params.isEmpty()) {
            // if no params, return the same sql without visiting
            return sqlCommand;
        }

        StringBuilder sqlbuffer = new StringBuilder();

        try {
            ExpressionDeParser expDeParser = new ExpressionDeParser() {
                @Override
                public void visit(JdbcParameter parameter) {
                    int index = parameter.getIndex();
                    this.getBuffer().append(params.get(index-1));
                }
            };
            SelectDeParser selectDeparser = new SelectDeParser(expDeParser, sqlbuffer);
            expDeParser.setSelectVisitor(selectDeparser);
            expDeParser.setBuffer(sqlbuffer);
            StatementDeParser stmtDeparser = new StatementDeParser(expDeParser, selectDeparser, sqlbuffer);

            Statement stmt = CCJSqlParserUtil.parse(sqlCommand);
            stmt.accept(stmtDeparser);
            return stmtDeparser.getBuffer().toString();
        } catch (Exception e) {
            // catch all kinds of exception here since there might exist problems in processing params
            SimpleLogger.error("EvoMaster ERROR. Could not handle "+ sqlCommand + " with an error message :"+e.getMessage());
            return sqlCommand;
        }
    }

    /**
     *
     * @param stmt a sql statement to be prepared
     * @return a null if skip to handle the stmt
     */
    private static String handlePreparedStatement(PreparedStatement stmt) {
        if (stmt == null) {
            //nothing to do
            return null;
        }

        String fullClassName = stmt.getClass().getName();
        if (fullClassName.startsWith("com.zaxxer.hikari.pool") ||
                fullClassName.startsWith("org.apache.tomcat.jdbc.pool") ||
                fullClassName.startsWith("com.sun.proxy") ||
                checkZebraPreparedStatementWrapper(fullClassName) // zebra
        ) {
            /*
                this is likely a proxy/wrapper, so we can skip it, as anyway we are going to
                intercept the call to the delegate
             */
            return null;
        }

        /*
            This is very tricky, as not supported by all DBs... see for example:
            https://stackoverflow.com/questions/2382532/how-can-i-get-the-sql-of-a-preparedstatement

            So what done here is quite ad-hoc...
            There is no direct way to access the SQL command... but some drivers do print it
            when calling toString.

            Another option would be to intercept all methods to build the query, and reconstruct
            it manually... but it would be a LOT of work.
            Maybe something to consider if we are going to support more DBs in the future...
         */
        String sql = stmt.toString();

        //Postgres print the command as it is

        if (sql.startsWith("com.mysql")) {
            //MySQL prepend the command with the name of class followed by :
            sql = sql.substring(sql.indexOf(":") + 1);
        }

        if (stmt.getClass().getName().equals("org.h2.jdbc.JdbcPreparedStatement")) {
            /*
                Unfortunately H2 does not support it... so we have to extract and
                recompute it manually :(
             */
            sql = extractSqlFromH2PreparedStatement(stmt);
        }

        /*
            all zebra prepared statements should be handled before this line
            (see checkZebraPreparedStatementWrapper).
            here, just check whether there exist any further update in zebra,
            and throw an exception with unsupported type
         */
        if (fullClassName.startsWith("com.dianping.zebra")){
            throw new IllegalArgumentException("unsupported type for zebra: " + fullClassName);
        }

        //TODO see TODO in StatementClassReplacement
//        SqlInfo info = new SqlInfo(sql, false, false);
//        ExecutionTracer.addSqlInfo(info);
        return sql;
    }

    private static boolean checkZebraPreparedStatementWrapper(String className){
        return className.equals("com.dianping.zebra.group.jdbc.GroupPreparedStatement") ||
                className.equals("com.dianping.zebra.shard.jdbc.ShardPreparedStatement") ||
                className.equals("com.dianping.zebra.single.jdbc.SinglePreparedStatement");
    }

    @Replacement(type = ReplacementType.TRACKER, isPure = false, category = ReplacementCategory.SQL)
    public static ResultSet executeQuery(PreparedStatement stmt) throws SQLException {
        String sql = handlePreparedStatement(stmt);
        return executeSql(stmt::executeQuery, sql);
    }

    @Replacement(type = ReplacementType.TRACKER, isPure = false, category = ReplacementCategory.SQL)
    public static int executeUpdate(PreparedStatement stmt) throws SQLException {
        String sql = handlePreparedStatement(stmt);
        return executeSql(stmt::executeUpdate, sql);
    }

    @Replacement(type = ReplacementType.TRACKER, isPure = false, category = ReplacementCategory.SQL)
    public static boolean execute(PreparedStatement stmt) throws SQLException {
        String sql = handlePreparedStatement(stmt);
        return executeSql(stmt::execute, sql);
    }
}
