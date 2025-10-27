package org.evomaster.dbconstraint.parser;

import org.evomaster.dbconstraint.parser.jsql.JSqlConditionParser;

public class SqlConditionParserFactory {

    private SqlConditionParserFactory() {
    }

    public static SqlConditionParser buildParser() {
        return new JSqlConditionParser();
    }

}
