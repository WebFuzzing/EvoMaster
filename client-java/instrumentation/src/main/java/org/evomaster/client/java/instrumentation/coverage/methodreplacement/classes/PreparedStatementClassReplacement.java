package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.SqlInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PreparedStatementClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return PreparedStatement.class;
    }

    private static void handlePreparedStatement(PreparedStatement stmt){
        if(stmt == null){
            //nothing to do
        }

        String fullClassName = stmt.getClass().getName();
        if(fullClassName.startsWith("com.zaxxer.hikari.pool")){
            /*
                this is likely a proxy, so we can skip it, as anyway we are going to
                intercept the call to the delegate
             */
            return;
        }

        /*
            This is very tricky, as not supported by all DBs... see for example:
            https://stackoverflow.com/questions/2382532/how-can-i-get-the-sql-of-a-preparedstatement

            So what done here is quite ad-hoc...
         */
        String sql = stmt.toString();

        if(sql.startsWith("com.mysql")){
            sql = sql.substring(sql.indexOf(":")+1);
        }

        //TODO see TODO in StatementClassReplacement
        SqlInfo info = new SqlInfo(sql, false, false);
        ExecutionTracer.addSqlInfo(info);
    }

    @Replacement(type = ReplacementType.TRACKER)
    public static ResultSet executeQuery(PreparedStatement stmt) throws SQLException{
        handlePreparedStatement(stmt);
        return stmt.executeQuery();
    }

    @Replacement(type = ReplacementType.TRACKER)
    public static int executeUpdate(PreparedStatement stmt) throws SQLException{
        handlePreparedStatement(stmt);
        return stmt.executeUpdate();
    }

    @Replacement(type = ReplacementType.TRACKER)
    public static boolean execute(PreparedStatement stmt) throws SQLException{
        handlePreparedStatement(stmt);
        return stmt.execute();
    }
}
