package org.evomaster.client.java.controller.internal.db.constraint.parser;

import org.evomaster.client.java.controller.internal.db.constraint.parser.jsql.JSqlConditionParser;

public class SqlConditionParserFactory {

    public static SqlConditionParser buildParser() {
        return new JSqlConditionParser();
    }

}
