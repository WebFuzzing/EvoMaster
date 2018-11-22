package org.evomaster.clientJava.databasespy;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

/**
 * To be able to easily parse the SQL commands intercepted by P6Spy, we
 * need them in a specific format.
 * This is also useful when we need to discern them from other SUT messages.
 */
public class P6SpyFormatter implements MessageFormattingStrategy {

    public static final String PREFIX = "P6SPY_SQL: ";

    @Override
    public String formatMessage(int connectionId,
                                String now,
                                long elapsed,
                                String category,
                                String prepared,
                                String sql) {

        boolean hasPrepared = (prepared != null && !prepared.trim().isEmpty());
        boolean hasSQL = (sql != null && !sql.trim().isEmpty());

        if (!hasPrepared && !hasSQL) {
            return "";
        }

        /*
            Note: weird behavior of P6Spy library, where definition
            of "prepared" and "sql" inputs is rather confusing,
            and we get different behavior based on whether parameters "?"
            are present or not in the query.
         */
        if (!hasSQL) {
            return PREFIX + prepared;
        } else {
            return PREFIX + sql;
        }
    }
}
