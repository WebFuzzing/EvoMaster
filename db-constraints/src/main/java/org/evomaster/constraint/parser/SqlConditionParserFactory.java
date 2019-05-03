package org.evomaster.constraint.parser;

import org.evomaster.constraint.parser.jsql.JSqlConditionParser;

public class SqlConditionParserFactory {

    public static SqlConditionParser buildParser() {
        return new JSqlConditionParser();
    }

}
