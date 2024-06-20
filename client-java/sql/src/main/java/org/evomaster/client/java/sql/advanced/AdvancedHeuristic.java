package org.evomaster.client.java.sql.advanced;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.advanced.driver.SqlDriver;
import org.evomaster.client.java.sql.advanced.query_calculator.CalculationResult;
import org.evomaster.client.java.sql.advanced.query_calculator.QueryCalculator;
import org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.WhereCalculator;
import org.evomaster.client.java.sql.internal.TaintHandler;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.UUID;

import static java.lang.String.format;
import static org.evomaster.client.java.sql.advanced.helpers.dump.ExceptionHelper.exceptionToString;
import static org.evomaster.client.java.sql.advanced.helpers.dump.JsonFileHelper.saveObject;
import static org.evomaster.client.java.sql.advanced.helpers.dump.PlainTextFileHelper.append;
import static org.evomaster.client.java.sql.advanced.query_calculator.QueryCalculator.createQueryCalculator;

public class AdvancedHeuristic {

    private SqlDriver sqlDriver;
    private Boolean dumpExceptions;
    private TaintHandler taintHandler;

    private AdvancedHeuristic(SqlDriver sqlDriver, Boolean dumpExceptions, TaintHandler taintHandler) {
        this.sqlDriver = sqlDriver;
        this.dumpExceptions = dumpExceptions;
        this.taintHandler = taintHandler;
    }

    public static AdvancedHeuristic createAdvancedHeuristic(SqlDriver sqlDriver, Boolean dumpExceptions, TaintHandler taintHandler) {
        return new AdvancedHeuristic(sqlDriver, dumpExceptions, taintHandler);
    }

    public static AdvancedHeuristic createAdvancedHeuristic(SqlDriver sqlDriver, TaintHandler taintHandler) {
        return createAdvancedHeuristic(sqlDriver, false, taintHandler);
    }

    public static AdvancedHeuristic createAdvancedHeuristic(SqlDriver sqlDriver) {
        return createAdvancedHeuristic(sqlDriver, null);
    }

    public Truthness calculate(String query) {
        try {
            QueryCalculator queryCalculator = createQueryCalculator(query, sqlDriver, taintHandler);
            CalculationResult calculationResult = queryCalculator.calculate();
            return calculationResult.getTruthness();
        } catch (Exception e) {
            SimpleLogger.error(format("Error occurred while calculating advanced distance " +
                "for query: %s\n%s", query, sqlDriver), e);
            if(dumpExceptions) dumpException(query, sqlDriver, e);
            throw new RuntimeException(e);
        }
    }

    private void dumpException(String query, SqlDriver sqlDriver, Exception exception) {
        if(!(exception instanceof UnsupportedOperationException)) {
            String tmpFile = UUID.randomUUID().toString();
            String cacheFile = tmpFile + "_cache.txt";
            String schemaFile = tmpFile + "_schema.txt";
            saveObject(cacheFile, sqlDriver.getCache());
            saveObject(schemaFile, sqlDriver.getSchema());
            append("errors.txt", format("Exception:\n\n%s\n\nQuery:\n\n%s\n\nCache (saved in %s):\n\n%s\n\nSchema (saved in %s):\n\n%s\n\n\n\n",
                exceptionToString(exception), query, cacheFile, sqlDriver.getCache(), schemaFile, sqlDriver.getSchema()));
        } else {
            StackTraceElement stackTraceElement = exception.getStackTrace()[0];
            if(stackTraceElement.getClassName().equals(WhereCalculator.class.getCanonicalName())){
                stackTraceElement = exception.getStackTrace()[1];
            }
            append("unsupported.txt", format("%s\n", stackTraceElement));
        }
    }

    public void setDumpExceptions(Boolean dumpExceptions) {
        this.dumpExceptions = dumpExceptions;
    }
}
