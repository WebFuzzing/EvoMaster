package org.evomaster.clientJava.instrumentation.db;

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

        if(prepared == null || prepared.trim().isEmpty()){
            return "";
        }

        return PREFIX + prepared;
    }
}
