package org.evomaster.client.java.controller.internal.db.constraint;

import org.evomaster.client.java.controller.internal.db.constraint.jsql.JSqlConditionParser;

public class SqlConditionParserFactory {

    public static SqlConditionParser buildParser() {
        return new JSqlConditionParser();
    }

}
